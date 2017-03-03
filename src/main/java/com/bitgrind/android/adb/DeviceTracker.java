package com.bitgrind.android.adb;

import com.google.common.base.Preconditions;

import java.nio.channels.ByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class DeviceTracker {

    private final Supplier<Result<ByteChannel>> channelSupplier;
    private final Executor executor;

    public DeviceTracker(Supplier<Result<ByteChannel>> channelSupplier) {
        this(channelSupplier, Executors.newSingleThreadExecutor());
    }

    public DeviceTracker(Supplier<Result<ByteChannel>> channelSupplier, Executor executor) {
        this.channelSupplier = channelSupplier;
        this.executor = executor;
    }

    public void start() {
        
    }
    void init() {
        executor.execute(() -> {
        });
    }
}