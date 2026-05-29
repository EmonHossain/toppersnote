package com.sharenote.ai;

import com.sharenote.ai.dto.AiProviderResponse;
import com.sharenote.ai.dto.NoteAiRequest;
import com.sharenote.ai.dto.NoteAiResponse;
import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.note.Note;
import com.sharenote.note.NoteNotFoundException;
import com.sharenote.note.NoteRepository;
import com.sharenote.storage.NoteFileStorage;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import com.sharenote.verification.EmailNotVerifiedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
public class NoteAiService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteFileStorage noteFileStorage;
    private final List<AiModelClient> aiModelClients;
    private final AiProperties aiProperties;
    private final AuditPublisher auditPublisher;
    private final Clock clock;

    public NoteAiService(
            NoteRepository noteRepository,
            UserRepository userRepository,
            NoteFileStorage noteFileStorage,
            List<AiModelClient> aiModelClients,
            AiProperties aiProperties,
            AuditPublisher auditPublisher
    ) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.noteFileStorage = noteFileStorage;
        this.aiModelClients = aiModelClients;
        this.aiProperties = aiProperties;
        this.auditPublisher = auditPublisher;
        this.clock = Clock.systemUTC();
    }

    public List<AiProviderResponse> listProviders() {
        return Arrays.stream(AiProvider.values())
                .sorted(Comparator.comparing(AiProvider::name))
                .map(provider -> new AiProviderResponse(
                        provider,
                        provider.getDisplayName(),
                        "API_KEY",
                        provider.isFileAttachmentSupported(),
                        List.of("apiKey"),
                        provider.getSuggestedModels()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteAiResponse askNote(Long noteId, NoteAiRequest request) {
        validateRequest(request);
        User currentUser = getCurrentUser();
        requireVerifiedEmail(currentUser);
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));
        checkNoteAccess(note, currentUser);

        AttachedNoteFile attachedNoteFile = null;
        if (request.attachNote()) {
            if (!request.provider().isFileAttachmentSupported()) {
                throw new AiRequestValidationException("Selected AI provider does not support file attachments");
            }
            if (note.getFileSize() > aiProperties.getMaxAttachmentBytes()) {
                throw new AiRequestValidationException("Note exceeds maximum AI attachment size");
            }
            attachedNoteFile = new AttachedNoteFile(
                    note.getOriginalFileName(),
                    note.getContentType(),
                    noteFileStorage.read(note)
            );
        }

        AiModelClient client = aiModelClients.stream()
                .filter(candidate -> candidate.supports(request.provider()))
                .findFirst()
                .orElseThrow(() -> new AiRequestValidationException("AI provider is not supported"));

        String answer = client.complete(new AiModelInvocation(
                request.provider(),
                request.model().trim(),
                request.prompt().trim(),
                request.apiKey().trim(),
                request.attachNote(),
                attachedNoteFile
        ));

        auditPublisher.publish(
                AuditAction.NOTE_AI_REQUESTED,
                currentUser,
                "NOTE",
                note.getId(),
                "AI requested for note",
                "provider=" + request.provider().name() + ",model=" + request.model().trim()
                        + ",attachNote=" + request.attachNote()
        );
        log.info(
                "AI requested for note userId={} noteId={} provider={} model={} attachNote={}",
                currentUser.getId(),
                note.getId(),
                request.provider().name(),
                request.model().trim(),
                request.attachNote()
        );

        return new NoteAiResponse(
                note.getId(),
                request.provider(),
                request.model().trim(),
                answer,
                Instant.now(clock)
        );
    }

    private void validateRequest(NoteAiRequest request) {
        if (request == null) {
            throw new AiRequestValidationException("AI request is required");
        }
        if (request.provider() == null) {
            throw new AiRequestValidationException("AI provider is required");
        }
        if (!StringUtils.hasText(request.model())) {
            throw new AiRequestValidationException("Model is required");
        }
        if (!StringUtils.hasText(request.prompt())) {
            throw new AiRequestValidationException("Prompt is required");
        }
        if (request.prompt().trim().length() > aiProperties.getMaxPromptChars()) {
            throw new AiRequestValidationException("Prompt exceeds maximum allowed length");
        }
        if (!StringUtils.hasText(request.apiKey())) {
            throw new AiRequestValidationException("API key is required");
        }
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new com.sharenote.note.CurrentUserNotFoundException();
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(com.sharenote.note.CurrentUserNotFoundException::new);
    }

    private void checkNoteAccess(Note note, User user) {
        if (user.getRoles().contains(Role.ADMIN)) {
            return;
        }
        if (note.getUploadedBy() == user) {
            return;
        }
        if (note.getUploadedBy().getId() != null && note.getUploadedBy().getId().equals(user.getId())) {
            return;
        }
        boolean isPeer = note.getInstitution().equalsIgnoreCase(user.getInstitution())
                && note.getDegreeProgram().equalsIgnoreCase(user.getDegreeProgram())
                && note.getSemester().equalsIgnoreCase(user.getCurrentSemester())
                && note.getYear().equalsIgnoreCase(user.getCurrentYear());

        if (!isPeer) {
            throw new SecurityException("You do not have permission to access this note");
        }
    }

    private void requireVerifiedEmail(User user) {
        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }
    }
}
