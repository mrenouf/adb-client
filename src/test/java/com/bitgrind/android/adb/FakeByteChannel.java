package com.bitgrind.android.adb;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;

/**
 * Created by mrenouf on 2/26/17.
 */
class FakeByteChannel implements ByteChannel {
    private boolean open;
    private ByteBuffer readBuffer;

    private final ByteBuffer writeBuffer;

    public FakeByteChannel(int writeBufferSize) {
        byte[] array = new byte[writeBufferSize];
        writeBuffer = ByteBuffer.wrap(array);
    }

    public void open() {
        open = true;
    }

    public void setReadBuffer(byte[] content) {
        readBuffer = ByteBuffer.wrap(content);
    }

    public void setReadBuffer(String content) {
        readBuffer = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getWriteBuffer() {
        writeBuffer.flip();
        byte[] output = new byte[writeBuffer.remaining()];
        System.arraycopy(writeBuffer.array(), writeBuffer.arrayOffset() + writeBuffer.position(), output,
                0, writeBuffer.remaining());
        writeBuffer.clear();
        return output;
    }

    public String getWriteBufferAsString() {
        writeBuffer.flip();
        String value = new String(writeBuffer.array(), writeBuffer.arrayOffset() + writeBuffer.position(),
                writeBuffer.remaining(), StandardCharsets.UTF_8);
        writeBuffer.clear();
        return value;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        this.open = false;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int previousPosition = readBuffer.position();
        int saveLimit = readBuffer.limit();
        readBuffer.limit(readBuffer.position() + dst.remaining());
        dst.put(readBuffer);
        readBuffer.limit(saveLimit);
        return readBuffer.position() - previousPosition;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int previousPosition = writeBuffer.position();
        writeBuffer.put(src);
        return writeBuffer.position() - previousPosition;
    }
}
