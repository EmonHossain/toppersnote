package com.sharenote.note;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteUpvoteRepository extends JpaRepository<NoteUpvote, Long> {

    boolean existsByNoteIdAndUserId(Long noteId, Long userId);

    long countByNoteId(Long noteId);
}
