package com.sharenote.note;

import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.notification.NotificationPublisher;
import com.sharenote.note.dto.NoteCommentResponse;
import com.sharenote.note.dto.NoteUpvoteResponse;
import com.sharenote.note.dto.TakeALookRequest;
import com.sharenote.note.dto.TakeALookSuggestionResponse;
import com.sharenote.user.User;
import com.sharenote.user.UserNotFoundException;
import com.sharenote.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NoteInteractionService {

    private final NoteRepository noteRepository;
    private final NoteCommentRepository noteCommentRepository;
    private final NoteUpvoteRepository noteUpvoteRepository;
    private final NoteTakeALookSuggestionRepository takeALookSuggestionRepository;
    private final UserRepository userRepository;
    private final NotificationPublisher notificationPublisher;
    private final AuditPublisher auditPublisher;
    private final Clock clock;

    public NoteInteractionService(
            NoteRepository noteRepository,
            NoteCommentRepository noteCommentRepository,
            NoteUpvoteRepository noteUpvoteRepository,
            NoteTakeALookSuggestionRepository takeALookSuggestionRepository,
            UserRepository userRepository,
            NotificationPublisher notificationPublisher,
            AuditPublisher auditPublisher
    ) {
        this.noteRepository = noteRepository;
        this.noteCommentRepository = noteCommentRepository;
        this.noteUpvoteRepository = noteUpvoteRepository;
        this.takeALookSuggestionRepository = takeALookSuggestionRepository;
        this.userRepository = userRepository;
        this.notificationPublisher = notificationPublisher;
        this.auditPublisher = auditPublisher;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public NoteCommentResponse addComment(Long noteId, String content) {
        Note note = getNote(noteId);
        User author = getCurrentUser();
        NoteComment comment = new NoteComment(
                note,
                author,
                null,
                requireInteractionText(content, "Comment is required", 1000),
                Instant.now(clock)
        );
        NoteComment savedComment = noteCommentRepository.save(comment);
        auditPublisher.publish(AuditAction.COMMENT_CREATED, author, "NOTE", note.getId(), "Comment added to note");
        return toCommentResponse(savedComment, List.of());
    }

    @Transactional
    public NoteCommentResponse addReply(Long noteId, Long commentId, String content) {
        Note note = getNote(noteId);
        User author = getCurrentUser();
        NoteComment parentComment = noteCommentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        if (!Objects.equals(parentComment.getNote().getId(), note.getId())) {
            throw new InvalidNoteInteractionException("Comment does not belong to this note");
        }
        if (parentComment.getParentComment() != null) {
            throw new InvalidNoteInteractionException("Replies can only be added to top-level comments");
        }

        NoteComment reply = new NoteComment(
                note,
                author,
                parentComment,
                requireInteractionText(content, "Reply is required", 1000),
                Instant.now(clock)
        );
        NoteComment savedReply = noteCommentRepository.save(reply);
        auditPublisher.publish(AuditAction.REPLY_CREATED, author, "NOTE", note.getId(), "Reply added to note comment");
        return toCommentResponse(savedReply, List.of());
    }

    @Transactional(readOnly = true)
    public List<NoteCommentResponse> getComments(Long noteId) {
        getNote(noteId);
        List<NoteComment> comments = noteCommentRepository.findByNoteIdOrderByCreatedAtAsc(noteId);

        Map<Long, List<NoteComment>> repliesByParentId = comments.stream()
                .filter(comment -> comment.getParentComment() != null)
                .collect(Collectors.groupingBy(comment -> comment.getParentComment().getId()));

        return comments.stream()
                .filter(comment -> comment.getParentComment() == null)
                .map(comment -> toCommentResponse(comment, repliesByParentId.getOrDefault(comment.getId(), List.of())))
                .toList();
    }

    @Transactional
    public NoteUpvoteResponse upvote(Long noteId) {
        Note note = getNote(noteId);
        User user = getCurrentUser();

        Long resolvedNoteId = resolveNoteId(note, noteId);
        if (!noteUpvoteRepository.existsByNoteIdAndUserId(resolvedNoteId, user.getId())) {
            noteUpvoteRepository.save(new NoteUpvote(note, user, Instant.now(clock)));
            auditPublisher.publish(AuditAction.NOTE_UPVOTED, user, "NOTE", resolvedNoteId, "Note upvoted");
        }

        return new NoteUpvoteResponse(resolvedNoteId, noteUpvoteRepository.countByNoteId(resolvedNoteId), true);
    }

    @Transactional
    public List<TakeALookSuggestionResponse> suggestTakeALook(Long noteId, TakeALookRequest request) {
        Note note = getNote(noteId);
        User suggestedBy = getCurrentUser();
        Set<Long> recipientIds = normalizeRecipientIds(request.recipientUserIds(), suggestedBy.getId());
        Map<Long, User> recipientsById = userRepository.findAllById(recipientIds)
                .stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        for (Long recipientId : recipientIds) {
            if (!recipientsById.containsKey(recipientId)) {
                throw new UserNotFoundException(recipientId);
            }
        }

        String normalizedMessage = normalizeOptional(request.message(), 500);
        List<TakeALookSuggestionResponse> responses = new ArrayList<>();
        List<User> newlyMentionedUsers = new ArrayList<>();
        for (Long recipientId : recipientIds) {
            User suggestedTo = recipientsById.get(recipientId);
            NoteTakeALookSuggestion suggestion = takeALookSuggestionRepository
                    .findByNoteIdAndSuggestedByIdAndSuggestedToId(noteId, suggestedBy.getId(), suggestedTo.getId())
                    .orElseGet(() -> {
                        newlyMentionedUsers.add(suggestedTo);
                        return takeALookSuggestionRepository.save(new NoteTakeALookSuggestion(
                            note,
                            suggestedBy,
                            suggestedTo,
                            normalizedMessage,
                            Instant.now(clock)
                        ));
                    });
            responses.add(toTakeALookResponse(suggestion));
        }
        notificationPublisher.notifyTakeALook(note, suggestedBy, newlyMentionedUsers, normalizedMessage);
        if (!newlyMentionedUsers.isEmpty()) {
            auditPublisher.publish(
                    AuditAction.TAKE_A_LOOK_SUGGESTED,
                    suggestedBy,
                    "NOTE",
                    noteId,
                    "Take a look suggested",
                    "recipientCount=" + newlyMentionedUsers.size()
            );
        }

        return responses;
    }

    @Transactional(readOnly = true)
    public List<TakeALookSuggestionResponse> getMyTakeALookSuggestions() {
        User currentUser = getCurrentUser();
        return takeALookSuggestionRepository.findBySuggestedToIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toTakeALookResponse)
                .toList();
    }

    private Note getNote(Long noteId) {
        return noteRepository.findById(noteId).orElseThrow(() -> new NoteNotFoundException(noteId));
    }

    private Long resolveNoteId(Note note, Long requestedNoteId) {
        return note.getId() == null ? requestedNoteId : note.getId();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CurrentUserNotFoundException();
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(CurrentUserNotFoundException::new);
    }

    private Set<Long> normalizeRecipientIds(Set<Long> recipientUserIds, Long currentUserId) {
        if (recipientUserIds == null || recipientUserIds.isEmpty()) {
            throw new InvalidNoteInteractionException("At least one user must be selected");
        }
        if (recipientUserIds.size() > 20) {
            throw new InvalidNoteInteractionException("You can suggest at most 20 users at a time");
        }
        if (recipientUserIds.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidNoteInteractionException("Recipient user id is required");
        }

        Set<Long> normalizedIds = recipientUserIds.stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedIds.contains(currentUserId)) {
            throw new InvalidNoteInteractionException("You cannot suggest a note to yourself");
        }
        return normalizedIds;
    }

    private String requireInteractionText(String value, String message, int maxLength) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidNoteInteractionException(message);
        }
        String trimmedValue = value.trim();
        if (trimmedValue.length() > maxLength) {
            throw new InvalidNoteInteractionException("Text must be at most " + maxLength + " characters");
        }
        return trimmedValue;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.length() > maxLength) {
            throw new InvalidNoteInteractionException("Message must be at most " + maxLength + " characters");
        }
        return trimmedValue;
    }

    private NoteCommentResponse toCommentResponse(NoteComment comment, List<NoteComment> replies) {
        return new NoteCommentResponse(
                comment.getId(),
                comment.getNote().getId(),
                comment.getParentComment() == null ? null : comment.getParentComment().getId(),
                comment.getAuthor().getId(),
                formatName(comment.getAuthor()),
                comment.getContent(),
                comment.getCreatedAt(),
                replies.stream().map(reply -> toCommentResponse(reply, List.of())).toList()
        );
    }

    private TakeALookSuggestionResponse toTakeALookResponse(NoteTakeALookSuggestion suggestion) {
        return new TakeALookSuggestionResponse(
                suggestion.getId(),
                suggestion.getNote().getId(),
                suggestion.getNote().getSubjectClass(),
                suggestion.getSuggestedBy().getId(),
                formatName(suggestion.getSuggestedBy()),
                suggestion.getSuggestedTo().getId(),
                formatName(suggestion.getSuggestedTo()),
                suggestion.getMessage(),
                suggestion.getCreatedAt()
        );
    }

    private String formatName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
