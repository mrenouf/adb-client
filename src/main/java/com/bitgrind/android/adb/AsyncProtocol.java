package com.bitgrind.android.adb;

import com.google.common.base.Splitter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AsyncProtocol {
    private static final int BUFFER_SIZE = 1024;

    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final Supplier<Result<ByteChannel>> channelSupplier;

    /** Handles output from 'devices-l' command */
    private final Function<String, Device> parseDeviceDetailLine = deviceText -> {
        // Serial is formatted to a field width of 22 characters, left-aligned.
        String serial = deviceText.substring(0, 22).trim();
        // Device state follows after a space.
        int stateEnd = deviceText.indexOf(' ', 23);
        String state = deviceText.substring(23, stateEnd);
        // The next token is usually devpath, if the device is connected via usb. Otherwise it's omitted.
        // The format is always 'usb:#-#' but this should just be treated as an opaque string.
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

        Device d = new Device(serial);
        d.state = Device.State.valueOf(state);
        d.devpath = devPath;
        d.product = values.get("product");
        d.model = values.get("model");
        d.device = values.get("device");
        return d;
    };

    private Function<? super String, ?> parseDeviceLine = line -> {
        String[] split = line.split("\t");
        return "";
    };

    AsyncProtocol(Supplier<Result<ByteChannel>> channelSupplier) {
        this.channelSupplier = Objects.requireNonNull(channelSupplier);
    }

    static String readMessage(ByteChannel channel, ByteBuffer buffer) throws IOException {
        String lengthHex = readString(channel, buffer, 4);
        int length = Integer.parseInt(lengthHex, 16);
        return readString(channel, buffer, length);
    }

    static void writeMessage(ByteChannel channel, ByteBuffer buffer, String msg) throws IOException {
        writeString(channel, buffer, String.format("%04x%s", msg.length(), msg));
    }

    static Status readStatus(ByteChannel channel, ByteBuffer buffer) throws IOException {
        String status = readString(channel, buffer, 4);
        if (status.equals("OKAY")) {
            return Status.okay();
        }
        String error = readMessage(channel, buffer);
        return Status.fail(error);
    }

    private static String toString(ByteBuffer buffer, Charset charset) {
        return new String(buffer.array(), buffer.position(), buffer.limit(), charset);
    }

    private static String readString(ByteChannel channel, ByteBuffer buffer, int length) throws IOException {
        buffer.clear();
        buffer.limit(length);
        while (length > 0) {
            length -= channel.read(buffer);
        }
        buffer.flip();
        return toString(buffer, StandardCharsets.UTF_8);
    }

    private static void writeString(ByteChannel channel, ByteBuffer buffer, String message) throws IOException {
        buffer.clear();
        buffer.put(message.getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        channel.write(buffer);
    }

    static Result<String> command(ByteChannel channel, ByteBuffer buffer, String msg) {
        try {
            writeMessage(channel, buffer, msg);
            Status status = readStatus(channel, buffer);
            if (status.isOk()) {
                return Result.ofValue(readMessage(channel, buffer));
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
            Result<String> result = command(channel.get(), buffer, "host:version");
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
            return command(channelResult.get(), buffer, "host:kill");
        }
    }

    public Result<List<Device>> listDevices() {
        try (Result<ByteChannel> channelResult = channelSupplier.get()) {
            if (!channelResult.ok()) {
                return channelResult.asError();
            }
            Result<String> result = command(channelResult.get(), buffer, "host:devices-l");
            if (!result.ok()) {
                return result.asError();
            }
            List<Device> devices =
                    Arrays.stream(result.get().split("\n"))
                            .map(parseDeviceDetailLine)
                            .collect(Collectors.toList());
            return Result.ofValue(devices);
        }
    }


    public Result<DeviceTracker> trackDevices() {
        try {
            Result<ByteChannel> channelResult = channelSupplier.get();
            if (!channelResult.ok()) {
                channelResult.close();
                return channelResult.asError();
            }

            writeMessage(channelResult.get(), buffer, "host:track-devices");
            Status status = readStatus(channelResult.get(), buffer);

            if (status.isOk()) {
                return null;//Result.ofValue(new DeviceTracker(channelResult.get()));
            } else {
                return Result.error(ErrorCode.COMMAND_FAILED, new CommandException(status.getMessage()));
            }
        } catch (IllegalArgumentException e) {
            return Result.error(ErrorCode.PARSE_ERROR, e);
        } catch (IOException e) {
            return Result.error(ErrorCode.IO_EXCEPTION, e);
        }
    }
}
