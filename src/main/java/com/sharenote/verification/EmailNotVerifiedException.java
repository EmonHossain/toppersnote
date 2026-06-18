package com.sharenote.verification;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("Please verify your university email to upload or download notes");
    }
}
