package com.sharenote.analytics;

public record SearchTrendRow(
        String queryText,
        long searchCount,
        long totalResultCount
) {
}
