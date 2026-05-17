package com.sharenote.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PermanentBanRequest(
        @NotBlank(message = "Reason is required")
        @Size(max = 1000, message = "Reason must be at most 1000 characters")
        String reason,

        @NotBlank(message = "Notice is required")
        @Size(max = 1000, message = "Notice must be at most 1000 characters")
        String notice
) {
}
