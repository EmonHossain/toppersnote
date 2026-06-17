package com.sharenote.dashboard;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dashboard")
public record DashboardProperties(
        int maxClasses,
        int maxUpcomingExams,
        int maxRecommendedNotes,
        int recommendationCandidateLimit,
        int maxRecentNotifications,
        int maxUnreadSuggestions,
        int maxActiveStudyGroups,
        int analyticsWindowDays
) {

    // normalizedMaxClasses: Provides a safe class cap for the dashboard.
    public int normalizedMaxClasses() {
        return positiveOrDefault(maxClasses, 10);
    }

    // normalizedMaxUpcomingExams: Provides a safe exam reminder cap.
    public int normalizedMaxUpcomingExams() {
        return positiveOrDefault(maxUpcomingExams, 5);
    }

    // normalizedMaxRecommendedNotes: Provides a safe recommended-note cap.
    public int normalizedMaxRecommendedNotes() {
        return positiveOrDefault(maxRecommendedNotes, 8);
    }

    // normalizedRecommendationCandidateLimit: Provides a bounded candidate pool.
    public int normalizedRecommendationCandidateLimit() {
        return Math.max(normalizedMaxRecommendedNotes(), positiveOrDefault(recommendationCandidateLimit, 30));
    }

    // normalizedMaxRecentNotifications: Provides a safe recent-notification cap.
    public int normalizedMaxRecentNotifications() {
        return positiveOrDefault(maxRecentNotifications, 8);
    }

    // normalizedMaxUnreadSuggestions: Provides a safe unread-suggestion cap.
    public int normalizedMaxUnreadSuggestions() {
        return positiveOrDefault(maxUnreadSuggestions, 5);
    }

    // normalizedMaxActiveStudyGroups: Provides a safe active-study-group cap.
    public int normalizedMaxActiveStudyGroups() {
        return positiveOrDefault(maxActiveStudyGroups, 6);
    }

    // normalizedAnalyticsWindowDays: Provides a safe metric window for note ranking.
    public int normalizedAnalyticsWindowDays() {
        return positiveOrDefault(analyticsWindowDays, 7);
    }

    // positiveOrDefault: Keeps environment-provided limits usable.
    private int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }
}
