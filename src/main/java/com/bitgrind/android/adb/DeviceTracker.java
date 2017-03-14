package com.bitgrind.android.adb;

import com.bitgrind.android.adb.DeviceTracker.Listener.Action;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static com.bitgrind.android.adb.AsyncProtocol.*;

public class DeviceTracker {
    private static final Splitter NEWLINE = Splitter.on("\n").omitEmptyStrings();
    private static final Pattern TAB = Pattern.compile(Pattern.quote("\t"));

    private final Supplier<Result<ByteChannel>> channelSupplier;
    private final List<Listener> listeners;
    private final AdbStarter adbStarter;
    private final RetryPolicy retryPolicy;
    private boolean running;
    boolean shouldExit;

    private Thread thread;

    private Map<String, Device.State> deviceStates;


    public interface Listener {
        enum Action {
            ADDED,
            REMOVED,
            CHANGED
        }

        void update(Action action, String serial, Device.State state);
    }

    public DeviceTracker(Supplier<Result<ByteChannel>> channelSupplier, AdbStarter starter) {
        this(channelSupplier, starter, new SimpleRetryPolicy(3000, 3));
    }

    public DeviceTracker(Supplier<Result<ByteChannel>> channelSupplier, AdbStarter starter, RetryPolicy retryPolicy) {
        this.channelSupplier = channelSupplier;
        this.listeners = new ArrayList<>();
        this.deviceStates = new HashMap<>();
        this.adbStarter = starter;
        this.retryPolicy = retryPolicy;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void join() throws InterruptedException {
        thread.join();
    }

    private void run() {
        int connectFailures = 0;
        while (!shouldExit) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            try (Result<ByteChannel> channelResult = channelSupplier.get()) {
                if (channelResult.ok()) {
                    ByteChannel channel = channelResult.get();
                    writeMessage(channel, buffer, "host:track-devices");
                    Status status = readStatus(channel, buffer);
                    if (status.isOk()) {
                        connectFailures = 0;
                        initStates(channel, buffer);
                        monitorUpdates(channel, buffer);
                    }
                } else {
                    System.out.println("Connection failed: " + channelResult);
                }
            } catch (IOException e) {
                System.out.println("Connection failed: " + e);
            }
            if (!shouldExit) {
                connectFailures++;
                if (retryPolicy.shouldRetry(connectFailures)) {
                    try {
                        Thread.sleep(retryPolicy.retryDelay(connectFailures));
                        System.out.println("Attempting to start ADB...");
                        if (!adbStarter.startAdb(AdbVersion.ANY).ok()) {
                            System.out.println("Failed to exec adb. Exiting.");
                            shouldExit = true;
                            break;
                        }
                    } catch (InterruptedException e) {
                        shouldExit = true;
                    }
                } else {
                    System.out.println("Giving up.");
                    shouldExit = true;
                }
            }
        }
    }

    public void stop() {
        shouldExit = true;
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        thread = new Thread(this::run, "DeviceTracker");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        thread.start();
        running = true;
    }

    private void parseState(String s, Map<String, Device.State> target) {
        String[] parts = TAB.split(s, 2);
        if (parts.length == 2) {
            target.put(parts[0], Device.State.tryParse(parts[1], Device.State.unknown));
        }
    }

    private void initStates(ByteChannel channel, ByteBuffer buffer) throws IOException {
        String initialState = readMessage(channel, buffer);
        System.out.println("initialState: '" + initialState + "'");
        for (String status : NEWLINE.split(initialState)) {
            parseState(status, deviceStates);
        }
    }

    private void monitorUpdates(ByteChannel channel, ByteBuffer buffer) throws IOException {
        Map<String, Device.State> updateStates = new HashMap<>();
        for (;;) {
            String update = readMessage(channel, buffer);
            System.out.println("update: '" + update + "'");
            for (String status : NEWLINE.split(update)) {
                parseState(status, updateStates);
            }
            Map<String, Action> changes = diff(deviceStates, updateStates);
            for (Map.Entry<String, Action> e : changes.entrySet()) {
                switch (e.getValue()) {
                    case ADDED:
                    case CHANGED:
                        deviceStates.put(e.getKey(), updateStates.get(e.getKey()));
                        break;
                    case REMOVED:
                        deviceStates.remove(e.getKey());
                        break;
                }
            }
            dispatchToListeners(changes);
            updateStates.clear();
        }
    }

    private void dispatchToListeners(Map<String, Action> changes) {
        for (Map.Entry<String, Action> e : changes.entrySet()) {
            for (Listener l : listeners) {
                l.update(e.getValue(), e.getKey(), deviceStates.get(e.getKey()));
            }
        }
    }

    private Map<String, Action> diff(Map<String, Device.State> before, Map<String, Device.State> after) {
        Map<String, Action> diffs = new HashMap<>();
        for (String serial : Sets.intersection(before.keySet(), after.keySet())) {
            diffs.put(serial, Action.CHANGED);
        }
        for (String serial : Sets.symmetricDifference(before.keySet(), after.keySet())) {
            diffs.put(serial, before.containsKey(serial) ? Action.REMOVED : Action.ADDED);
        }
        return diffs;
    }
}