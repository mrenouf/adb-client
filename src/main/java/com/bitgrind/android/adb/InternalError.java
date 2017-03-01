package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 2/25/17.
 */
public class InternalError extends Exception {
    public InternalError() {
        super();
    }

    public InternalError(String message) {
        super(message);
    }

    public InternalError(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalError(Throwable cause) {
        super(cause);
    }

    protected InternalError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
