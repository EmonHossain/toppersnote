package com.sharenote.admin.retention;

import com.sharenote.persistence.CriteriaRepositorySupport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class NoteRetentionCandidateRepository extends CriteriaRepositorySupport<NoteRetentionCandidate> {

    // NoteRetentionCandidateRepository: Creates a Criteria-backed retention candidate repository.
    public NoteRetentionCandidateRepository() {
        super(NoteRetentionCandidate.class);
    }

    // save: Persists a note retention candidate.
    @Transactional
    public NoteRetentionCandidate save(NoteRetentionCandidate candidate) {
        return saveEntity(candidate);
    }

    // findById: Finds one retention candidate by id.
    @Transactional(readOnly = true)
    public Optional<NoteRetentionCandidate> findById(Long id) {
        return findEntityById(id);
    }

    // findAllActive: Lists active candidates for admin observation.
    @Transactional(readOnly = true)
    public List<NoteRetentionCandidate> findAllActive(int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<NoteRetentionCandidate> query = cb.createQuery(NoteRetentionCandidate.class);
        Root<NoteRetentionCandidate> candidate = query.from(NoteRetentionCandidate.class);

        query.where(candidate.get("status").in(NoteRetentionStatus.PENDING_NOTICE, NoteRetentionStatus.NOTICE_SENT));
        query.orderBy(cb.asc(candidate.get("removalDueAt")), cb.asc(candidate.get("id")));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }

    // existsActiveByNoteId: Checks whether a note is already scheduled for active retention.
    @Transactional(readOnly = true)
    public boolean existsActiveByNoteId(Long noteId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<NoteRetentionCandidate> candidate = query.from(NoteRetentionCandidate.class);

        query.select(cb.count(candidate));
        query.where(
                cb.equal(candidate.get("noteIdSnapshot"), noteId),
                candidate.get("status").in(NoteRetentionStatus.PENDING_NOTICE, NoteRetentionStatus.NOTICE_SENT)
        );

        return entityManager.createQuery(query).getSingleResult() > 0;
    }

    // findNoticeDue: Finds candidates whose uploader notice is due.
    @Transactional(readOnly = true)
    public List<NoteRetentionCandidate> findNoticeDue(Instant now, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<NoteRetentionCandidate> query = cb.createQuery(NoteRetentionCandidate.class);
        Root<NoteRetentionCandidate> candidate = query.from(NoteRetentionCandidate.class);

        query.where(
                cb.equal(candidate.get("status"), NoteRetentionStatus.PENDING_NOTICE),
                cb.lessThanOrEqualTo(candidate.get("noticeDueAt"), now)
        );
        query.orderBy(cb.asc(candidate.get("noticeDueAt")), cb.asc(candidate.get("id")));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }

    // findRemovalDue: Finds candidates ready for automatic deletion.
    @Transactional(readOnly = true)
    public List<NoteRetentionCandidate> findRemovalDue(Instant now, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<NoteRetentionCandidate> query = cb.createQuery(NoteRetentionCandidate.class);
        Root<NoteRetentionCandidate> candidate = query.from(NoteRetentionCandidate.class);

        query.where(
                candidate.get("status").in(NoteRetentionStatus.PENDING_NOTICE, NoteRetentionStatus.NOTICE_SENT),
                cb.lessThanOrEqualTo(candidate.get("removalDueAt"), now)
        );
        query.orderBy(cb.asc(candidate.get("removalDueAt")), cb.asc(candidate.get("id")));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList();
    }
}
