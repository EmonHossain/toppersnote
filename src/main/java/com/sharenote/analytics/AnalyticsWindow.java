package com.sharenote.analytics;

import java.time.Instant;

public record AnalyticsWindow(
        Instant start,
        Instant end
) {
}
