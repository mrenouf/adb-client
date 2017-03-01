package com.bitgrind.android.adb;


import sun.net.ConnectionResetException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

public class NetworkChannelSupplier implements Supplier<Result<ByteChannel>> {
    private final InetSocketAddress addr;


    public NetworkChannelSupplier(InetSocketAddress addr) throws IOException {
        this.addr = addr;
    }

    @Override
    public Result<ByteChannel> get() {
        try {
            SocketChannel channel = SocketChannel.open();
            channel.connect(addr);
            return Result.ofValue(channel);
        } catch (ConnectException | NoRouteToHostException | ConnectionResetException | PortUnreachableException e) {
            return Result.error(ErrorCode.CONNECTION_FAILED, e);
        } catch (IOException e) {
            return Result.error(ErrorCode.IO_EXCEPTION, e);
        }
    }
}
