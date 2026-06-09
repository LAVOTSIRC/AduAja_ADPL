package com.plr.aduaja.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitingFilter extends HttpFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final Map<String, SlidingWindowCounter> counters = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_WINDOW = 60;
    private static final long WINDOW_DURATION_MS = 60_000L;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String path = request.getRequestURI();

        boolean shouldLimit = path.startsWith("/api/upload")
                || path.equals("/warga/create-report")
                || path.equals("/admin/login")
                || path.equals("/petugas/login")
                || path.equals("/warga/login");

        if (!shouldLimit) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + path;

        SlidingWindowCounter counter = counters.computeIfAbsent(key, k -> new SlidingWindowCounter());

        if (counter.isAllowed()) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP {} on path {}", clientIp, path);
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Terlalu banyak permintaan. Silakan coba lagi dalam 60 detik.\"}");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return ip != null ? ip : "unknown";
    }

    private static class SlidingWindowCounter {
        private final long windowSizeMs;
        private final int maxRequests;
        private final AtomicInteger counter;
        private volatile long windowStart;

        SlidingWindowCounter() {
            this.windowSizeMs = WINDOW_DURATION_MS;
            this.maxRequests = MAX_REQUESTS_PER_WINDOW;
            this.counter = new AtomicInteger(0);
            this.windowStart = System.currentTimeMillis();
        }

        boolean isAllowed() {
            long now = System.currentTimeMillis();
            long start = windowStart;
            if (now - start >= windowSizeMs) {
                synchronized (this) {
                    if (now - windowStart >= windowSizeMs) {
                        windowStart = now;
                        counter.set(0);
                    }
                }
            }
            int current = counter.incrementAndGet();
            return current <= maxRequests;
        }
    }
}
