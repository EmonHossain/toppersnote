package com.sharenote.note;

import com.sharenote.note.dto.CreateCommentRequest;
import com.sharenote.note.dto.NoteCommentResponse;
import com.sharenote.note.dto.NoteResponse;
import com.sharenote.note.dto.NoteUploadResponse;
import com.sharenote.note.dto.NoteUpvoteResponse;
import com.sharenote.note.dto.TakeALookRequest;
import com.sharenote.note.dto.TakeALookSuggestionResponse;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;
    private final NoteInteractionService noteInteractionService;

    public NoteController(NoteService noteService, NoteInteractionService noteInteractionService) {
        this.noteService = noteService;
        this.noteInteractionService = noteInteractionService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subjectClass") String subjectClass,
            @RequestParam("semester") String semester,
            @RequestParam("year") String year) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.uploadNote(file, subjectClass, semester, year));
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getVisibleNotes(
            @RequestParam("subjectClass") String subjectClass,
            @RequestParam("semester") String semester,
            @RequestParam("year") String year) {
        return ResponseEntity.ok(noteService.getVisibleNotes(subjectClass, semester, year));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id) {
        NoteService.DownloadDetails details = noteService.getDownloadDetails(id);
        if (details.isPresignedUrl()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, details.pathOrUrl())
                    .build();
        } else {
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(details.pathOrUrl());
                Resource resource = new UrlResource(path.toUri());
                if (resource.exists() || resource.isReadable()) {
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(details.contentType()))
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + details.originalFileName() + "\"")
                            .body(resource);
                } else {
                    throw new com.sharenote.storage.FileStorageException("File not found locally", null);
                }
            } catch (java.net.MalformedURLException e) {
                throw new com.sharenote.storage.FileStorageException("Could not read file", e);
            }
        }
    }

    @PostMapping("/{noteId}/comments")
    public ResponseEntity<NoteCommentResponse> addComment(
            @PathVariable Long noteId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteInteractionService.addComment(noteId, request.content()));
    }

    @PostMapping("/{noteId}/comments/{commentId}/replies")
    public ResponseEntity<NoteCommentResponse> addReply(
            @PathVariable Long noteId,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteInteractionService.addReply(noteId, commentId, request.content()));
    }

    @GetMapping("/{noteId}/comments")
    public ResponseEntity<List<NoteCommentResponse>> getComments(@PathVariable Long noteId) {
        return ResponseEntity.ok(noteInteractionService.getComments(noteId));
    }

    @PostMapping("/{noteId}/upvotes")
    public ResponseEntity<NoteUpvoteResponse> upvote(@PathVariable Long noteId) {
        return ResponseEntity.ok(noteInteractionService.upvote(noteId));
    }

    @PostMapping("/{noteId}/take-a-look")
    public ResponseEntity<List<TakeALookSuggestionResponse>> suggestTakeALook(
            @PathVariable Long noteId,
            @Valid @RequestBody TakeALookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteInteractionService.suggestTakeALook(noteId, request));
    }

    @GetMapping("/take-a-look")
    public ResponseEntity<List<TakeALookSuggestionResponse>> getMyTakeALookSuggestions() {
        return ResponseEntity.ok(noteInteractionService.getMyTakeALookSuggestions());
    }
}
