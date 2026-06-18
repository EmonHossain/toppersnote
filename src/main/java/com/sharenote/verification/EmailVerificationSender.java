package com.sharenote.verification;

import com.sharenote.verification.messaging.EmailVerificationMessage;

public interface EmailVerificationSender {

    // Sends one fully prepared verification message through a concrete delivery channel.
    void sendVerificationEmail(EmailVerificationMessage message);
}
