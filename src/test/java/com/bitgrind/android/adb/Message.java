package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 3/13/17.
 */
public class Message {
    static final String EMPTY = "0000";

    static String create(String message) {
        return String.format("%04X%s", message.length(), message);
    }

}
