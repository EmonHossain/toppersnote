package com.sharenote.note.dto;

public record NoteUpvoteResponse(
        Long noteId,
        long upvoteCount,
        boolean upvotedByCurrentUser
) {
}
