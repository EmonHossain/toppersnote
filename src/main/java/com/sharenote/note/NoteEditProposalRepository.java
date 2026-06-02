package com.sharenote.note;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteEditProposalRepository extends JpaRepository<NoteEditProposal, Long> {

    List<NoteEditProposal> findByNoteIdOrderByCreatedAtDesc(Long noteId);

    List<NoteEditProposal> findByProposerIdOrderByCreatedAtDesc(Long proposerId);
}
