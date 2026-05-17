package com.sharenote.audit;

import com.sharenote.user.User;

public interface AuditPublisher {

    void publish(AuditAction action, User actor, String targetType, Long targetId, String message);

    void publish(AuditAction action, User actor, String targetType, Long targetId, String message, String metadata);

    void publishAnonymous(AuditAction action, String actorEmail, String targetType, Long targetId, String message);
}
