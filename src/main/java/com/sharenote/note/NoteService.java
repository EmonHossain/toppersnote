package com.sharenote.note;

import com.sharenote.academic.AcademicClassRegistrar;
import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.notification.NotificationPublisher;
import com.sharenote.note.dto.NoteDownloadBatchResponse;
import com.sharenote.note.dto.NoteDownloadItemResponse;
import com.sharenote.note.dto.NoteUploadResponse;
import com.sharenote.note.dto.NoteResponse;
import com.sharenote.note.dto.NoteVersionResponse;
import com.sharenote.note.dto.NoteEditProposalResponse;
import com.sharenote.note.dto.RejectProposalRequest;
import com.sharenote.note.dto.RecentlyUploadedNoteResponse;
import com.sharenote.note.dto.SelectedNotesDownloadRequest;
import com.sharenote.storage.FileValidationService;
import com.sharenote.storage.InvalidFileException;
import com.sharenote.storage.NoteFileStorage;
import com.sharenote.storage.StoredFile;
import com.sharenote.storage.ValidatedFile;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import com.sharenote.verification.EmailNotVerifiedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NoteService {

    private static final int MAX_BATCH_DOWNLOAD_SIZE = 100;

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final NoteFileStorage noteFileStorage;
    private final NoteCommentRepository noteCommentRepository;
    private final NoteUpvoteRepository noteUpvoteRepository;
    private final NoteTakeALookSuggestionRepository takeALookSuggestionRepository;
    private final NotificationPublisher notificationPublisher;
    private final AcademicClassRegistrar academicClassRegistrar;
    private final AuditPublisher auditPublisher;
    private final FileValidationService fileValidationService;
    private final NoteEditProposalRepository noteEditProposalRepository;
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
            AuditPublisher auditPublisher,
            FileValidationService fileValidationService,
            NoteEditProposalRepository noteEditProposalRepository
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
        this.fileValidationService = fileValidationService;
        this.noteEditProposalRepository = noteEditProposalRepository;
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
        requireVerifiedEmail(uploadedBy);

        ValidatedFile validatedFile = fileValidationService.validate(file);

        // Calculate SHA-256 hash of the validated file bytes
        String fileHash;
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(validatedFile.bytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            fileHash = hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new com.sharenote.storage.FileStorageException("SHA-256 algorithm not found", e);
        }

        java.util.Optional<Note> existingNote = noteRepository.findFirstByFileHash(fileHash);
        StoredFile storedFile = null;
        boolean isNewFile = false;

        if (existingNote.isPresent()) {
            Note existing = existingNote.get();
            storedFile = new StoredFile(
                    validatedFile.originalFileName(),
                    existing.getStoredFileName(),
                    existing.getContentType(),
                    existing.getFileSize(),
                    existing.getStoredFileName(),
                    existing.getStoragePath()
            );
        } else {
            storedFile = noteFileStorage.store(file);
            isNewFile = true;
        }

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
            note.setFileHash(fileHash);
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
            if (isNewFile && storedFile != null) {
                noteFileStorage.deleteIfExists(storedFile);
            }
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
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseAndLatestTrueOrderByCreatedAtDesc(
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

    @Transactional(readOnly = true)
    public List<RecentlyUploadedNoteResponse> getRecentlyUploadedNotes(String subjectClass) {
        User currentUser = getCurrentUser();
        String normalizedSubjectClass = requireQueryText(subjectClass, "Subject/class is required");

        List<RecentlyUploadedNoteResponse> responses = noteRepository
                .findTop20ByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseAndLatestTrueOrderByCreatedAtDesc(
                        currentUser.getInstitution(),
                        currentUser.getDegreeProgram(),
                        normalizedSubjectClass,
                        currentUser.getCurrentSemester(),
                        currentUser.getCurrentYear()
                )
                .stream()
                .map(this::toRecentlyUploadedNoteResponse)
                .toList();

        log.info(
                "Recently uploaded notes requested userId={} userEmail={} subjectClass={} count={}",
                currentUser.getId(),
                currentUser.getEmail(),
                normalizedSubjectClass,
                responses.size()
        );
        return responses;
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

    @Transactional(readOnly = true)
    public DownloadDetails getDownloadDetails(Long noteId) {
        User currentUser = getCurrentUser();
        requireVerifiedEmail(currentUser);
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        checkNoteAccess(note, currentUser);

        String downloadUrl = noteFileStorage.generateDownloadUrl(note.getStoredFileName());
        if (downloadUrl != null) {
            return new DownloadDetails(downloadUrl, true, note.getOriginalFileName(), note.getContentType());
        } else {
            return new DownloadDetails(note.getStoragePath(), false, note.getOriginalFileName(), note.getContentType());
        }
    }

    @Transactional(readOnly = true)
    public NoteDownloadBatchResponse getSelectedDownloadDetails(SelectedNotesDownloadRequest request) {
        User currentUser = getCurrentUser();
        requireVerifiedEmail(currentUser);
        List<Long> noteIds = requireNoteIds(request);

        List<Note> notes = noteRepository.findByIdIn(noteIds);
        Map<Long, Note> notesById = notes.stream()
                .collect(Collectors.toMap(Note::getId, Function.identity()));

        for (Long noteId : noteIds) {
            if (!notesById.containsKey(noteId)) {
                throw new NoteNotFoundException(noteId);
            }
        }

        List<NoteDownloadItemResponse> items = noteIds.stream()
                .map(notesById::get)
                .peek(note -> checkNoteAccess(note, currentUser))
                .map(this::toDownloadItemResponse)
                .toList();

        auditPublisher.publish(
                AuditAction.NOTE_DOWNLOAD_PREPARED,
                currentUser,
                "NOTE",
                null,
                "Selected note downloads prepared",
                "count=" + items.size()
        );
        log.info(
                "Selected note download prepared userId={} userEmail={} count={}",
                currentUser.getId(),
                currentUser.getEmail(),
                items.size()
        );
        return new NoteDownloadBatchResponse(items.size(), items);
    }

    @Transactional(readOnly = true)
    public NoteDownloadBatchResponse getAllVisibleDownloadDetails(String subjectClass, String semester, String year) {
        User currentUser = getCurrentUser();
        requireVerifiedEmail(currentUser);

        String normalizedSubjectClass = requireQueryText(subjectClass, "Subject/class is required");
        String normalizedSemester = requireQueryText(semester, "Semester is required");
        String normalizedYear = requireQueryText(year, "Year is required");

        List<NoteDownloadItemResponse> items = noteRepository
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseOrderByCreatedAtDesc(
                        currentUser.getInstitution(),
                        currentUser.getDegreeProgram(),
                        normalizedSubjectClass,
                        normalizedSemester,
                        normalizedYear
                )
                .stream()
                .peek(note -> checkNoteAccess(note, currentUser))
                .map(this::toDownloadItemResponse)
                .toList();

        auditPublisher.publish(
                AuditAction.NOTE_DOWNLOAD_PREPARED,
                currentUser,
                "NOTE",
                null,
                "All visible note downloads prepared",
                "subjectClass=" + normalizedSubjectClass + ",count=" + items.size()
        );
        log.info(
                "All visible note downloads prepared userId={} userEmail={} subjectClass={} semester={} year={} count={}",
                currentUser.getId(),
                currentUser.getEmail(),
                normalizedSubjectClass,
                normalizedSemester,
                normalizedYear,
                items.size()
        );
        return new NoteDownloadBatchResponse(items.size(), items);
    }

    private void checkNoteAccess(Note note, User user) {
        if (user.getRoles().contains(com.sharenote.user.Role.ADMIN)) {
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

    private List<Long> requireNoteIds(SelectedNotesDownloadRequest request) {
        if (request == null || request.noteIds() == null || request.noteIds().isEmpty()) {
            throw new InvalidNoteQueryException("At least one note id is required");
        }
        if (request.noteIds().size() > MAX_BATCH_DOWNLOAD_SIZE) {
            throw new InvalidNoteQueryException("At most 100 notes can be downloaded at once");
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (Long noteId : request.noteIds()) {
            if (noteId == null || noteId <= 0) {
                throw new InvalidNoteQueryException("Note id must be positive");
            }
            uniqueIds.add(noteId);
        }
        return List.copyOf(uniqueIds);
    }

    private NoteDownloadItemResponse toDownloadItemResponse(Note note) {
        String downloadUrl = noteFileStorage.generateDownloadUrl(note.getStoredFileName());
        if (downloadUrl == null) {
            downloadUrl = "/notes/" + note.getId() + "/download";
        }
        return new NoteDownloadItemResponse(
                note.getId(),
                note.getOriginalFileName(),
                note.getContentType(),
                note.getFileSize(),
                downloadUrl
        );
    }

    private RecentlyUploadedNoteResponse toRecentlyUploadedNoteResponse(Note note) {
        User uploadedBy = note.getUploadedBy();
        return new RecentlyUploadedNoteResponse(
                note.getId(),
                note.getOriginalFileName(),
                note.getCreatedAt(),
                uploadedBy.getId(),
                formatName(uploadedBy),
                note.getSubjectClass(),
                note.getDegreeProgram()
        );
    }

    @Transactional
    public NoteResponse uploadVersionDirectly(Long noteId, MultipartFile file, String changeSummary) {
        User currentUser = getCurrentUser();
        requireVerifiedEmail(currentUser);

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        Note rootNote = (note.getParentNote() != null) ? note.getParentNote() : note;

        if (!rootNote.getUploadedBy().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the original author is allowed to edit and upload new versions");
        }

        String normalizedSummary = requireText(changeSummary, "Change summary is required");
        if (normalizedSummary.length() > 1000) {
            throw new InvalidFileException("Change summary must be at most 1000 characters");
        }

        ValidatedFile validatedFile = fileValidationService.validate(file);
        String fileHash = calculateFileHash(validatedFile);

        java.util.Optional<Note> existingNote = noteRepository.findFirstByFileHash(fileHash);
        StoredFile storedFile = null;
        boolean isNewFile = false;

        if (existingNote.isPresent()) {
            Note existing = existingNote.get();
            storedFile = new StoredFile(
                    validatedFile.originalFileName(),
                    existing.getStoredFileName(),
                    existing.getContentType(),
                    existing.getFileSize(),
                    existing.getStoredFileName(),
                    existing.getStoragePath()
            );
        } else {
            storedFile = noteFileStorage.store(file);
            isNewFile = true;
        }

        try {
            List<Note> versions = noteRepository.findAllVersions(rootNote.getId());
            int nextVersionNumber = 2;
            for (Note v : versions) {
                if (v.getVersionNumber() >= nextVersionNumber) {
                    nextVersionNumber = v.getVersionNumber() + 1;
                }
                if (v.isLatest()) {
                    v.setLatest(false);
                    noteRepository.save(v);
                }
            }

            Note newVersion = new Note(
                    rootNote.getSubjectClass(),
                    rootNote.getInstitution(),
                    rootNote.getDegreeProgram(),
                    rootNote.getSemester(),
                    rootNote.getYear(),
                    storedFile.originalFileName(),
                    storedFile.storedFileName(),
                    storedFile.contentType(),
                    storedFile.fileSize(),
                    storedFile.storageLocation(),
                    currentUser,
                    Instant.now(clock)
            );
            newVersion.setFileHash(fileHash);
            newVersion.setParentNote(rootNote);
            newVersion.setVersionNumber(nextVersionNumber);
            newVersion.setChangeSummary(normalizedSummary);
            newVersion.setLatest(true);

            Note savedVersion = noteRepository.save(newVersion);
            notificationPublisher.notifyNoteVersionAdded(savedVersion, currentUser);
            auditPublisher.publish(
                    AuditAction.NOTE_VERSION_UPLOADED,
                    currentUser,
                    "NOTE",
                    savedVersion.getId(),
                    "Note version uploaded directly",
                    "versionNumber=" + nextVersionNumber
            );

            return toNoteResponse(savedVersion);
        } catch (RuntimeException exception) {
            if (isNewFile && storedFile != null) {
                noteFileStorage.deleteIfExists(storedFile);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<NoteVersionResponse> listNoteVersions(Long noteId) {
        User currentUser = getCurrentUser();
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        Note rootNote = (note.getParentNote() != null) ? note.getParentNote() : note;
        checkNoteAccess(rootNote, currentUser);

        List<Note> versions = noteRepository.findAllVersions(rootNote.getId());
        // Root note might not be included in findAllVersions if it's only finding children, but our query:
        // "SELECT n FROM Note n WHERE n.id = :rootId OR n.parentNote.id = :rootId" covers both root and children.
        // Let's map all of them.
        return versions.stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Transactional
    public NoteEditProposalResponse createProposal(Long noteId, MultipartFile file, String changeSummary) {
        User currentUser = getCurrentUser();
        requireVerifiedEmail(currentUser);

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        Note rootNote = (note.getParentNote() != null) ? note.getParentNote() : note;
        checkNoteAccess(rootNote, currentUser);

        String normalizedSummary = requireText(changeSummary, "Change summary is required");
        if (normalizedSummary.length() > 1000) {
            throw new InvalidFileException("Change summary must be at most 1000 characters");
        }

        ValidatedFile validatedFile = fileValidationService.validate(file);
        String fileHash = calculateFileHash(validatedFile);

        java.util.Optional<Note> existingNote = noteRepository.findFirstByFileHash(fileHash);
        StoredFile storedFile = null;
        boolean isNewFile = false;

        if (existingNote.isPresent()) {
            Note existing = existingNote.get();
            storedFile = new StoredFile(
                    validatedFile.originalFileName(),
                    existing.getStoredFileName(),
                    existing.getContentType(),
                    existing.getFileSize(),
                    existing.getStoredFileName(),
                    existing.getStoragePath()
            );
        } else {
            storedFile = noteFileStorage.store(file);
            isNewFile = true;
        }

        try {
            NoteEditProposal proposal = new NoteEditProposal(
                    rootNote,
                    currentUser,
                    normalizedSummary,
                    storedFile.originalFileName(),
                    storedFile.storedFileName(),
                    storedFile.contentType(),
                    storedFile.fileSize(),
                    storedFile.storageLocation(),
                    Instant.now(clock)
            );
            proposal.setFileHash(fileHash);

            NoteEditProposal savedProposal = noteEditProposalRepository.save(proposal);
            notificationPublisher.notifyNoteEditProposalCreated(rootNote, currentUser, normalizedSummary);
            auditPublisher.publish(
                    AuditAction.NOTE_EDIT_PROPOSAL_CREATED,
                    currentUser,
                    "NOTE",
                    rootNote.getId(),
                    "Note edit proposal created",
                    "proposalId=" + savedProposal.getId()
            );

            return toProposalResponse(savedProposal);
        } catch (RuntimeException exception) {
            if (isNewFile && storedFile != null) {
                noteFileStorage.deleteIfExists(storedFile);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<NoteEditProposalResponse> listProposals(Long noteId) {
        User currentUser = getCurrentUser();
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        Note rootNote = (note.getParentNote() != null) ? note.getParentNote() : note;

        List<NoteEditProposal> proposals = noteEditProposalRepository.findByNoteIdOrderByCreatedAtDesc(rootNote.getId());

        if (rootNote.getUploadedBy().getId().equals(currentUser.getId())) {
            // Original author sees all
            return proposals.stream().map(this::toProposalResponse).toList();
        } else {
            // Classmates see only their own
            return proposals.stream()
                    .filter(p -> p.getProposer().getId().equals(currentUser.getId()))
                    .map(this::toProposalResponse)
                    .toList();
        }
    }

    @Transactional
    public NoteResponse approveProposal(Long noteId, Long proposalId) {
        User currentUser = getCurrentUser();
        requireVerifiedEmail(currentUser);

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        Note rootNote = (note.getParentNote() != null) ? note.getParentNote() : note;

        if (!rootNote.getUploadedBy().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the original author can approve proposals");
        }

        NoteEditProposal proposal = noteEditProposalRepository.findById(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new InvalidNoteInteractionException("Proposal is not pending");
        }

        proposal.approve(currentUser, Instant.now(clock));
        noteEditProposalRepository.save(proposal);

        List<Note> versions = noteRepository.findAllVersions(rootNote.getId());
        int nextVersionNumber = 2;
        for (Note v : versions) {
            if (v.getVersionNumber() >= nextVersionNumber) {
                nextVersionNumber = v.getVersionNumber() + 1;
            }
            if (v.isLatest()) {
                v.setLatest(false);
                noteRepository.save(v);
            }
        }

        Note newVersion = new Note(
                rootNote.getSubjectClass(),
                rootNote.getInstitution(),
                rootNote.getDegreeProgram(),
                rootNote.getSemester(),
                rootNote.getYear(),
                proposal.getOriginalFileName(),
                proposal.getStoredFileName(),
                proposal.getContentType(),
                proposal.getFileSize(),
                proposal.getStoragePath(),
                proposal.getProposer(),
                Instant.now(clock)
        );
        newVersion.setFileHash(proposal.getFileHash());
        newVersion.setParentNote(rootNote);
        newVersion.setVersionNumber(nextVersionNumber);
        newVersion.setChangeSummary(proposal.getChangeSummary());
        newVersion.setLatest(true);

        Note savedVersion = noteRepository.save(newVersion);

        notificationPublisher.notifyNoteEditProposalApproved(rootNote, currentUser, proposal.getProposer());
        notificationPublisher.notifyNoteVersionAdded(savedVersion, proposal.getProposer());

        auditPublisher.publish(
                AuditAction.NOTE_EDIT_PROPOSAL_APPROVED,
                currentUser,
                "NOTE",
                rootNote.getId(),
                "Note edit proposal approved",
                "proposalId=" + proposalId + ",versionNumber=" + nextVersionNumber
        );

        return toNoteResponse(savedVersion);
    }

    @Transactional
    public NoteEditProposalResponse rejectProposal(Long noteId, Long proposalId, RejectProposalRequest request) {
        User currentUser = getCurrentUser();
        requireVerifiedEmail(currentUser);

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        Note rootNote = (note.getParentNote() != null) ? note.getParentNote() : note;

        if (!rootNote.getUploadedBy().getId().equals(currentUser.getId())) {
            throw new SecurityException("Only the original author can reject proposals");
        }

        NoteEditProposal proposal = noteEditProposalRepository.findById(proposalId)
                .orElseThrow(() -> new ProposalNotFoundException(proposalId));

        if (proposal.getStatus() != ProposalStatus.PENDING) {
            throw new InvalidNoteInteractionException("Proposal is not pending");
        }

        proposal.reject(currentUser, request.rejectionReason(), Instant.now(clock));
        NoteEditProposal savedProposal = noteEditProposalRepository.save(proposal);

        notificationPublisher.notifyNoteEditProposalRejected(rootNote, currentUser, proposal.getProposer(), request.rejectionReason());

        auditPublisher.publish(
                AuditAction.NOTE_EDIT_PROPOSAL_REJECTED,
                currentUser,
                "NOTE",
                rootNote.getId(),
                "Note edit proposal rejected",
                "proposalId=" + proposalId
        );

        return toProposalResponse(savedProposal);
    }

    private String calculateFileHash(ValidatedFile validatedFile) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(validatedFile.bytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new com.sharenote.storage.FileStorageException("SHA-256 algorithm not found", e);
        }
    }

    private NoteVersionResponse toVersionResponse(Note version) {
        User uploadedBy = version.getUploadedBy();
        return new NoteVersionResponse(
                version.getId(),
                version.getParentNote() == null ? null : version.getParentNote().getId(),
                version.getVersionNumber(),
                version.getChangeSummary(),
                version.getOriginalFileName(),
                version.getContentType(),
                version.getFileSize(),
                uploadedBy.getId(),
                formatName(uploadedBy),
                version.getCreatedAt()
        );
    }

    private NoteEditProposalResponse toProposalResponse(NoteEditProposal proposal) {
        User proposer = proposal.getProposer();
        User reviewer = proposal.getReviewedBy();
        return new NoteEditProposalResponse(
                proposal.getId(),
                proposal.getNote().getId(),
                proposer.getId(),
                formatName(proposer),
                proposal.getChangeSummary(),
                proposal.getOriginalFileName(),
                proposal.getContentType(),
                proposal.getFileSize(),
                proposal.getStatus().name(),
                proposal.getRejectionReason(),
                proposal.getCreatedAt(),
                proposal.getReviewedAt(),
                reviewer == null ? null : reviewer.getId(),
                reviewer == null ? null : formatName(reviewer)
        );
    }

    public record DownloadDetails(
            String pathOrUrl,
            boolean isPresignedUrl,
            String originalFileName,
            String contentType
    ) {}
}
