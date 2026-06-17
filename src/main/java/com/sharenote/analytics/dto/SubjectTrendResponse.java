package com.sharenote.analytics.dto;

public record SubjectTrendResponse(
        String subjectClass,
        String degreeProgram,
        long uploadCount,
        long viewCount,
        long downloadCount
) {
}
