package com.sharenote.note.dto;

import java.time.Instant;
import java.util.List;

public record NoteCommentResponse(
        Long id,
        Long noteId,
        Long parentCommentId,
        Long authorUserId,
        String authorName,
        String content,
        Instant createdAt,
        List<NoteCommentResponse> replies
) {
}
