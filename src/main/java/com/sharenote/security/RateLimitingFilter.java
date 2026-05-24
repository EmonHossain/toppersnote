package com.sharenote.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> aiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain chain)
            throws IOException, ServletException {

        String path = httpRequest.getRequestURI();
        String ip = getClientIp(httpRequest);

        if (path.startsWith("/auth/")) {
            Bucket bucket = authBuckets.computeIfAbsent(ip, this::createAuthBucket);
            if (!bucket.tryConsume(1)) {
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.setContentType("text/plain");
                httpResponse.getWriter().write("Too many authentication requests. Please try again later.");
                return;
            }
        } else if (path.contains("/ai") || path.startsWith("/ai/")) {
            Bucket bucket = aiBuckets.computeIfAbsent(ip, this::createAiBucket);
            if (!bucket.tryConsume(1)) {
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.setContentType("text/plain");
                httpResponse.getWriter().write("Too many AI requests. Please try again later.");
                return;
            }
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    private Bucket createAuthBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createAiBucket(String ip) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
