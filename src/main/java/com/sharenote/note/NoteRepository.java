package com.sharenote.note;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class NoteRepository {

        @PersistenceContext
        private EntityManager entityManager;

        public List<Note> findVisibleNotes(
                        String institution,
                        String degreeProgram,
                        String subjectClass,
                        String semester,
                        String year) {

                // 1. Initialize the CriteriaBuilder and Query container
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<Note> query = cb.createQuery(Note.class);

                // 2. Define the FROM clause (FROM Note)
                Root<Note> note = query.from(Note.class);

                // 3. Build the case-insensitive and boolean conditions (WHERE clauses)
                // We use cb.lower() to emulate the "IgnoreCase" behavior safely
                List<Predicate> predicates = new ArrayList<>();

                predicates.add(cb.equal(cb.lower(note.get("institution")), institution.toLowerCase()));
                predicates.add(cb.equal(cb.lower(note.get("degreeProgram")), degreeProgram.toLowerCase()));
                predicates.add(cb.equal(cb.lower(note.get("subjectClass")), subjectClass.toLowerCase()));
                predicates.add(cb.equal(cb.lower(note.get("semester")), semester.toLowerCase()));
                predicates.add(cb.equal(cb.lower(note.get("year")), year.toLowerCase()));

                // Handling the "AndLatestTrue" part
                predicates.add(cb.equal(note.get("latest"), true));

                // Combine all predicates with an AND operator
                query.where(cb.and(predicates.toArray(new Predicate[0])));

                // 4. Handle the sorting logic (OrderByCreatedAtDesc)
                query.orderBy(cb.desc(note.get("createdAt")));

                // 5. Execute the query programmatically
                return entityManager.createQuery(query).getResultList();
        }

        public List<Note> findTop20Notes(
                        String institution,
                        String degreeProgram,
                        String subjectClass,
                        String semester,
                        String year) {

                // 1. Initialize CriteriaBuilder and Query container
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<Note> query = cb.createQuery(Note.class);

                // 2. Define the FROM clause
                Root<Note> note = query.from(Note.class);

                // 3. Build the case-insensitive and boolean conditions (WHERE clauses)
                List<Predicate> predicates = new ArrayList<>();

                predicates.add(cb.equal(cb.lower(note.get("institution")), institution.toLowerCase()));
                predicates.add(cb.equal(cb.lower(note.get("degreeProgram")), degreeProgram.toLowerCase()));
                predicates.add(cb.equal(cb.lower(note.get("subjectClass")), subjectClass.toLowerCase()));
                predicates.add(cb.equal(cb.lower(note.get("semester")), semester.toLowerCase()));
                predicates.add(cb.equal(cb.lower(note.get("year")), year.toLowerCase()));

                // Handling the "AndLatestTrue"
                predicates.add(cb.equal(note.get("latest"), true));

                // Combine predicates
                query.where(cb.and(predicates.toArray(new Predicate[0])));

                // 4. Handle sorting: OrderByCreatedAtDesc
                query.orderBy(cb.desc(note.get("createdAt")));

                // 5. Create the TypedQuery and apply the "Top20" limit programmatically
                TypedQuery<Note> typedQuery = entityManager.createQuery(query);
                typedQuery.setMaxResults(20); // <-- This replaces "Top20"

                // 6. Execute and return
                return typedQuery.getResultList();
        }

        public List<Note> findNotesByFilters(
                        String institution,
                        String degreeProgram,
                        String subjectClass,
                        String semester,
                        String year) {

                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<Note> query = cb.createQuery(Note.class);
                Root<Note> note = query.from(Note.class);

                List<Predicate> predicates = new ArrayList<>();

                // Emulating UPPER(n.field) = UPPER(:param) cleanly
                predicates.add(cb.equal(cb.upper(note.get("institution")), institution.toUpperCase()));
                predicates.add(cb.equal(cb.upper(note.get("degreeProgram")), degreeProgram.toUpperCase()));
                predicates.add(cb.equal(cb.upper(note.get("subjectClass")), subjectClass.toUpperCase()));
                predicates.add(cb.equal(cb.upper(note.get("semester")), semester.toUpperCase()));
                predicates.add(cb.equal(cb.upper(note.get("year")), year.toUpperCase()));

                query.where(cb.and(predicates.toArray(new Predicate[0])));
                query.orderBy(cb.desc(note.get("createdAt")));

                return entityManager.createQuery(query).getResultList();
        }

        /**
         * 2. Replaces findByIdIn(List<Long> ids)
         * Handles empty lists gracefully to avoid SQL syntax errors.
         */
        public List<Note> findByIds(List<Long> ids) {
                if (ids == null || ids.isEmpty()) {
                        return new ArrayList<>();
                }

                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<Note> query = cb.createQuery(Note.class);
                Root<Note> note = query.from(Note.class);

                // Use the criteria .in() expression
                query.where(note.get("id").in(ids));

                return entityManager.createQuery(query).getResultList();
        }

        /**
         * 3. Replaces findFirstByFileHash(String fileHash)
         * Programmatically limits the query response to 1 row and wraps it in an
         * Optional.
         */
        public Optional<Note> findFirstByFileHash(String fileHash) {
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<Note> query = cb.createQuery(Note.class);
                Root<Note> note = query.from(Note.class);

                query.where(cb.equal(note.get("fileHash"), fileHash));

                TypedQuery<Note> typedQuery = entityManager.createQuery(query);
                typedQuery.setMaxResults(1); // Ensures we only fetch the first record

                return typedQuery.getResultList().stream().findFirst();
        }

        /**
         * 4. Replaces @Query("SELECT n FROM Note n WHERE n.id = :rootId OR
         * n.parentNote.id = :rootId...")
         * Written in clear HQL for better readability of the OR logic.
         */
        public List<Note> findAllVersions(Long rootId) {
                // 1. Initialize CriteriaBuilder and Query container
                CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                CriteriaQuery<Note> query = cb.createQuery(Note.class);

                // 2. Define the FROM clause (FROM Note n)
                Root<Note> note = query.from(Note.class);

                // 3. Build the individual predicates
                // Condition 1: n.id = :rootId
                Predicate isRootNote = cb.equal(note.get("id"), rootId);

                // Condition 2: n.parentNote.id = :rootId
                // This navigates the relationship path: Note -> parentNote -> id
                Predicate isChildOfRoot = cb.equal(note.get("parentNote").get("id"), rootId);

                // 4. Combine them with an OR operator: WHERE n.id = :rootId OR n.parentNote.id
                // = :rootId
                query.where(cb.or(isRootNote, isChildOfRoot));

                // 5. Handle the sorting logic: ORDER BY n.versionNumber ASC
                query.orderBy(cb.asc(note.get("versionNumber")));

                // 6. Execute the programmatic query
                return entityManager.createQuery(query).getResultList();
        }

        @Transactional
        public void saveNewNote(Note note) {
                // entityManager.persist takes a transient instance and makes it persistent
                entityManager.persist(note);
        }

        /**
         * Updates an existing Note in the database (UPDATE).
         */
        @Transactional
        public Note updateNote(Note note) {
                // entityManager.merge copies the state of the given entity
                // into the current persistence context and returns the updated managed instance
                return entityManager.merge(note);
        }

        /**
         * Removes a Note from the database (DELETE).
         */
        @Transactional
        public void deleteNote(Note note) {
                // Entity must be managed before it can be removed.
                // If it's detached, we merge it first, then remove it.
                if (!entityManager.contains(note)) {
                        note = entityManager.merge(note);
                }
                entityManager.remove(note);
        }
}
