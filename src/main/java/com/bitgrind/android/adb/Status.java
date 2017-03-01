package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 2/25/17.
 */
public class Status {
    public enum Code {
        OKAY,
        FAIL;
    }

    private final String message;
    private final Code code;

    public static final Status OKAY = new Status(Code.OKAY, null);

    public static Status fail(String message) {
        return new Status(Code.FAIL, message);
    }

    public static Status okay() {
        return OKAY;
    }

    protected Status(Code code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOk() {
        return code == Code.OKAY;
    }

}
