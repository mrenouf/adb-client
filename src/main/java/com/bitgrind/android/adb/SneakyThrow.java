package com.bitgrind.android.adb;

/**
 * Provides a hacky method that always throws {@code t} even if {@code t} is a checked exception.
 * and is not declared to be thrown.
 * <p>
 * See
 * http://www.mail-archive.com/javaposse@googlegroups.com/msg05984.html
 */
public class SneakyThrow {
    /**
     * Throw even checked exceptions without being required
     * to declare them or catch them. Suggested idiom:
     * throw sneakyThrow( some exception );
     */
    public static RuntimeException sneakyThrow(Throwable t) {
        // http://www.mail-archive.com/javaposse@googlegroups.com/msg05984.html
        if (t == null)
            throw new NullPointerException();
        SneakyThrow.<RuntimeException>sneakyThrow0(t);
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow0(Throwable t) throws T {
        throw (T) t;
    }
}