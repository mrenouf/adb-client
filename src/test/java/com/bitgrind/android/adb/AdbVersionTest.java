package com.bitgrind.android.adb;

import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by mrenouf on 3/10/17.
 */
public class AdbVersionTest {

    @Test
    public void testEqualsLessThanAndHashCode() {
        AdbVersion older = new AdbVersion(1, 0, 12);
        AdbVersion newer = new AdbVersion(1, 0, 35);

        assertTrue(older.lessThan(newer));
        assertFalse(newer.lessThan(older));

        assertTrue(older.equals(older));
        assertFalse(older.equals(newer));

        assertTrue(newer.equals(newer));
        assertFalse(newer.equals(older));

        assertFalse(newer.hashCode() == older.hashCode());
    }
}
