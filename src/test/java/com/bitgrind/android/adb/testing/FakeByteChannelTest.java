package com.bitgrind.android.adb.testing;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Created by mrenouf on 3/10/17.
 */
public class FakeByteChannelTest {
    private FakeByteChannel channel;

    @Before
    public void setUp() {
        channel = new FakeByteChannel();
        channel.setBlocking(false);
    }

    @Test
    public void testAppendForRead_byte() throws IOException {
        ByteBuffer out = ByteBuffer.allocate(10);
        channel.appendForRead(1);
        channel.appendForRead(2);
        channel.appendForRead(3);
        channel.read(out);

        out.flip();
        assertEquals(1, out.get());
        assertEquals(2, out.get());
        assertEquals(3, out.get());
    }

    @Test
    public void testAppendForRead_byteArray() throws IOException {
        ByteBuffer out = ByteBuffer.allocate(10);
        channel.appendForRead(new byte[] { 1, 2, 3});
        channel.read(out);

        out.flip();
        assertEquals(1, out.get());
        assertEquals(2, out.get());
        assertEquals(3, out.get());
    }

    @Test
    public void testAppendForRead_string() throws IOException {
        ByteBuffer out = ByteBuffer.allocate(10);
        channel.appendForRead("0123456789ABCDEFGHIJKLMNOP");

        out.clear();
        channel.read(out);
        out.flip();

        assertEquals("0123456789",
                new String(out.array(), out.position(), out.limit(), StandardCharsets.UTF_8));

        out.clear();
        channel.read(out);
        out.flip();
        assertEquals("ABCDEFGHIJ",
                new String(out.array(), out.position(), out.limit(), StandardCharsets.UTF_8));

        channel.appendForRead("xyz123");

        out.clear();
        channel.read(out);
        out.flip();
        assertEquals("KLMNOPxyz1",
                new String(out.array(), out.position(), out.limit(), StandardCharsets.UTF_8));

    }

    @Test
    public void testWrite_getWriteBuffer() throws IOException {

        channel.write(ByteBuffer.wrap(new byte[] {1, 2, 3, 4}));
        assertArrayEquals(channel.getWriteBuffer(), new byte[] {1, 2, 3, 4});

        channel.write(ByteBuffer.wrap(new byte[] {5, 6, 7, 8}));
        assertArrayEquals(channel.getWriteBuffer(), new byte[] {1, 2, 3, 4, 5, 6, 7 ,8});

        channel.clearWriteBuffer();
        assertArrayEquals(new byte[] {}, channel.getWriteBuffer());
    }

    @Test
    public void testWrite_getWriteBufferAsString() throws IOException {

        channel.write(ByteBuffer.wrap("This is a test".getBytes(StandardCharsets.UTF_8)));
        assertEquals(channel.getWriteBufferAsString(), "This is a test");

        channel.write(ByteBuffer.wrap(", this is only a test".getBytes(StandardCharsets.UTF_8)));
        assertEquals(channel.getWriteBufferAsString(), "This is a test, this is only a test");

        channel.clearWriteBuffer();
        assertEquals(channel.getWriteBufferAsString(), "");
    }

    private static byte[] toArray(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.mark();
        buffer.get(out);
        buffer.reset();
        return out;
    }

    @Test
    public void testRead_blocking() throws IOException, InterruptedException {
        channel.setBlocking(true);
        channel.expectReads(2);
        ByteBuffer out = ByteBuffer.allocate(10);
        AtomicReference<Exception> readThreadFailure = new AtomicReference<>();
        AtomicReference<Exception> writeThreadFailure = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try {
                out.limit(2);
                channel.read(out);
                out.limit(4);
                channel.read(out);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                readThreadFailure.set(e);
            }
        });
        reader.setName("reader");
        reader.setDaemon(true);
        Thread writer = new Thread(() -> {
            channel.appendForRead(1);
            channel.appendForRead(2);
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                writeThreadFailure.set(e);
            }
            channel.appendForRead(3);
            channel.appendForRead(4);
        });
        writer.setName("writer");
        writer.setDaemon(true);
        reader.start();
        writer.start();
        assertTrue("Timed out waiting for blocking reads",
                channel.awaitCompletedReads(100, TimeUnit.DAYS.MILLISECONDS));

        out.flip();
        assertArrayEquals(new byte[] {1, 2, 3, 4}, toArray(out));

        // Check for reader thread readThreadFailure
        Exception readFail = readThreadFailure.get();
        if (readFail != null) {
            throw new IOException("From reader thread", readFail);
        }
        Exception writeFail = readThreadFailure.get();
        if (writeFail != null) {
            throw new IOException("From writer thread", writeFail);
        }
    }
}
