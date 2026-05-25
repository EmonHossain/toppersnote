package com.sharenote.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logging")
public record LoggingProperties(
    String traceIdKey,
    String tenantIdKey,
    String userIdKey,
    String clientIpKey
) {
}
