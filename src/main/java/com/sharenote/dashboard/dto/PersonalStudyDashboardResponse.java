package com.sharenote.dashboard.dto;

import java.time.Instant;
import java.util.List;

public record PersonalStudyDashboardResponse(
        Long userId,
        String userName,
        String institution,
        String degreeProgram,
        String currentYear,
        String currentSemester,
        Instant generatedAt,
        DashboardSummaryResponse summary,
        List<DashboardClassResponse> registeredClasses,
        List<DashboardExamReminderResponse> upcomingExams,
        List<DashboardRecommendedNoteResponse> recommendedNotes,
        List<DashboardNotificationResponse> unreadSuggestions,
        List<DashboardNotificationResponse> recentNotifications,
        List<DashboardStudyGroupResponse> activeStudyGroups
) {
}
