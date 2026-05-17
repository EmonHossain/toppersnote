package com.sharenote.note;

import com.sharenote.note.dto.NoteCommentResponse;
import com.sharenote.note.dto.NoteUpvoteResponse;
import com.sharenote.note.dto.TakeALookRequest;
import com.sharenote.note.dto.TakeALookSuggestionResponse;
import com.sharenote.notification.NotificationPublisher;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteInteractionServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private NoteCommentRepository noteCommentRepository;

    @Mock
    private NoteUpvoteRepository noteUpvoteRepository;

    @Mock
    private NoteTakeALookSuggestionRepository takeALookSuggestionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    private NoteInteractionService noteInteractionService;

    @BeforeEach
    void setUp() {
        noteInteractionService = new NoteInteractionService(
                noteRepository,
                noteCommentRepository,
                noteUpvoteRepository,
                takeALookSuggestionRepository,
                userRepository,
                notificationPublisher
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "amina@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void addCommentStoresTrimmedOpinionForNote() {
        Note note = note();
        User author = user(1L, "Amina", "Rahman", "amina@example.com");

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(author));
        when(noteCommentRepository.save(any(NoteComment.class))).thenAnswer(invocation -> {
            NoteComment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 100L);
            return comment;
        });

        NoteCommentResponse response = noteInteractionService.addComment(10L, " Very helpful ");

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.noteId()).isEqualTo(10L);
        assertThat(response.authorUserId()).isEqualTo(1L);
        assertThat(response.content()).isEqualTo("Very helpful");
        assertThat(response.replies()).isEmpty();
    }

    @Test
    void addReplyRejectsCommentFromAnotherNote() {
        Note note = note();
        Note otherNote = note(20L);
        User author = user(1L, "Amina", "Rahman", "amina@example.com");
        NoteComment parentComment = new NoteComment(
                otherNote,
                author,
                null,
                "Different note",
                Instant.parse("2026-05-18T10:15:30Z")
        );
        ReflectionTestUtils.setField(parentComment, "id", 200L);

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(author));
        when(noteCommentRepository.findById(200L)).thenReturn(Optional.of(parentComment));

        assertThatThrownBy(() -> noteInteractionService.addReply(10L, 200L, "I agree"))
                .isInstanceOf(InvalidNoteInteractionException.class)
                .hasMessage("Comment does not belong to this note");

        verify(noteCommentRepository, never()).save(any());
    }

    @Test
    void upvoteCreatesOnlyOneLikePerUser() {
        Note note = note();
        User user = user(1L, "Amina", "Rahman", "amina@example.com");

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        when(noteUpvoteRepository.existsByNoteIdAndUserId(10L, 1L)).thenReturn(false);
        when(noteUpvoteRepository.countByNoteId(10L)).thenReturn(1L);

        NoteUpvoteResponse response = noteInteractionService.upvote(10L);

        assertThat(response.noteId()).isEqualTo(10L);
        assertThat(response.upvoteCount()).isEqualTo(1L);
        assertThat(response.upvotedByCurrentUser()).isTrue();
        verify(noteUpvoteRepository).save(any(NoteUpvote.class));
    }

    @Test
    void upvoteIsIdempotentForExistingLike() {
        Note note = note();
        User user = user(1L, "Amina", "Rahman", "amina@example.com");

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        when(noteUpvoteRepository.existsByNoteIdAndUserId(10L, 1L)).thenReturn(true);
        when(noteUpvoteRepository.countByNoteId(10L)).thenReturn(3L);

        NoteUpvoteResponse response = noteInteractionService.upvote(10L);

        assertThat(response.upvoteCount()).isEqualTo(3L);
        verify(noteUpvoteRepository, never()).save(any());
    }

    @Test
    void suggestTakeALookCreatesSuggestionForEachRecipient() {
        Note note = note();
        User sender = user(1L, "Amina", "Rahman", "amina@example.com");
        User recipientOne = user(2L, "Karim", "Said", "karim@example.com");
        User recipientTwo = user(3L, "Maya", "Khan", "maya@example.com");

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(sender));
        when(userRepository.findAllById(Set.of(2L, 3L))).thenReturn(List.of(recipientOne, recipientTwo));
        when(takeALookSuggestionRepository.findByNoteIdAndSuggestedByIdAndSuggestedToId(10L, 1L, 2L))
                .thenReturn(Optional.empty());
        when(takeALookSuggestionRepository.findByNoteIdAndSuggestedByIdAndSuggestedToId(10L, 1L, 3L))
                .thenReturn(Optional.empty());
        when(takeALookSuggestionRepository.save(any(NoteTakeALookSuggestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<TakeALookSuggestionResponse> responses = noteInteractionService.suggestTakeALook(
                10L,
                new TakeALookRequest(Set.of(2L, 3L), " Worth checking ")
        );

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(TakeALookSuggestionResponse::suggestedToUserId)
                .containsExactly(2L, 3L);
        assertThat(responses).extracting(TakeALookSuggestionResponse::message)
                .containsOnly("Worth checking");
    }

    @Test
    void suggestTakeALookRejectsSelfSuggestion() {
        Note note = note();
        User sender = user(1L, "Amina", "Rahman", "amina@example.com");

        when(noteRepository.findById(10L)).thenReturn(Optional.of(note));
        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(sender));

        assertThatThrownBy(() -> noteInteractionService.suggestTakeALook(
                10L,
                new TakeALookRequest(Set.of(1L), null)
        ))
                .isInstanceOf(InvalidNoteInteractionException.class)
                .hasMessage("You cannot suggest a note to yourself");

        verify(takeALookSuggestionRepository, never()).save(any());
    }

    private Note note() {
        return note(10L);
    }

    private Note note(Long id) {
        Note note = new Note(
                "Mathematics",
                "3",
                "2026",
                "calculus.pdf",
                "stored.pdf",
                "application/pdf",
                123,
                "uploads/notes/stored.pdf",
                user(9L, "Uploader", "User", "uploader@example.com"),
                Instant.parse("2026-05-18T10:15:30Z")
        );
        ReflectionTestUtils.setField(note, "id", id);
        return note;
    }

    private User user(Long id, String firstName, String lastName, String email) {
        User user = new User(
                firstName,
                null,
                lastName,
                email,
                "hashed-password",
                "university",
                "3",
                "+491234567890",
                "Germany",
                Set.of(Role.USER)
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
