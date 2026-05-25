package com.sharenote.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.slf4j.MDC;

import java.io.IOException;

import org.springframework.stereotype.Component;

@Component
public class ContextLoggingFilter implements Filter {

    private final LoggingProperties loggingProperties;

    public ContextLoggingFilter(LoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 1. Extract context data from incoming headers/request parameters
        String traceId = httpRequest.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        String tenantId = httpRequest.getHeader("X-Tenant-Id"); // Fallback to "default" if missing
        if (tenantId == null)
            tenantId = "anonymous_tenant";

        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null)
            userId = "anonymous_user";

        String clientIp = httpRequest.getRemoteAddr();
        try {
            // 2. Put context data into MDC for logging
            MDC.put(loggingProperties.traceIdKey(), traceId);
            MDC.put(loggingProperties.tenantIdKey(), tenantId);
            MDC.put(loggingProperties.userIdKey(), userId);
            MDC.put(loggingProperties.clientIpKey(), clientIp);

            chain.doFilter(request, response);
        } finally {
            // Clean up MDC to prevent memory leaks
            MDC.remove(loggingProperties.traceIdKey());
            MDC.remove(loggingProperties.tenantIdKey());
            MDC.remove(loggingProperties.userIdKey());
            MDC.remove(loggingProperties.clientIpKey());
        }
    }
}
