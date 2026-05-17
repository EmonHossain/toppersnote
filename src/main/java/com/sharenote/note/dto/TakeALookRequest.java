package com.sharenote.note.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record TakeALookRequest(
        @NotEmpty(message = "At least one user must be selected")
        @Size(max = 20, message = "You can suggest at most 20 users at a time")
        Set<Long> recipientUserIds,

        @Size(max = 500, message = "Message must be at most 500 characters")
        String message
) {
}
