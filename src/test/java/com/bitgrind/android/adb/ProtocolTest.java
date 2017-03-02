package com.bitgrind.android.adb;

import com.google.common.base.Suppliers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Created by mrenouf on 2/25/17.
 */

@RunWith(MockitoJUnitRunner.class)
public class ProtocolTest {

    private FakeByteChannel channel;
    private Supplier<Result<ByteChannel>> supplier;

    @Before
    public void setUp() {
        channel = new FakeByteChannel(1024);
        supplier = Suppliers.ofInstance(Result.ofValue(channel));
    }

    @Test
    public void testGetVersionSuccessful() {
        channel.open();
        channel.setReadBuffer("OKAY00040ace");
        AsyncProtocol proto = new AsyncProtocol(supplier);
        Result<Integer> version = proto.getVersion();
        assertEquals("000chost:version", channel.getWriteBufferAsString());
        assertEquals(2766 /* 0xACE */, version.get().intValue());
        assertFalse(channel.isOpen());
    }

    @Test
    public void testGetVersionFailure() {
        channel.open();
        channel.setReadBuffer("FAIL" +
                "000e" +
                "Internal Error");
        AsyncProtocol proto = new AsyncProtocol(supplier);
        Result<Integer> versionResult = proto.getVersion();
        assertEquals("000chost:version", channel.getWriteBufferAsString());
        assertFalse(versionResult.ok());
        assertEquals(ErrorCode.COMMAND_FAILED, versionResult.error());
        assertEquals("Internal Error", versionResult.getException().getMessage());
        assertFalse(channel.isOpen());
    }

    @Test
    public void testKill() {
        channel.open();
        channel.setReadBuffer("OKAY" + "0000");
        AsyncProtocol proto = new AsyncProtocol(supplier);
        Result<String> resp = proto.kill();
        assertEquals(channel.getWriteBufferAsString(), "0009host:kill");
        assertFalse(channel.isOpen());
    }

    @Test
    public void testListDevices() {
        channel.open();
        final String response =
            "HT6CP0204170           unauthorized usb:1-14\n" +
            "HT6CP0204170           device <usbdevpath> product:marlin model:Pixel_XL device:marlin\n" +
            "emulator-5554          device product:sdk_google_phone_x86 model:Android_SDK_built_for_x86 device:generic_x86\n";

        channel.setReadBuffer(String.format("OKAY" + "%04X%s", response.length(), response));
        AsyncProtocol proto = new AsyncProtocol(supplier);
        Result<String> resp = proto.listDevices();
        assertEquals("000ehost:devices-l", channel.getWriteBufferAsString());
        assertFalse(channel.isOpen());
    }
}
