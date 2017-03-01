package com.bitgrind.android.adb;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mrenouf on 2/25/17.
 */

@RunWith(MockitoJUnitRunner.class)
public class ProtocolTest {

    private FakeByteChannel channel;

    @Before
    public void setUp() {
        channel = new FakeByteChannel(1024);
    }

    @Test
    public void testGetVersionSuccessful() throws IOException, CommandException {
        channel.open();
        channel.setReadBuffer("OKAY00040ace");
        AsyncProtocol proto = new AsyncProtocol(channel);
        int version = proto.getVersion();
        assertFalse(channel.isOpen());
        assertEquals(channel.getWriteBufferAsString(), "000chost:version");
        assertEquals(version, 2766 /* 0xACE */);
        System.out.println(version);
    }

    @Test(expected = CommandException.class)
    public void testGetVersionFailure() throws IOException, CommandException {
        channel.open();
        channel.setReadBuffer("FAIL000eInternal Error");
        AsyncProtocol proto = new AsyncProtocol(channel);
        int version = proto.getVersion();
        assertFalse(channel.isOpen());
        assertEquals(channel.getWriteBufferAsString(), "000chost:version");
    }


    @Test
    public void testKill() throws IOException, CommandException {
        channel.open();
        channel.setReadBuffer("OKAY0000");
        AsyncProtocol proto = new AsyncProtocol(channel);
        String resp = proto.kill();
        assertFalse(channel.isOpen());
        assertEquals(channel.getWriteBufferAsString(), "0009host:kill");
    }

}
