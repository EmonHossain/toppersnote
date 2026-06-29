package com.sharenote.audit;

import com.sharenote.user.entities.User;

public interface AuditRecorder {

    void record(AuditAction action, User actor, String targetType, Long targetId, String message);

    void record(AuditAction action, User actor, String targetType, Long targetId, String message, String metadata);

    void recordAnonymous(AuditAction action, String actorEmail, String targetType, Long targetId, String message);
}