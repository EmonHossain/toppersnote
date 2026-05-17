package com.sharenote.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemporaryBanRequest(
        @Min(value = 1, message = "Temporary ban must last at least 1 day")
        @Max(value = 365, message = "Temporary ban must be at most 365 days")
        int durationDays,

        @NotBlank(message = "Reason is required")
        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String reason,

        @NotBlank(message = "Notice is required")
        @Size(max = 1000, message = "Notice must be at most 1000 characters")
        String notice
) {
}
