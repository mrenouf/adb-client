package com.bitgrind.android.adb;


public class SimpleRetryPolicy implements RetryPolicy {

    private final long delayMillis;
    private final int maxAttempts;

    public SimpleRetryPolicy(long delayMillis, int maxAttempts) {

        this.delayMillis = delayMillis;
        this.maxAttempts = maxAttempts;
    }
    @Override
    public boolean shouldRetry(int failureCount) {
        return failureCount < maxAttempts;
    }

    @Override
    public long retryDelay(int failureCount) {
        return delayMillis;
    }
}
