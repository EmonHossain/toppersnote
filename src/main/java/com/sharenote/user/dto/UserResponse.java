package com.sharenote.user.dto;

import java.util.Set;

public record UserResponse(
        Long id,
        String firstName,
        String middleName,
        String lastName,
        String email,
        String institution,
        String currentSemesterOrYear,
        String phoneNumber,
        String country,
        Set<String> roles
) {
}
