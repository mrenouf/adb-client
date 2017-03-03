package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 3/1/17.
 */
public class Device {
    String serial;
    State state;
    String devpath;
    String product;
    String model;
    String device;

    public Device(String serial) {
        this.serial = serial;
    }
    // platform/system/core/adb/transport.cpp atransport::connection_state_name()
    public enum State {
        offline,
        bootloader,
        device,
        host,
        recovery,
        sideload,
        unauthorized,
        unknown
    }

}
