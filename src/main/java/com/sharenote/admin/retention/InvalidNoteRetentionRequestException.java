package com.sharenote.admin.retention;

public class InvalidNoteRetentionRequestException extends RuntimeException {

    // InvalidNoteRetentionRequestException: Builds an invalid retention request error.
    public InvalidNoteRetentionRequestException(String message) {
        super(message);
    }
}
