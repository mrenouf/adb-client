package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 2/28/17.
 */
public enum ErrorCode {
    IO_EXCEPTION,
    INTERRUPTED,
    COMMAND_FAILED,
    CONNECTION_FAILED,
    PARSE_ERROR, ADB_MISSING_FROM_PATH, ADB_FAILED_TO_START, ADB_VERSION_MISMATCH, NOT_FOUND
}
