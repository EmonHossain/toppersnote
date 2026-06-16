package com.sharenote.audit;

import com.sharenote.persistence.CriteriaRepositorySupport;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.time.Instant;

@Repository
public class AuditRepository extends CriteriaRepositorySupport<AuditEvent> {

    private static final int AUDIT_LIMIT = 200;

    public AuditRepository() {
        super(AuditEvent.class);
    }

    // save
    @Transactional
    public AuditEvent save(AuditEvent auditEvent) {
        return saveEntity(auditEvent);
    }

    // findRecent
    @Transactional(readOnly = true)
    public List<AuditEvent> findTop200ByOrderByCreatedAtDesc() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEvent> query = cb.createQuery(AuditEvent.class);
        Root<AuditEvent> auditEvent = query.from(AuditEvent.class);

        query.orderBy(cb.desc(auditEvent.get("createdAt")));

        return limited(query);
    }

    // findByAction
    @Transactional(readOnly = true)
    public List<AuditEvent> findTop200ByActionOrderByCreatedAtDesc(AuditAction action) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEvent> query = cb.createQuery(AuditEvent.class);
        Root<AuditEvent> auditEvent = query.from(AuditEvent.class);

        query.where(cb.equal(auditEvent.get("action"), action));
        query.orderBy(cb.desc(auditEvent.get("createdAt")));

        return limited(query);
    }

    // findByActor
    @Transactional(readOnly = true)
    public List<AuditEvent> findTop200ByActorUserIdOrderByCreatedAtDesc(Long actorUserId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEvent> query = cb.createQuery(AuditEvent.class);
        Root<AuditEvent> auditEvent = query.from(AuditEvent.class);

        query.where(cb.equal(auditEvent.get("actorUserId"), actorUserId));
        query.orderBy(cb.desc(auditEvent.get("createdAt")));

        return limited(query);
    }

    // findByTarget
    @Transactional(readOnly = true)
    public List<AuditEvent> findTop200ByTargetTypeIgnoreCaseAndTargetIdOrderByCreatedAtDesc(
            String targetType,
            Long targetId
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AuditEvent> query = cb.createQuery(AuditEvent.class);
        Root<AuditEvent> auditEvent = query.from(AuditEvent.class);

        query.where(
                cb.equal(cb.lower(auditEvent.get("targetType")), targetType.toLowerCase(Locale.ROOT)),
                cb.equal(auditEvent.get("targetId"), targetId)
        );
        query.orderBy(cb.desc(auditEvent.get("createdAt")));

        return limited(query);
    }

    // countByActionBetween: Counts audit events of one action in a time window.
    @Transactional(readOnly = true)
    public long countByActionBetween(AuditAction action, Instant start, Instant end) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<AuditEvent> auditEvent = query.from(AuditEvent.class);

        query.select(cb.count(auditEvent));
        query.where(
                cb.equal(auditEvent.get("action"), action),
                cb.between(auditEvent.get("createdAt"), start, end)
        );

        return entityManager.createQuery(query).getSingleResult();
    }

    // limited: Applies the standard audit query result limit.
    private List<AuditEvent> limited(CriteriaQuery<AuditEvent> query) {
        TypedQuery<AuditEvent> typedQuery = entityManager.createQuery(query);
        typedQuery.setMaxResults(AUDIT_LIMIT);
        return typedQuery.getResultList();
    }
}
