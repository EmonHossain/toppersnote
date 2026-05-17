package com.sharenote.audit;

import com.sharenote.audit.dto.AuditEventResponse;
import com.sharenote.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class AuditService implements AuditPublisher {

    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final int MAX_METADATA_LENGTH = 2000;

    private final AuditRepository auditRepository;
    private final Clock clock;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
        this.clock = Clock.systemUTC();
    }

    @Override
    @Transactional
    public void publish(AuditAction action, User actor, String targetType, Long targetId, String message) {
        publish(action, actor, targetType, targetId, message, null);
    }

    @Override
    @Transactional
    public void publish(
            AuditAction action,
            User actor,
            String targetType,
            Long targetId,
            String message,
            String metadata
    ) {
        auditRepository.save(new AuditEvent(
                action,
                actor == null ? null : actor.getId(),
                actor == null ? null : actor.getEmail(),
                normalizeTargetType(targetType),
                targetId,
                truncate(requireMessage(message), MAX_MESSAGE_LENGTH),
                truncate(metadata, MAX_METADATA_LENGTH),
                Instant.now(clock)
        ));
    }

    @Override
    @Transactional
    public void publishAnonymous(AuditAction action, String actorEmail, String targetType, Long targetId, String message) {
        auditRepository.save(new AuditEvent(
                action,
                null,
                actorEmail,
                normalizeTargetType(targetType),
                targetId,
                truncate(requireMessage(message), MAX_MESSAGE_LENGTH),
                null,
                Instant.now(clock)
        ));
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> search(AuditAction action, Long actorUserId, String targetType, Long targetId) {
        if (action != null) {
            return auditRepository.findTop200ByActionOrderByCreatedAtDesc(action).stream().map(this::toResponse).toList();
        }
        if (actorUserId != null) {
            return auditRepository.findTop200ByActorUserIdOrderByCreatedAtDesc(actorUserId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        if (StringUtils.hasText(targetType) && targetId != null) {
            return auditRepository.findTop200ByTargetTypeIgnoreCaseAndTargetIdOrderByCreatedAtDesc(
                            targetType.trim(),
                            targetId
                    )
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        return auditRepository.findTop200ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getAction().name(),
                event.getActorUserId(),
                event.getActorEmail(),
                event.getTargetType(),
                event.getTargetId(),
                event.getMessage(),
                event.getMetadata(),
                event.getCreatedAt()
        );
    }

    private String normalizeTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            return "SYSTEM";
        }
        return truncate(targetType.trim(), 80);
    }

    private String requireMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "No message provided";
        }
        return message.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
