package com.bitgrind.android.adb;

/**
 * Created by mrenouf on 3/13/17.
 */
public interface RetryPolicy {

    boolean shouldRetry(int failureCount);

    long retryDelay(int failureCount);

    static RetryPolicy simple(int maxAttempts, long delayMillis) {
        return new SimpleRetryPolicy(delayMillis,maxAttempts);
    }

    RetryPolicy ALWAYS_IMMEDIATE = new RetryPolicy() {
        @Override
        public boolean shouldRetry(int failureCount) {
            return true;
        }

        @Override
        public long retryDelay(int failureCount) {
            return 0;
        }
    };

}
