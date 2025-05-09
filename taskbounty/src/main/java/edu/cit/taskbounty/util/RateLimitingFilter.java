package edu.cit.taskbounty.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final int maxRequests;
    private final Duration timeWindow;
    private final Map<String, RequestTracker> requestCounts = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${rate.limiting.max.requests:5}") int maxRequests,
            @Value("${rate.limiting.time.window.seconds:60}") long timeWindowSeconds) {
        this.maxRequests = maxRequests;
        this.timeWindow = Duration.ofSeconds(timeWindowSeconds);
        logger.info("RateLimitingFilter initialized with maxRequests={} and timeWindow={} seconds", maxRequests, timeWindowSeconds);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();

        if (!requestUri.startsWith("/bounty_post")) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = clientIp + ":" + requestUri;
        RequestTracker tracker = requestCounts.computeIfAbsent(key, k -> new RequestTracker());

        synchronized (tracker) {
            Instant now = Instant.now();
            tracker.removeExpiredTimestamps(now, timeWindow);

            if (tracker.getTimestamps().size() >= maxRequests) {
                logger.warn("Rate limit exceeded for client {} on URI {}", clientIp, requestUri);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Too many requests. Please try again later.");
                return;
            }

            tracker.addTimestamp(now);
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    public int getOrder() {
        return 50; // Run before JwtAuthFilter and UsernamePasswordAuthenticationFilter
    }

    private static class RequestTracker {
        private final List<Instant> timestamps = new ArrayList<>();

        public List<Instant> getTimestamps() {
            return timestamps;
        }

        public void addTimestamp(Instant timestamp) {
            timestamps.add(timestamp);
        }

        public void removeExpiredTimestamps(Instant now, Duration timeWindow) {
            timestamps.removeIf(timestamp -> Duration.between(timestamp, now).compareTo(timeWindow) > 0);
        }
    }
}