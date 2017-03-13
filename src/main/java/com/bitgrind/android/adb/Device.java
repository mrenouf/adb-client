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
        this(serial, null);
    }

    public Device(String serial, State state) {
        this.serial = serial;
        this.state = state;
    }

    @Override
    public String toString() {
        return "Device{" +
                "serial='" + serial + '\'' +
                ", state=" + state +
                ", devpath='" + devpath + '\'' +
                ", product='" + product + '\'' +
                ", model='" + model + '\'' +
                ", device='" + device + '\'' +
                '}';
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

        public static State tryParse(String value, State defaultValue) {
            try {
                return State.valueOf(value);
            } catch (IllegalArgumentException ex) {
                return defaultValue;
            }
        }
    }
}
