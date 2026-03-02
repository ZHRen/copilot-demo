package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Spring MVC interceptor that enforces a sliding-window rate limit per client IP address.
 *
 * <p>How it works:
 * <ul>
 *   <li>Each client IP is tracked in an in-memory map that stores the start of the
 *       current time window and the number of requests made within that window.</li>
 *   <li>When a new request arrives the counter is incremented. If the window has expired
 *       (i.e. {@code now - windowStart >= windowDurationMs}) a fresh window is started
 *       instead.</li>
 *   <li>When the counter exceeds {@code maxRequests} the interceptor writes
 *       HTTP 429 (Too Many Requests) and returns {@code false} to short-circuit further
 *       processing.</li>
 *   <li>A background thread runs every minute to evict entries for IPs whose window has
 *       already expired, preventing unbounded memory growth.</li>
 * </ul>
 *
 * <p>Configuration properties (with defaults):
 * <ul>
 *   <li>{@code app.rate-limit.max-requests} – maximum allowed requests per window (default: 10)</li>
 *   <li>{@code app.rate-limit.window-seconds} – duration of the time window in seconds (default: 60)</li>
 * </ul>
 *
 * <p>The client IP is determined by inspecting the {@code X-Forwarded-For} and
 * {@code X-Real-IP} headers (for reverse-proxy deployments) before falling back to
 * {@link HttpServletRequest#getRemoteAddr()}.
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /** Maximum number of requests allowed within a single time window. */
    private final int maxRequests;

    /** Duration of the sliding time window in milliseconds. */
    private final long windowDurationMs;

    /**
     * Per-IP tracking table. Each entry is a two-element {@code long[]} array where
     * {@code [0]} is the epoch-millisecond timestamp at which the current window started
     * and {@code [1]} is the number of requests counted so far in that window.
     */
    private final ConcurrentHashMap<String, long[]> requestCounts = new ConcurrentHashMap<>();

    /** Background executor used to periodically evict expired entries. */
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * Constructs the interceptor and schedules periodic cleanup of stale entries.
     *
     * @param maxRequests   maximum requests per window (bound to {@code app.rate-limit.max-requests})
     * @param windowSeconds time-window length in seconds (bound to {@code app.rate-limit.window-seconds})
     */
    public RateLimitInterceptor(
            @Value("${app.rate-limit.max-requests:10}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowDurationMs = windowSeconds * 1000L;
        cleanupExecutor.scheduleAtFixedRate(this::removeExpiredEntries, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Intercepts every matched request and applies the rate limit.
     *
     * <p>The counter update is performed atomically via
     * {@link ConcurrentHashMap#compute} to avoid race conditions under concurrent
     * requests from the same IP.
     *
     * @param request  the incoming HTTP request
     * @param response the outgoing HTTP response
     * @param handler  the chosen handler object (not used here)
     * @return {@code true} to continue the handler chain; {@code false} if the rate
     *         limit has been exceeded (HTTP 429 is written to {@code response})
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        // Atomically reset or increment the counter for this IP
        long[] data = requestCounts.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing[0] >= windowDurationMs) {
                // No previous entry or the window has expired – start a fresh window
                return new long[]{now, 1};
            }
            // Still within the same window – increment the request count
            return new long[]{existing[0], existing[1] + 1};
        });

        if (data[1] > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        return true;
    }

    /**
     * Resolves the real client IP address from the request.
     *
     * <p>Checks headers set by common reverse proxies in order of precedence:
     * <ol>
     *   <li>{@code X-Forwarded-For} – may contain a comma-separated list of IPs;
     *       the leftmost (first) value is the original client.</li>
     *   <li>{@code X-Real-IP} – single-value header used by nginx.</li>
     *   <li>{@link HttpServletRequest#getRemoteAddr()} – direct TCP peer address.</li>
     * </ol>
     *
     * @param request the current HTTP request
     * @return the best-effort client IP string
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String firstIp = xForwardedFor.split(",")[0].strip();
            if (!firstIp.isEmpty()) {
                return firstIp;
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.strip();
        }
        return request.getRemoteAddr();
    }

    /**
     * Removes entries from the tracking map whose time window has elapsed.
     *
     * <p>Called by the scheduled executor once per minute to prevent the map from
     * growing indefinitely when many distinct IP addresses make requests.
     */
    private void removeExpiredEntries() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue()[0] >= windowDurationMs);
    }
}
