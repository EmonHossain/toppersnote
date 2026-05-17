package com.sharenote.note;

public class InvalidNoteQueryException extends RuntimeException {

    public InvalidNoteQueryException(String message) {
        super(message);
    }
}
