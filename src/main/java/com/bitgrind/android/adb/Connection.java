package com.bitgrind.android.adb;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;

public interface Connection extends Closeable {

    public InputStream getInputStream();
    public OutputStream getOutputStream();
    public void close();
}
