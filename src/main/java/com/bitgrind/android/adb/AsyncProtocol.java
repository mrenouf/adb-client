package com.bitgrind.android.adb;

import com.google.common.base.Splitter;
import sun.net.ConnectionResetException;

import java.io.*;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.lang.String.format;

public class AsyncProtocol {
    private static final int BUFFER_SIZE = 1024;
    private static final Splitter NEWLINE = Splitter.on('\n');

    private final byte[] array = new byte[BUFFER_SIZE];
    private final ByteBuffer buffer = ByteBuffer.wrap(array);
    private final Supplier<Result<ByteChannel>> channelSupplier;

    AsyncProtocol(Supplier<Result<ByteChannel>> channelSupplier) {
        this.channelSupplier = Objects.requireNonNull(channelSupplier);
    }

    private String readMessage(ByteChannel channel) throws IOException {
        String lengthHex = readString(channel, 4);
        int length = Integer.parseInt(lengthHex, 16);
        return readString(channel, length);
    }
    private void writeMessage(ByteChannel channel, String msg) throws IOException {
        writeString(channel, String.format("%04x%s", msg.length(), msg));
    }

    private Status readStatus(ByteChannel channel) throws IOException {
        String status = readString(channel, 4);
        if (status.equals("OKAY")) {
            return Status.okay();
        }
        String error = readMessage(channel);
        return Status.fail(error);
    }

    private static String toString(ByteBuffer buffer, Charset charset) {
        return new String(buffer.array(), buffer.position(), buffer.limit(), charset);
    }

    private String readString(ByteChannel channel, int length) throws IOException {
        buffer.clear();
        buffer.limit(length);
        while (length > 0) {
            length -= channel.read(buffer);
        }
        buffer.flip();
        return toString(buffer, StandardCharsets.UTF_8);
    }

    private void writeString(ByteChannel channel, String message) throws IOException {
        buffer.clear(); 
        buffer.put(message.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        channel.write(buffer);
    }

    protected Result<String> command(ByteChannel channel, String msg) {
        try {
            writeMessage(channel, msg);
            Status status = readStatus(channel);
            if (status.isOk()) {
                return Result.ofValue(readMessage(channel));
            } else {
                return Result.error(ErrorCode.COMMAND_FAILED, new CommandException(status.getMessage()));
            }
        } catch (IOException e) {
            return Result.error(ErrorCode.IO_EXCEPTION, e);
        }
    }

    public Result<Integer> getVersion() {
        try (Result<ByteChannel> channel = channelSupplier.get()) {
            if (!channel.ok()) {
                return channel.asError();
            }
            Result<String> result = command(channel.get(), "host:version");
            if (result.hasValue()) {
                return Result.ofValue(Integer.parseInt(result.get(), 16));
            } else {
                return result.asError();
            }
        } catch (IOException e) {
            return Result.error(ErrorCode.IO_EXCEPTION, e);
        }
    }

    public Result<String> kill() {
        try (Result<ByteChannel> channel = channelSupplier.get()) {
            if (!channel.ok()) {
                return channel.asError();
            }
            return command(channel.get(), "host:kill");
        } catch (IOException e) {
            return Result.error(ErrorCode.IO_EXCEPTION, e);
        }
    }

    public Result<String> listDevices() {
        try (Result<ByteChannel> channel = channelSupplier.get()) {
            if (!channel.ok()) {
                return channel.asError();
            }
            Result<String> result = command(channel.get(), "host:devices");
            // TODO: split, create device list
            return result;
        } catch (ConnectException|NoRouteToHostException|ConnectionResetException|PortUnreachableException e) {
            return Result.error(ErrorCode.CONNECTION_FAILED, e);
        } catch (IOException e) {
            return Result.error(ErrorCode.IO_EXCEPTION, e);
        }
    }
}
