package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 3/1/17.
 */
public class Device {
    public Device(String serialNumber) {
        this.serialNumber = serialNumber;
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
        unknown;
    }

    String serialNumber;
    State state;
    String devpath;
    String product;
    String model;
    String device;

}
