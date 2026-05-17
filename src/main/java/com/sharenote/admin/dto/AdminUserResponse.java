package com.sharenote.admin.dto;

import java.time.Instant;
import java.util.Set;

public record AdminUserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String institution,
        String degreeProgram,
        String currentYear,
        String currentSemester,
        boolean permanentlyBanned,
        Instant bannedUntil,
        String banNotice,
        String banReason,
        int policyViolationCount,
        Set<String> roles
) {
}
