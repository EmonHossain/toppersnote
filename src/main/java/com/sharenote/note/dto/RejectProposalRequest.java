package com.sharenote.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectProposalRequest(
        @NotBlank(message = "Rejection reason is required")
        @Size(max = 1000, message = "Rejection reason must be at most 1000 characters")
        String rejectionReason
) {}
