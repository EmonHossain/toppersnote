package com.sharenote.note;

import com.sharenote.academic.AcademicClassRegistrar;
import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.notification.NotificationPublisher;
import com.sharenote.note.dto.NoteUploadResponse;
import com.sharenote.note.dto.NoteResponse;
import com.sharenote.storage.InvalidFileException;
import com.sharenote.storage.NoteFileStorage;
import com.sharenote.storage.StoredFile;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteFileStorage noteFileStorage;
    private final NoteCommentRepository noteCommentRepository;
    private final NoteUpvoteRepository noteUpvoteRepository;
    private final NoteTakeALookSuggestionRepository takeALookSuggestionRepository;
    private final NotificationPublisher notificationPublisher;
    private final AcademicClassRegistrar academicClassRegistrar;
    private final AuditPublisher auditPublisher;
    private final Clock clock;

    public NoteService(
            NoteRepository noteRepository,
            UserRepository userRepository,
            NoteFileStorage noteFileStorage,
            NoteCommentRepository noteCommentRepository,
            NoteUpvoteRepository noteUpvoteRepository,
            NoteTakeALookSuggestionRepository takeALookSuggestionRepository,
            NotificationPublisher notificationPublisher,
            AcademicClassRegistrar academicClassRegistrar,
            AuditPublisher auditPublisher
    ) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.noteFileStorage = noteFileStorage;
        this.noteCommentRepository = noteCommentRepository;
        this.noteUpvoteRepository = noteUpvoteRepository;
        this.takeALookSuggestionRepository = takeALookSuggestionRepository;
        this.notificationPublisher = notificationPublisher;
        this.academicClassRegistrar = academicClassRegistrar;
        this.auditPublisher = auditPublisher;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public NoteUploadResponse uploadNote(
            MultipartFile file,
            String subjectClass,
            String semester,
            String year
    ) {
        String normalizedSubjectClass = requireText(subjectClass, "Subject/class is required");
        String normalizedSemester = requireText(semester, "Semester is required");
        String normalizedYear = requireText(year, "Year is required");

        User uploadedBy = getCurrentUser();
        StoredFile storedFile = noteFileStorage.store(file);

        try {
            Note note = new Note(
                    normalizedSubjectClass,
                    uploadedBy.getInstitution(),
                    uploadedBy.getDegreeProgram(),
                    normalizedSemester,
                    normalizedYear,
                    storedFile.originalFileName(),
                    storedFile.storedFileName(),
                    storedFile.contentType(),
                    storedFile.fileSize(),
                    storedFile.storageLocation(),
                    uploadedBy,
                    Instant.now(clock)
            );
            Note savedNote = noteRepository.save(note);
            academicClassRegistrar.registerMatchingUsers(savedNote);
            notificationPublisher.notifyNewNote(savedNote);
            auditPublisher.publish(
                    AuditAction.NOTE_UPLOADED,
                    uploadedBy,
                    "NOTE",
                    savedNote.getId(),
                    "Note uploaded",
                    "subjectClass=" + savedNote.getSubjectClass()
            );
            return toResponse(savedNote);
        } catch (RuntimeException exception) {
            noteFileStorage.deleteIfExists(storedFile);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getVisibleNotes(String subjectClass, String semester, String year) {
        User currentUser = getCurrentUser();

        String normalizedSubjectClass = requireQueryText(subjectClass, "Subject/class is required");
        String normalizedSemester = requireQueryText(semester, "Semester is required");
        String normalizedYear = requireQueryText(year, "Year is required");

        return noteRepository
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseOrderByCreatedAtDesc(
                        currentUser.getInstitution(),
                        currentUser.getDegreeProgram(),
                        normalizedSubjectClass,
                        normalizedSemester,
                        normalizedYear
                )
                .stream()
                .map(this::toNoteResponse)
                .toList();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CurrentUserNotFoundException();
        }

        return userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(CurrentUserNotFoundException::new);
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidFileException(message);
        }
        return value.trim();
    }

    private String requireQueryText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new InvalidNoteQueryException(message);
        }
        return value.trim();
    }

    private NoteUploadResponse toResponse(Note note) {
        return new NoteUploadResponse(
                note.getId(),
                note.getSubjectClass(),
                note.getInstitution(),
                note.getDegreeProgram(),
                note.getSemester(),
                note.getYear(),
                note.getOriginalFileName(),
                note.getContentType(),
                note.getFileSize(),
                note.getUploadedBy().getId(),
                note.getCreatedAt()
        );
    }

    private NoteResponse toNoteResponse(Note note) {
        User uploadedBy = note.getUploadedBy();
        return new NoteResponse(
                note.getId(),
                note.getSubjectClass(),
                note.getInstitution(),
                note.getDegreeProgram(),
                note.getSemester(),
                note.getYear(),
                note.getOriginalFileName(),
                note.getContentType(),
                note.getFileSize(),
                uploadedBy.getId(),
                formatName(uploadedBy),
                noteCommentRepository.countByNoteId(note.getId()),
                noteUpvoteRepository.countByNoteId(note.getId()),
                takeALookSuggestionRepository.countByNoteId(note.getId()),
                note.getCreatedAt()
        );
    }

    private String formatName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
