package com.example.taskflow_backend.security;

import com.example.taskflow_backend.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private final Map<String, Deque<Instant>> requestBuckets = new ConcurrentHashMap<>();

    /**
     * Checks if the given action is allowed under the rate limit.
     * Throws TooManyRequestsException if limit is exceeded.
     */
    public synchronized void checkLimit(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);

        Deque<Instant> timestamps = requestBuckets.computeIfAbsent(key, k -> new ArrayDeque<>());

        // Evict expired entries
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= maxRequests) {
            long retryAfterSeconds = Duration.between(cutoff, timestamps.peekFirst()).toSeconds();
            if (retryAfterSeconds <= 0) {
                retryAfterSeconds = 1;
            }
            throw new TooManyRequestsException("Too many requests. Please try again in " + retryAfterSeconds + " seconds.", retryAfterSeconds);
        }

        timestamps.addLast(now);
    }

    /**
     * Resets rate limit for a given key upon successful completion if needed.
     */
    public synchronized void reset(String key) {
        requestBuckets.remove(key);
    }
}
