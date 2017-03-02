package com.bitgrind.android.adb;

import com.google.common.base.Splitter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

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
            if (result.ok()) {
                return Result.ofValue(Integer.parseInt(result.get(), 16));
            } else {
                return result.asError();
            }
        }
    }

    public Result<String> kill() {
        try (Result<ByteChannel> channelResult = channelSupplier.get()) {
            if (!channelResult.ok()) {
                return channelResult.asError();
            }
            return command(channelResult.get(), "host:kill");
        }
    }

    public Result<String> listDevices() {
        try (Result<ByteChannel> channelResult = channelSupplier.get()) {
            if (!channelResult.ok()) {
                return channelResult.asError();
            }
            Result<String> result = command(channelResult.get(), "host:devices-l");
            if (!result.ok()) {
                return result.asError();
            }

            Iterable<String> lines = Splitter.on('\n').omitEmptyStrings().split(result.get());
            for (String deviceText : lines) {
                // Serial is formatted to a field width of 22 characters, left-aligned.
                String serial = deviceText.substring(0, 22).trim();
                // Device state follows after a space.
                int stateEnd = deviceText.indexOf(' ', 23);
                String state = deviceText.substring(23, stateEnd);
                // The next token is usually devpath, if the device is connected via usb. Otherwise it's omitted.
                // The format is always 'usb:#-#' but this is not guaranteed, so be extra careful here.
                String valuesText = deviceText.substring(stateEnd + 1);
                int endNextToken = valuesText.indexOf(' ');
                String nextToken = valuesText.substring(0, endNextToken == -1 ? valuesText.length() : endNextToken);
                String devPath = null;
                // If this isn't the product value, it must be the devpath.
                if (!nextToken.startsWith("product")) {
                    devPath = nextToken;
                    if (endNextToken != -1) {
                        valuesText = valuesText.substring(endNextToken + 1);
                    } else {
                        valuesText = "";
                    }
                }
                Map<String, String> values = Collections.emptyMap();
                if (!valuesText.isEmpty()) {
                    values = Splitter.on(' ').withKeyValueSeparator(':').split(valuesText);
                }

                Device d = new Device(serial); /* serial */
                System.err.println("serial=" + serial);
                System.err.println("devpath=" + devPath);
                System.err.println("values=" + values);
            }
            // TODO: split, create device list
            return result;
        }
    }
}
