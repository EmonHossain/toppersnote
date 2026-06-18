package com.sharenote.verification;

public class EmailVerificationDeliveryException extends RuntimeException {

    // Wraps a recoverable SMTP construction or delivery failure.
    public EmailVerificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
