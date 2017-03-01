package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 2/25/17.
 */
public class DeviceError extends Exception {
    public DeviceError() {
        super();
    }

    public DeviceError(String message) {
        super(message);
    }

    public DeviceError(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceError(Throwable cause) {
        super(cause);
    }

    protected DeviceError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
