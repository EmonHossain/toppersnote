package com.sharenote.ai;

import com.sharenote.ai.dto.AiProviderResponse;
import com.sharenote.ai.dto.NoteAiRequest;
import com.sharenote.ai.dto.NoteAiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class NoteAiController {

    private final NoteAiService noteAiService;

    public NoteAiController(NoteAiService noteAiService) {
        this.noteAiService = noteAiService;
    }

    @GetMapping("/ai/providers")
    public ResponseEntity<List<AiProviderResponse>> listProviders() {
        return ResponseEntity.ok(noteAiService.listProviders());
    }

    @PostMapping("/notes/{noteId}/ai")
    public ResponseEntity<NoteAiResponse> askNote(
            @PathVariable Long noteId,
            @Valid @RequestBody NoteAiRequest request
    ) {
        return ResponseEntity.ok(noteAiService.askNote(noteId, request));
    }
}
