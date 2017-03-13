package com.bitgrind.android.adb.testing;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mrenouf on 2/26/17.
 */
public class FakeByteChannel implements ByteChannel {
    private final ByteBuffer readBuffer;
    private final ByteBuffer writeBuffer;
    private final ReentrantLock readLock = new ReentrantLock();
    private final Condition readBufferAppended = readLock.newCondition();
    private final Condition readBlocked = readLock.newCondition();
    private final ReentrantLock writeLock = new ReentrantLock();
    private boolean blocked;
    private volatile boolean open;
    private volatile boolean blocking;

    private volatile CountDownLatch readWaitLatch = new CountDownLatch(0);

    public FakeByteChannel() {
        this(1024, 1024);
    }

    public FakeByteChannel(int writeBufferSize) {
        this(1024, writeBufferSize);
    }

    public FakeByteChannel(int readBufferSize, int writeBufferSize) {
        writeBuffer = ByteBuffer.allocate(writeBufferSize);
        readBuffer = ByteBuffer.allocate(readBufferSize);
        open = true;

    }

    public void appendForRead(int value) {
        try {
            readLock.lock();
            readBuffer.put((byte) (value & 0xff));
            readBufferAppended.signalAll();
        } finally {
            readLock.unlock();
        }
    }

    public void appendForRead(byte[] content) {
        try {
            readLock.lock();
            readBuffer.put(content);
            readBufferAppended.signalAll();
        } finally {
            readLock.unlock();
        }
    }

    public void appendForRead(String content) {
        appendForRead(content.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getWriteBuffer() {
        try {
            writeLock.lock();
            writeBuffer.flip();
            byte[] output = new byte[writeBuffer.remaining()];
            System.arraycopy(writeBuffer.array(), writeBuffer.arrayOffset() + writeBuffer.position(), output,
                    0, writeBuffer.remaining());
            writeBuffer.position(writeBuffer.limit());
            writeBuffer.limit(writeBuffer.capacity());
            return output;
        } finally {
            writeLock.unlock();
        }
    }

    public String getWriteBufferAsString() {
        try {
            writeLock.lock();
            writeBuffer.flip();
            String value = new String(writeBuffer.array(), writeBuffer.arrayOffset() + writeBuffer.position(),
                    writeBuffer.remaining(), StandardCharsets.UTF_8);
            writeBuffer.position(writeBuffer.limit());
            writeBuffer.limit(writeBuffer.capacity());
            return value;
        } finally {
            writeLock.unlock();
        }
    }

    public void clearWriteBuffer() {
        try {
            writeLock.lock();
            writeBuffer.clear();
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() throws IOException {
        this.open = false;
    }

    public void setBlocking(boolean blocking) {
        this.blocking = blocking;
    }

    public void expectReads(int count) {
        readWaitLatch = new CountDownLatch(count);
    }

    public boolean awaitCompletedReads(long timeout, TimeUnit unit) throws InterruptedException {
        return readWaitLatch.await(timeout, unit);
    }

    public boolean awaitBlockedRead(long timeout, TimeUnit unit) throws InterruptedException {
        readLock.lock();
        try {
            boolean timedOut = false;
            while (!blocked) {
                timedOut = readBlocked.await(timeout, unit);
            }
            return timedOut;
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        // Block if no data is available
        try {
            readLock.lock();
            if (blocking) {
                while (readBuffer.position() < dst.remaining()) {
                    blocked = true;
                    readBlocked.signalAll();
                    readBufferAppended.await();
                }
                blocked = false;
            }

            if (readBuffer.position() == 0) {
                return 0;
            } else {
                readBuffer.flip();
                int limit = readBuffer.limit();
                readBuffer.limit(Math.min(readBuffer.limit(), dst.remaining()));
                dst.put(readBuffer);
                readBuffer.limit(limit);
                return readBuffer.position();
            }
        } catch (InterruptedException e) {
            return 0;
        } finally {
            readBuffer.compact();
            readWaitLatch.countDown();
            readLock.unlock();
         }
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (!isOpen()) {
            throw new ClosedChannelException();
        }
        int previousPosition = writeBuffer.position();
        writeBuffer.put(src);
        return writeBuffer.position() - previousPosition;
    }

    public void reopen() {
        open = true;
    }
}
