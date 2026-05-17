package com.sharenote.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @Size(max = 100, message = "Middle name must be at most 100 characters")
        String middleName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must be at most 320 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                message = "Password must include uppercase, lowercase, and number characters"
        )
        String password,

        @NotBlank(message = "Institution is required")
        @Size(max = 120, message = "Institution must be at most 120 characters")
        String institution,

        @NotBlank(message = "Degree program is required")
        @Size(max = 120, message = "Degree program must be at most 120 characters")
        String degreeProgram,

        @NotBlank(message = "Current semester or year is required")
        @Size(max = 50, message = "Current semester or year must be at most 50 characters")
        String currentSemesterOrYear,

        @NotBlank(message = "Current year is required")
        @Size(max = 20, message = "Current year must be at most 20 characters")
        String currentYear,

        @NotBlank(message = "Current semester is required")
        @Size(max = 50, message = "Current semester must be at most 50 characters")
        String currentSemester,

        @NotBlank(message = "Phone number is required")
        @Size(max = 30, message = "Phone number must be at most 30 characters")
        @Pattern(regexp = "^\\+?[0-9 .()\\-]{7,30}$", message = "Phone number must be valid")
        String phoneNumber,

        @NotBlank(message = "Country is required")
        @Size(max = 100, message = "Country must be at most 100 characters")
        String country
) {
}
