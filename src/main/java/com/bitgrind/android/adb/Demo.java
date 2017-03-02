package com.bitgrind.android.adb;


import sun.misc.HexDumpEncoder;

import java.io.IOException;
import java.io.StringBufferInputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.util.function.Supplier;

/**
 * Created by mrenouf on 2/27/17.
 */
public class Demo {
    public static void main(String[] args) throws IOException, CommandException {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 5037);
        AsyncProtocol proto = new AsyncProtocol(new NetworkChannelSupplier(addr));
        System.err.println(proto.getVersion());
        System.err.println(proto.getVersion());
        System.err.println(proto.listDevices());
        //new HexDumpEncoder().encode(new StringBufferInputStream(proto.listDevices()), System.out);
    }
}
