package com.bitgrind.android.adb;

import com.bitgrind.android.adb.Device.State;
import com.bitgrind.android.adb.DeviceTracker.Listener.Action;
import com.bitgrind.android.adb.testing.FakeByteChannel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.bitgrind.android.adb.AsyncProtocol.formatMessage;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class DeviceTrackerTest {

    private FakeByteChannel channel;

    @Mock
    private DeviceTracker.Listener listener;

    @Mock
    private AdbStarter starter;

    @Mock
    private Supplier<Result<ByteChannel>> supplier;

    @Before
    public void setUp() {
        channel = new FakeByteChannel(1024);
        channel.setBlocking(true);
        when(supplier.get()).thenReturn(Result.ofValue(channel));
   }

    @Test
    public void testDeviceInteractions() throws InterruptedException {
        channel.setBlocking(true);

        DeviceTracker deviceTracker = new DeviceTracker(supplier, starter);
        deviceTracker.addListener(listener);
        deviceTracker.start();

        channel.expectReads(1);
        channel.appendForRead("OKAY");
        channel.appendForRead(formatMessage(""));
        channel.awaitCompletedReads(25, TimeUnit.MILLISECONDS);
        verifyZeroInteractions(listener);

        reset(listener);
        channel.expectReads(1);
        channel.appendForRead(formatMessage("emulator-5554\tdevice"));
        channel.awaitCompletedReads(25, TimeUnit.MILLISECONDS);
        channel.awaitBlockedRead(25, TimeUnit.MILLISECONDS);
        verify(listener).update(eq(Action.ADDED), eq("emulator-5554"), eq(State.device));

        reset(listener);
        channel.expectReads(1);
        channel.appendForRead(formatMessage("emulator-5554\toffline\nemulator-5556\tdevice\nemulator-5558\tdevice"));
        channel.awaitCompletedReads(25, TimeUnit.MILLISECONDS);
        channel.awaitBlockedRead(25, TimeUnit.MILLISECONDS);
        verify(listener).update(eq(Action.CHANGED), eq("emulator-5554"), eq(State.offline));
        verify(listener).update(eq(Action.ADDED), eq("emulator-5556"), eq(State.device));
        verify(listener).update(eq(Action.ADDED), eq("emulator-5558"), eq(State.device));

        reset(listener);
        channel.expectReads(1);
        channel.appendForRead(formatMessage("emulator-5556\tunauthorized"));
        channel.awaitCompletedReads(25, TimeUnit.MILLISECONDS);
        channel.awaitBlockedRead(25, TimeUnit.MILLISECONDS);
        verify(listener).update(eq(Action.REMOVED), eq("emulator-5554"), isNull());
        verify(listener).update(eq(Action.CHANGED), eq("emulator-5556"), eq(State.unauthorized));
        verify(listener).update(eq(Action.REMOVED), eq("emulator-5558"), isNull());

        reset(listener);
        channel.expectReads(1);
        channel.appendForRead(formatMessage(""));
        channel.awaitCompletedReads(25, TimeUnit.MILLISECONDS);
        channel.awaitBlockedRead(25, TimeUnit.MILLISECONDS);
        verify(listener).update(eq(Action.REMOVED), eq("emulator-5556"), isNull());
    }

    @Test
    public void testConnectionFailures_callsAdbStarter() throws InterruptedException {
        when(supplier.get()).thenReturn(Result.error(ErrorCode.CONNECTION_FAILED));

        DeviceTracker deviceTracker = new DeviceTracker(supplier, starter, RetryPolicy.ALWAYS_IMMEDIATE);
        deviceTracker.addListener(listener);

        AtomicInteger count = new AtomicInteger(0);
        when(starter.startAdb(any(AdbVersion.class))).then(invocation -> {
            if (count.incrementAndGet() == 3) {
                when(supplier.get()).thenReturn(Result.ofValue(channel));
            }
            return Result.ofValue(AdbVersion.ANY);
        });

        deviceTracker.start();
        channel.awaitBlockedRead(25, TimeUnit.MILLISECONDS);
        verify(starter, times(3)).startAdb(any(AdbVersion.class));
    }

    @Test
    public void testIOException_callsAdbStarter() throws InterruptedException, IOException {
        DeviceTracker deviceTracker = new DeviceTracker(supplier, starter, RetryPolicy.ALWAYS_IMMEDIATE);
        deviceTracker.addListener(listener);

        AtomicInteger count = new AtomicInteger(0);
        channel.close();

        when(starter.startAdb(any(AdbVersion.class)))
                .thenReturn(Result.ofValue(AdbVersion.ANY))
                .thenReturn(Result.ofValue(AdbVersion.ANY))
                .thenAnswer(invocation -> {
                    channel.reopen();
                    return Result.ofValue(AdbVersion.ANY);
                });
        deviceTracker.start();

        channel.awaitBlockedRead(25, TimeUnit.MILLISECONDS);
        verify(starter, times(3)).startAdb(any(AdbVersion.class));
    }
}
