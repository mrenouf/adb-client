package com.bitgrind.android.adb;

import java.io.IOException;
import java.nio.channels.ByteChannel;

public interface ConnectionProvider {
    public ByteChannel connect() throws IOException;
}
