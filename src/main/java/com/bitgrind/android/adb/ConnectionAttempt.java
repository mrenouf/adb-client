package com.bitgrind.android.adb;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.util.Optional;

public class ConnectionAttempt implements Closeable {

    private final Optional<ByteChannel> channel;
    private final IOException error;

    private ConnectionAttempt(Optional<ByteChannel> channel, IOException error) {
        this.channel = channel;
        this.error = error;
    }

    public boolean isConnected() {
        return channel.isPresent() && channel.get().isOpen();
    }

    public ByteChannel checkedGet() throws IOException {
        return channel.orElseThrow(() -> error);
    }

    @Override
    public void close() {
        channel.ifPresent((channel) -> {
            try {
                channel.close();
            } catch (IOException e) {
            }
        });
    }
}
