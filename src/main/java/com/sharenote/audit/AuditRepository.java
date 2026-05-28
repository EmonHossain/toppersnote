package com.sharenote.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditRepository extends JpaRepository<AuditEvent, Long> {

    @Query("SELECT a FROM AuditEvent a ORDER BY a.createdAt desc")
    List<AuditEvent> findTop200ByOrderByCreatedAtDesc();

    List<AuditEvent> findTop200ByActionOrderByCreatedAtDesc(AuditAction action);

    List<AuditEvent> findTop200ByActorUserIdOrderByCreatedAtDesc(Long actorUserId);

    List<AuditEvent> findTop200ByTargetTypeIgnoreCaseAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
}
