package com.bitgrind.android.adb;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Created by mrenouf on 2/27/17.
 */
public class Demo {
    public static void main(String[] args) throws IOException, CommandException, InterruptedException {
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", 5037);
        NetworkChannelSupplier local = new NetworkChannelSupplier(addr);
        AsyncProtocol proto = new AsyncProtocol(local);
        //System.err.println("getVersion(): " + proto.getVersion());
        //System.err.println("listDevices(): " + proto.listDevices());
        //System.err.println("kill(): " + proto.kill());

        // "/home/mrenouf/Android/Sdk/platform-tools/adb"
        DeviceTracker deviceTracker = new DeviceTracker(local, new AdbStarter());
        DateTimeFormatter fmt = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
        deviceTracker.addListener((action, serial, state) ->
                System.out.format("%s %8s %20s %s\n", fmt.format(LocalDateTime.now()), action, serial, state));
        deviceTracker.start();
        deviceTracker.join();
    }
}
