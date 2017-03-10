package com.bitgrind.android.adb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import static com.bitgrind.android.adb.AsyncProtocol.*;

public class DeviceTracker {

    private final Supplier<Result<ByteChannel>> channelSupplier;
    private final Executor executor;
    boolean shouldExit = false;
    private boolean running;

    public DeviceTracker(Supplier<Result<ByteChannel>> channelSupplier) {
        this(channelSupplier, Executors.newSingleThreadExecutor());
    }

    public DeviceTracker(Supplier<Result<ByteChannel>> channelSupplier, Executor executor) {
        this.channelSupplier = channelSupplier;
        this.executor = executor;
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        DeviceTracker tracker = new DeviceTracker(
                new NetworkChannelSupplier(new InetSocketAddress("127.0.0.1", 5037)));
        tracker.start();
        Thread.sleep(10000);

    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        executor.execute(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            for (; ; ) {
                try (Result<ByteChannel> channelResult = channelSupplier.get()) {
                    if (channelResult.ok()) {
                        ByteChannel channel = channelResult.get();
                        writeMessage(channel, buffer, "host:track-devices");
                        Status status = readStatus(channel, buffer);
                        if (status.isOk()) {
                            monitor(channel, buffer);
                        }
                    }
                } catch (IOException e) {
                }
                // Delay, try reconnect, try to start ADB, etc
            }
        });
        shouldExit = true;
    }

    private void monitor(ByteChannel channel, ByteBuffer buffer) throws IOException {
        String initialState = readMessage(channel, buffer);
        System.err.println("Initial state: " + initialState);
        for (; ; ) {
            String update = readMessage(channel, buffer);
            System.err.println("Update: " + update);
        }
    }
}