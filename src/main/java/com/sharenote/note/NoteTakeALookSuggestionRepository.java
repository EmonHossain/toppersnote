package com.sharenote.note;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteTakeALookSuggestionRepository extends JpaRepository<NoteTakeALookSuggestion, Long> {

    boolean existsByNoteIdAndSuggestedByIdAndSuggestedToId(Long noteId, Long suggestedById, Long suggestedToId);

    Optional<NoteTakeALookSuggestion> findByNoteIdAndSuggestedByIdAndSuggestedToId(
            Long noteId,
            Long suggestedById,
            Long suggestedToId
    );

    List<NoteTakeALookSuggestion> findBySuggestedToIdOrderByCreatedAtDesc(Long suggestedToId);

    long countByNoteId(Long noteId);
}
