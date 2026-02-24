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

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final int maxRequests;
    private final long windowDurationMs;

    private final ConcurrentHashMap<String, long[]> requestCounts = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public RateLimitInterceptor(
            @Value("${app.rate-limit.max-requests:10}") int maxRequests,
            @Value("${app.rate-limit.window-seconds:60}") int windowSeconds) {
        this.maxRequests = maxRequests;
        this.windowDurationMs = windowSeconds * 1000L;
        cleanupExecutor.scheduleAtFixedRate(this::removeExpiredEntries, 1, 1, TimeUnit.MINUTES);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        long now = System.currentTimeMillis();

        long[] data = requestCounts.compute(clientIp, (ip, existing) -> {
            if (existing == null || now - existing[0] >= windowDurationMs) {
                return new long[]{now, 1};
            }
            return new long[]{existing[0], existing[1] + 1};
        });

        if (data[1] > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }
        return true;
    }

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

    private void removeExpiredEntries() {
        long now = System.currentTimeMillis();
        requestCounts.entrySet().removeIf(entry -> now - entry.getValue()[0] >= windowDurationMs);
    }
}
