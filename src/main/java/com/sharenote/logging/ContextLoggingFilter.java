package com.sharenote.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
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

    // ContextLoggingFilter: Creates the request context filter with configurable MDC keys.
    public ContextLoggingFilter(LoggingProperties loggingProperties) {
        this.loggingProperties = loggingProperties;
    }

    // doFilter: Adds request context values to MDC so ECS JSON logs stay correlated.
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Keep request correlation stable across services when callers provide a trace id.
        String traceId = httpRequest.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString();
        }

        String tenantId = httpRequest.getHeader("X-Tenant-Id");
        if (tenantId == null) {
            tenantId = "anonymous_tenant";
        }

        String userId = httpRequest.getHeader("X-User-Id");
        if (userId == null) {
            userId = "anonymous_user";
        }

        String clientIp = httpRequest.getRemoteAddr();
        try {
            // MDC keys are configured so the ECS log pipeline can preserve project-specific field names.
            MDC.put(loggingProperties.traceIdKey(), traceId);
            MDC.put(loggingProperties.tenantIdKey(), tenantId);
            MDC.put(loggingProperties.userIdKey(), userId);
            MDC.put(loggingProperties.clientIpKey(), clientIp);

            chain.doFilter(request, response);
        } finally {
            // Cleanup prevents request context leaking across servlet threads.
            MDC.remove(loggingProperties.traceIdKey());
            MDC.remove(loggingProperties.tenantIdKey());
            MDC.remove(loggingProperties.userIdKey());
            MDC.remove(loggingProperties.clientIpKey());
        }
    }
}
