package com.sharenote.academic.dto;

public record AcademicClassResponse(
        Long id,
        String institution,
        String degreeProgram,
        String year,
        String semester,
        String subjectClass
) {
}
