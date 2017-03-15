package com.bitgrind.android.adb;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AdbStarterTest {

    private static final String ADB_PATH = "/path/to/adb";

    @Mock
    private CommandExec commandExec;

    @Test
    public void testAdbStarter() throws IOException, InterruptedException {
        doReturn(Result.ofValue(new String[] {"Android Debug Bridge version 1.0.36"}))
                .when(commandExec)
                .execute(aryEq(new String[] {"/path/to/adb" , "version"}));

        doReturn(Result.ofValue(new String[] {}))
                .when(commandExec)
                .execute(aryEq(new String[] {"/path/to/adb" , "start-server"}));

        AdbStarter starter = new AdbStarter(commandExec, ADB_PATH);
        Result<AdbVersion> result = starter.startAdb(AdbVersion.ANY);
        //InOrder inOrder = inOrder(commandExec);

        verify(commandExec).execute(aryEq(new String[] {"/path/to/adb" , "version"}));
        verify(commandExec).execute(aryEq(new String[] {"/path/to/adb" , "start-server"}));

        assertTrue(result.ok());
        assertEquals(new AdbVersion(1,0,36), result.get());
    }
}
