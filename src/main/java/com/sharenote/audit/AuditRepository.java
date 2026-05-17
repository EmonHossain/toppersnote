package com.sharenote.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findTop200ByOrderByCreatedAtDesc();

    List<AuditEvent> findTop200ByActionOrderByCreatedAtDesc(AuditAction action);

    List<AuditEvent> findTop200ByActorUserIdOrderByCreatedAtDesc(Long actorUserId);

    List<AuditEvent> findTop200ByTargetTypeIgnoreCaseAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
