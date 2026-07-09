package com.sharenote.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 320, message = "Email must be at most 320 characters")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters long")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\",./<>?|\\\\~`]).+$",
                message = "Password must include uppercase, lowercase, and either a number or a special character")
        String password
) {
}
