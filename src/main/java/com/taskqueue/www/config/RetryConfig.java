package com.taskqueue.www.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class RetryConfig {

    public static final int MAX_RETRIES = 3;
    public static final long INITIAL_BACKOFF_MS = 5000; // 5 seconds
    public static final int BACKOFF_MULTIPLIER = 3;

    /**
     * Calculate exponential backoff delay
     * Retry 1: 5 seconds
     * Retry 2: 15 seconds (5 * 3)
     * Retry 3: 45 seconds (15 * 3)
     */
    public static long calculateBackoffDelay(int retryCount) {
        if (retryCount <= 0) {
            return 0;
        }
        return (long) (INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, retryCount - 1));
    }

    /**
     * Check if task should be retried
     */
    public static boolean shouldRetry(int currentRetryCount) {
        return currentRetryCount < MAX_RETRIES;
    }
}