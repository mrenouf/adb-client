package com.bitgrind.android.adb;

public class CommandException extends Exception {
    public CommandException(String message) {
        super(message);
    }

    public CommandException(String message, Throwable cause) {
        super(message, cause);
    }

    public CommandException(String message, Exception e) {
        super(message, e);
    }
}
