package com.sharenote.note;

public class CurrentUserNotFoundException extends RuntimeException {

    public CurrentUserNotFoundException() {
        super("Authenticated user could not be found");
    }
}
