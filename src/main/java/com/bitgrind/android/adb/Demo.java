package com.bitgrind.android.adb;


import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by mrenouf on 2/27/17.
 */
public class Demo {
    public static void main(String[] args) throws IOException, CommandException {
        InetSocketAddress addr = new InetSocketAddress("123.0.0.1", 5037);
        AsyncProtocol proto = new AsyncProtocol(new NetworkChannelSupplier(addr));
        System.err.println("getVersion(): " + proto.getVersion());
        System.err.println("listDevices(): " + proto.listDevices());
        System.err.println("kill(): " + proto.kill());
    }
}
