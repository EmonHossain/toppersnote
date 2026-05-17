package com.sharenote.note;

import com.sharenote.note.dto.NoteResponse;
import com.sharenote.note.dto.NoteUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subjectClass") String subjectClass,
            @RequestParam("semester") String semester,
            @RequestParam("year") String year
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(noteService.uploadNote(file, subjectClass, semester, year));
    }

    @GetMapping
    public ResponseEntity<List<NoteResponse>> getVisibleNotes(
            @RequestParam("subjectClass") String subjectClass,
            @RequestParam("semester") String semester,
            @RequestParam("year") String year
    ) {
        return ResponseEntity.ok(noteService.getVisibleNotes(subjectClass, semester, year));
    }
}
