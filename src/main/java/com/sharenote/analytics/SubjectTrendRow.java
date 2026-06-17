package com.sharenote.analytics;

public record SubjectTrendRow(
        String subjectClass,
        String degreeProgram,
        long count
) {
}
