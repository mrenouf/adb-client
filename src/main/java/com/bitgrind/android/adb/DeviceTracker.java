package com.bitgrind.android.adb;

import java.io.IOException;
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

    public DeviceTracker(Supplier<Result<ByteChannel>> channelSupplier) {
        this(channelSupplier, Executors.newSingleThreadExecutor());
    }

    public DeviceTracker(Supplier<Result<ByteChannel>> channelSupplier, Executor executor) {
        this.channelSupplier = channelSupplier;
        this.executor = executor;
    }

    public void start() {
        executor.execute(() -> {

        });
        shouldExit = true;
    }

    void foo() {
        for (; ; ) {
        }
    }

    boolean bar() {
        try (Result<ByteChannel> channelResult = channelSupplier.get()) {
            if (channelResult.ok()) {
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                ByteChannel channel = channelResult.get();

                try {
                    writeMessage(channel, buffer, "host:track-devices");
                    Status status = readStatus(channel, buffer);
                    if (!status.isOk()) {
                        return false;
                    }
                    for (; ; ) {
                        String update = readMessage(channel, buffer);
                        System.err.println(update);
                    }


                } catch (IOException e) {
                    return true;
                }
            }
        }
        return false;
    }
}