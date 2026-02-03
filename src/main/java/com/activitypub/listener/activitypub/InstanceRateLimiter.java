package com.activitypub.listener.activitypub;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-instance rate limiting with exponential backoff.
 * Uses a simple token-bucket style limit per instance URL and backs off on 429/errors.
 */
@Component
@Slf4j
public class InstanceRateLimiter {

    @Value("${activitypub.default-rate-limit:300}")
    private int defaultRequestsPerMinute;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> backoffUntil = new ConcurrentHashMap<>();

    /**
     * Extract instance base URL (e.g. https://mastodon.social) from any URL.
     */
    public String instanceFromUrl(String url) {
        if (url == null || url.isEmpty()) return "default";
        try {
            int schemeEnd = url.indexOf("://");
            if (schemeEnd < 0) return "default";
            int pathStart = url.indexOf("/", schemeEnd + 3);
            return pathStart > 0 ? url.substring(0, pathStart) : url;
        } catch (Exception e) {
            return "default";
        }
    }

    /**
     * Wait if necessary to respect rate limit and backoff for the given instance URL.
     */
    public void acquire(String requestUrl) throws InterruptedException {
        String instance = instanceFromUrl(requestUrl);
        long now = System.currentTimeMillis();

        Long backoff = getBackoffUntil(instance);
        if (backoff != null && backoff > now) {
            long waitMs = backoff - now;
            log.debug("Rate limit backoff for {}: waiting {} ms", instance, waitMs);
            Thread.sleep(Math.min(waitMs, 30_000));
        }

        Bucket bucket = buckets.computeIfAbsent(instance, k -> new Bucket(defaultRequestsPerMinute));
        long waitMs = bucket.waitTimeForRequest(now);
        if (waitMs > 0) {
            log.debug("Rate limit wait for {}: {} ms", instance, waitMs);
            Thread.sleep(Math.min(waitMs, 5_000));
        }
    }

    /**
     * Record that the instance returned 429 or an error; apply exponential backoff.
     */
    public void recordBackoff(String requestUrl) {
        String instance = instanceFromUrl(requestUrl);
        long backoffMs = 60_000; // 1 minute base
        Long existing = getBackoffUntil(instance);
        if (existing != null && existing > System.currentTimeMillis()) {
            backoffMs = Math.min(300_000, (existing - System.currentTimeMillis()) * 2);
        }
        backoffUntil.put(instance, new AtomicLong(System.currentTimeMillis() + backoffMs));
        log.warn("Backoff applied for {}: {} ms", instance, backoffMs);
    }

    public void setLimitForInstance(String instanceUrl, int requestsPerMinute) {
        buckets.put(instanceFromUrl(instanceUrl), new Bucket(requestsPerMinute));
    }

    private Long getBackoffUntil(String instance) {
        AtomicLong a = backoffUntil.get(instance);
        if (a == null) return null;
        long v = a.get();
        return v > System.currentTimeMillis() ? v : null;
    }

    private static class Bucket {
        private final int permitsPerMinute;
        private final AtomicLong windowStart = new AtomicLong(0);
        private final AtomicInteger count = new AtomicInteger(0);

        Bucket(int permitsPerMinute) {
            this.permitsPerMinute = Math.max(1, permitsPerMinute);
        }

        long waitTimeForRequest(long nowMs) {
            long windowMs = 60_000;
            long start = windowStart.get();
            if (nowMs - start >= windowMs) {
                if (windowStart.compareAndSet(start, nowMs)) {
                    count.set(0);
                }
            }
            int c = count.incrementAndGet();
            if (c <= permitsPerMinute) {
                return 0;
            }
            long nextWindow = windowStart.get() + windowMs;
            return Math.max(0, nextWindow - nowMs);
        }
    }
}
