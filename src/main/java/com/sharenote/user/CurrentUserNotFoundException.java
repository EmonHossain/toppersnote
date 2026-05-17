package com.sharenote.user;

public class CurrentUserNotFoundException extends RuntimeException {

    public CurrentUserNotFoundException() {
        super("Authenticated user could not be found");
    }
}
