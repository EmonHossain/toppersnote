package com.sharenote.admin.retention;

import com.sharenote.admin.retention.dto.CancelNoteRetentionRequest;
import com.sharenote.admin.retention.dto.NoteRetentionCandidateResponse;
import com.sharenote.admin.retention.dto.RetentionJobResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/note-retention")
@RequiredArgsConstructor
@Tag(name = "Admin Note Retention", description = "Admin observation endpoints for automatic note retention jobs.")
@SecurityRequirement(name = "bearerAuth")
public class NoteRetentionController {

    private final NoteRetentionService noteRetentionService;
    private final NoteRetentionJobService noteRetentionJobService;

    // getCandidates: Lists active notes scheduled for automatic removal.
    @GetMapping("/candidates")
    @Operation(summary = "List note retention candidates")
    public ResponseEntity<List<NoteRetentionCandidateResponse>> getCandidates() {
        return ResponseEntity.ok(noteRetentionService.listCandidates());
    }

    // cancelCandidate: Cancels a scheduled removal for future emergency intervention.
    @PatchMapping("/candidates/{candidateId}/cancel")
    @Operation(summary = "Cancel a note retention candidate")
    public ResponseEntity<NoteRetentionCandidateResponse> cancelCandidate(
            @PathVariable Long candidateId,
            @Valid @RequestBody CancelNoteRetentionRequest request
    ) {
        return ResponseEntity.ok(noteRetentionService.cancelCandidate(candidateId, request));
    }

    // getJobs: Lists Quartz jobs that drive automatic note retention.
    @GetMapping("/jobs")
    @Operation(summary = "List note retention jobs")
    public ResponseEntity<List<RetentionJobResponse>> getJobs() {
        return ResponseEntity.ok(noteRetentionJobService.listJobs());
    }
}
