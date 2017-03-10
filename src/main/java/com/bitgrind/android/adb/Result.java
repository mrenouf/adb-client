package com.bitgrind.android.adb;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.Closeable;
import java.io.IOException;

public class Result<T> implements Closeable {
    private final T value;
    private final Exception exception;
    private final ErrorCode code;

    public Result(@Nullable T value, @Nullable ErrorCode code, @Nullable  Exception exception) {
        this.value = value;
        this.exception = exception;
        this.code = code;
    }

    public static <T> Result<T> ofValue(@NonNull T value) {
        return new Result<T>(/*Preconditions.checkNotNull(*/value/*)*/, null, null);
    }

    public static <T> Result<T> exception(@NonNull Exception exception) {
        return new Result<T>(null, null, Preconditions.checkNotNull(exception));
    }

    public static <T> Result<T> error(@NonNull ErrorCode code) {
        return new Result<T>(null, code, null);
    }

    public static <T> Result<T> error(@NonNull ErrorCode code, @NonNull Exception exception) {
        return new Result<T>(null, code, exception);
    }

    public T checkedGet() throws Exception {
        if (exception != null) {
            throw exception;
        }
        return value;
    }

    public Exception getException() {
        if (ok() || exception == null) {
            throw new IllegalStateException("No exception to return!");
        }
        return exception;
    }

    public T get() {
        return value;
    }

    public ErrorCode error() {
        return code;
    }

    boolean ok() {
        return code == null && exception == null;
    }

    public <T> Result<T> asError() {
        return (Result<T>) this;
    }

    public String toString() {
        if (code == null && exception == null) {
            return "Result[value: " + value + "]";
        } else {
            return "Result[errorCode: " + code + ", exception: " + exception + "]";
        }
    }

    @Override
    public void close() {
        if (value instanceof Closeable) {
            try {
                ((Closeable) value).close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }


}
