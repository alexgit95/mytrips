package com.alexgit95.MyTrips.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ApiExportRateLimitFilter extends OncePerRequestFilter {

    private static final String API_EXPORT_PATH = "/api/admin/export";
    private static final int CLEANUP_FREQUENCY = 200;

    private final boolean enabled;
    private final int maxRequests;
    private final Duration window;

    private final Map<String, SlidingWindowBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger cleanupCounter = new AtomicInteger(0);

    public ApiExportRateLimitFilter(
            @Value("${app.security.api-export-rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.api-export-rate-limit.max-requests:30}") int maxRequests,
            @Value("${app.security.api-export-rate-limit.window-seconds:60}") long windowSeconds) {
        this.enabled = enabled;
        this.maxRequests = Math.max(1, maxRequests);
        this.window = Duration.ofSeconds(Math.max(1L, windowSeconds));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || !("GET".equalsIgnoreCase(request.getMethod()) && API_EXPORT_PATH.equals(request.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        maybeCleanup();

        String clientKey = resolveClientKey(request);
        Instant now = Instant.now();
        SlidingWindowBucket bucket = buckets.computeIfAbsent(clientKey, key -> new SlidingWindowBucket());
        Decision decision = bucket.tryConsume(now, window, maxRequests);

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
                response.sendError(429,
                    "Trop de tentatives sur l'endpoint d'export API. Reessayez plus tard.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            int commaIndex = forwardedFor.indexOf(',');
            String ip = (commaIndex >= 0 ? forwardedFor.substring(0, commaIndex) : forwardedFor).trim();
            if (!ip.isBlank()) {
                return ip;
            }
        }
        return request.getRemoteAddr();
    }

    private void maybeCleanup() {
        int current = cleanupCounter.incrementAndGet();
        if (current % CLEANUP_FREQUENCY != 0) {
            return;
        }

        Instant staleBefore = Instant.now().minus(window.multipliedBy(2));
        buckets.entrySet().removeIf(entry -> entry.getValue().isStale(staleBefore));
    }

    private static final class SlidingWindowBucket {
        private final ArrayDeque<Instant> hits = new ArrayDeque<>();
        private Instant lastSeen = Instant.EPOCH;

        synchronized Decision tryConsume(Instant now, Duration window, int maxRequests) {
            Instant threshold = now.minus(window);
            while (!hits.isEmpty() && hits.peekFirst().isBefore(threshold)) {
                hits.removeFirst();
            }

            lastSeen = now;
            if (hits.size() < maxRequests) {
                hits.addLast(now);
                return new Decision(true, 0);
            }

            Instant oldest = hits.peekFirst();
            long retryAfter = oldest == null ? 1 : Duration.between(now, oldest.plus(window)).getSeconds();
            return new Decision(false, Math.max(1, retryAfter));
        }

        synchronized boolean isStale(Instant staleBefore) {
            return lastSeen.isBefore(staleBefore);
        }
    }

    private record Decision(boolean allowed, long retryAfterSeconds) {
    }
}