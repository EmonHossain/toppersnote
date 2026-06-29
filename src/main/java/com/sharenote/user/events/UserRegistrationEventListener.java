package com.sharenote.user.events;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditRecorder;
import com.sharenote.user.entities.User;
import com.sharenote.verification.EmailVerificationService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class UserRegistrationEventListener {

    private final AuditRecorder auditRecorder;
    private final EmailVerificationService emailVerificationService;

    public UserRegistrationEventListener(AuditRecorder auditRecorder, 
                                    EmailVerificationService emailVerificationService) {
        this.auditRecorder = auditRecorder;
        this.emailVerificationService = emailVerificationService;
    }

    @Async(value = "eventTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAuditLogging(UserRegisteredEvent event) {
        User user = event.user();
        try {
            auditRecorder.record(
                AuditAction.USER_REGISTERED, 
                user, 
                "USER", 
                user.getId(), 
                "User registered"
            );
        } catch (Exception e) {
            // Log exception here so it doesn't interrupt other listeners
            
        }
    }

    // Runs in a background thread pool parallel to the audit log
    @Async("backgroundTaskExecutor") 
    @EventListener
    public void handleVerificationEmail(UserRegisteredEvent event) {
        User user = event.user();
        try {
            emailVerificationService.sendVerification(user);
        } catch (Exception e) {
            // Log email failure safely without rolling back the user's account creation
            log.error("Failed to send verification email to " + user.getEmail() + ": " + e.getMessage());
        }
    }

}
