package com.sharenote.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analytics")
public record AnalyticsProperties(
        int defaultWindowDays,
        int maxWindowDays,
        int maxResultLimit,
        boolean trackingEnabled
) {
    // normalizedDefaultWindowDays: Returns a safe default analytics window.
    public int normalizedDefaultWindowDays() {
        return defaultWindowDays <= 0 ? 7 : defaultWindowDays;
    }

    // normalizedMaxWindowDays: Returns a safe maximum analytics window.
    public int normalizedMaxWindowDays() {
        return maxWindowDays <= 0 ? 90 : maxWindowDays;
    }

    // normalizedMaxResultLimit: Returns a safe analytics result limit.
    public int normalizedMaxResultLimit() {
        return maxResultLimit <= 0 ? 50 : maxResultLimit;
    }
}
