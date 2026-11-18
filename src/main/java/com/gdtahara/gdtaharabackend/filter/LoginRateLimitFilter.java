package com.gdtahara.gdtaharabackend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Blocks IPs that exceed MAX_ATTEMPTS failed login attempts within WINDOW_SECONDS.
 * No external dependency — uses a ConcurrentHashMap sliding window.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoginRateLimitFilter.class);

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final int MAX_ATTEMPTS  = 10;
    private static final long WINDOW_MS    = 60_000L; // 1 minute

    private record Bucket(AtomicInteger count, long windowStart) {}

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LOGIN_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = resolveIp(request);
        long now = Instant.now().toEpochMilli();

        Bucket bucket = buckets.compute(ip, (k, b) -> {
            if (b == null || (now - b.windowStart()) >= WINDOW_MS) {
                return new Bucket(new AtomicInteger(0), now);
            }
            return b;
        });

        int attempts = bucket.count().incrementAndGet();
        if (attempts > MAX_ATTEMPTS) {
            logger.warn("Rate limit exceeded for IP {} — {} attempts in window", ip, attempts);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many login attempts. Please wait 1 minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
