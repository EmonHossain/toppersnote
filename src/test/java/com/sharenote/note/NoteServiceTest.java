package com.sharenote.note;

import com.sharenote.academic.AcademicClassRegistrar;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.note.dto.NoteUploadResponse;
import com.sharenote.note.dto.NoteResponse;
import com.sharenote.notification.NotificationPublisher;
import com.sharenote.storage.FileValidationService;
import com.sharenote.storage.InvalidFileException;
import com.sharenote.storage.LocalNoteFileStorage;
import com.sharenote.storage.StorageProperties;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @TempDir
    private Path tempDirectory;

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteCommentRepository noteCommentRepository;

    @Mock
    private NoteUpvoteRepository noteUpvoteRepository;

    @Mock
    private NoteTakeALookSuggestionRepository takeALookSuggestionRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private AcademicClassRegistrar academicClassRegistrar;

    @Mock
    private AuditPublisher auditPublisher;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = localStorageProperties(10_485_760);
        FileValidationService fileValidationService = new FileValidationService(storageProperties);
        LocalNoteFileStorage localNoteFileStorage = new LocalNoteFileStorage(
                storageProperties,
                fileValidationService
        );
        noteService = new NoteService(
                noteRepository,
                userRepository,
                localNoteFileStorage,
                noteCommentRepository,
                noteUpvoteRepository,
                takeALookSuggestionRepository,
                notificationPublisher,
                academicClassRegistrar,
                auditPublisher,
                fileValidationService
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
    void uploadNoteStoresFileAndSavesMetadata() throws Exception {
        User user = user();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "calculus.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteUploadResponse response = noteService.uploadNote(file, " Mathematics ", " 3 ", " 2026 ");

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        Note savedNote = noteCaptor.getValue();

        assertThat(savedNote.getSubjectClass()).isEqualTo("Mathematics");
        assertThat(savedNote.getSemester()).isEqualTo("3");
        assertThat(savedNote.getYear()).isEqualTo("2026");
        assertThat(savedNote.getOriginalFileName()).isEqualTo("calculus.pdf");
        assertThat(savedNote.getUploadedBy()).isSameAs(user);
        assertThat(response.originalFileName()).isEqualTo("calculus.pdf");

        try (Stream<Path> files = Files.list(tempDirectory)) {
            assertThat(files.toList()).hasSize(1);
        }
    }

    @Test
    void uploadNoteRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> noteService.uploadNote(file, "Math", "3", "2026"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File is required");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void uploadNoteRejectsUnsupportedFileType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                "bad".getBytes()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> noteService.uploadNote(file, "Math", "3", "2026"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File type is not allowed");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void uploadNoteRejectsExecutableContent() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "MZ executable content".getBytes()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> noteService.uploadNote(file, "Math", "3", "2026"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File content is not allowed");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void uploadNoteRejectsContentThatDoesNotMatchDeclaredType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "not a real pdf".getBytes()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> noteService.uploadNote(file, "Math", "3", "2026"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File content does not match the declared type");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void uploadNoteRejectsFileOverSizeLimit() {
        StorageProperties storageProperties = localStorageProperties(4);
        FileValidationService fileValidationService = new FileValidationService(storageProperties);
        LocalNoteFileStorage tinyLimitStorage = new LocalNoteFileStorage(
                storageProperties,
                fileValidationService
        );
        NoteService tinyLimitNoteService = new NoteService(
                noteRepository,
                userRepository,
                tinyLimitStorage,
                noteCommentRepository,
                noteUpvoteRepository,
                takeALookSuggestionRepository,
                notificationPublisher,
                academicClassRegistrar,
                auditPublisher,
                fileValidationService
        );
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "calculus.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> tinyLimitNoteService.uploadNote(file, "Math", "3", "2026"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File exceeds maximum allowed size");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void uploadNoteRejectsPathTraversalFileName() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../calculus.pdf",
                "application/pdf",
                "%PDF-1.7".getBytes()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> noteService.uploadNote(file, "Math", "3", "2026"))
                .isInstanceOf(InvalidFileException.class)
                .hasMessage("File name is invalid");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void uploadNoteDeletesStoredFileWhenMetadataSaveFails() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "calculus.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user()));
        when(noteRepository.save(any(Note.class))).thenThrow(new RuntimeException("database failure"));

        assertThatThrownBy(() -> noteService.uploadNote(file, "Math", "3", "2026"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("database failure");

        try (Stream<Path> files = Files.list(tempDirectory)) {
            assertThat(files.toList()).isEmpty();
        }
    }

    @Test
    void getVisibleNotesReturnsRepositoryFilteredNotes() {
        User currentUser = user();
        User uploader = user("Karim", "Said", "karim@example.com");
        Note note = new Note(
                "Mathematics",
                "university",
                "Computer Science",
                "3",
                "2026",
                "calculus.pdf",
                "stored.pdf",
                "application/pdf",
                123,
                tempDirectory.resolve("stored.pdf").toString(),
                uploader,
                Instant.parse("2026-05-16T10:15:30Z")
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(currentUser));
        when(noteRepository.findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseOrderByCreatedAtDesc(
                "university",
                "Computer Science",
                "Mathematics",
                "3",
                "2026"
        )).thenReturn(List.of(note));

        List<NoteResponse> responses = noteService.getVisibleNotes(" Mathematics ", " 3 ", " 2026 ");

        assertThat(responses).hasSize(1);
        NoteResponse response = responses.get(0);
        assertThat(response.subjectClass()).isEqualTo("Mathematics");
        assertThat(response.semester()).isEqualTo("3");
        assertThat(response.year()).isEqualTo("2026");
        assertThat(response.originalFileName()).isEqualTo("calculus.pdf");
        assertThat(response.uploadedByName()).isEqualTo("Karim Said");
        verify(noteRepository).findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseOrderByCreatedAtDesc(
                "university",
                "Computer Science",
                "Mathematics",
                "3",
                "2026"
        );
    }

    @Test
    void getVisibleNotesRejectsMissingVisibilityFilter() {
        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> noteService.getVisibleNotes("Math", " ", "2026"))
                .isInstanceOf(InvalidNoteQueryException.class)
                .hasMessage("Semester is required");

        verify(noteRepository, never())
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseOrderByCreatedAtDesc(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void getVisibleNotesRequiresAuthenticatedUser() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> noteService.getVisibleNotes("Math", "3", "2026"))
                .isInstanceOf(CurrentUserNotFoundException.class)
                .hasMessage("Authenticated user could not be found");

        verify(noteRepository, never())
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndSubjectClassIgnoreCaseAndSemesterIgnoreCaseAndYearIgnoreCaseOrderByCreatedAtDesc(
                        any(),
                        any(),
                        any(),
                        any(),
                        any()
                );
    }

    private User user() {
        return user("Amina", "Rahman", "amina@example.com");
    }

    private User user(String firstName, String lastName, String email) {
        return new User(
                firstName,
                null,
                lastName,
                email,
                "hashed-password",
                "university",
                "Computer Science",
                "3",
                "2026",
                "3",
                "+491234567890",
                "Germany",
                Set.of(Role.USER)
        );
    }

    private StorageProperties localStorageProperties(long maxFileSizeBytes) {
        return new StorageProperties(
                tempDirectory.toString(),
                maxFileSizeBytes,
                "local",
                "",
                "notes",
                "us-east-1"
        );
    }

    @Test
    void uploadNoteDeDuplicatesFile() throws Exception {
        User user = user();
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "calculus.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "math-notes.pdf",
                "application/pdf",
                "%PDF-1.7\ncontent".getBytes()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));

        // Mock repository calls
        Note existingNote = new Note(
                "Mathematics", "university", "Computer Science", "3", "2026",
                "calculus.pdf", "stored-name.pdf", "application/pdf", file1.getSize(),
                tempDirectory.resolve("stored-name.pdf").toString(), user, Instant.now()
        );
        existingNote.setFileHash("abcde12345");

        // First upload (not de-duplicated, return saved note)
        when(noteRepository.findFirstByFileHash(any())).thenReturn(Optional.empty());
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteUploadResponse response1 = noteService.uploadNote(file1, "Mathematics", "3", "2026");
        assertThat(response1.originalFileName()).isEqualTo("calculus.pdf");

        // Second upload (de-duplicated)
        when(noteRepository.findFirstByFileHash(any())).thenReturn(Optional.of(existingNote));
        NoteUploadResponse response2 = noteService.uploadNote(file2, "Mathematics", "3", "2026");

        // Assert it points to same stored file name and size
        assertThat(response2.originalFileName()).isEqualTo("math-notes.pdf");

        // Verify only 1 file is physically written to disk
        try (Stream<Path> files = Files.list(tempDirectory)) {
            assertThat(files.toList()).hasSize(1);
        }
    }

    @Test
    void getDownloadDetailsSuccessForOwner() {
        User user = user();
        Note note = new Note(
                "Mathematics", "university", "Computer Science", "3", "2026",
                "calculus.pdf", "stored-name.pdf", "application/pdf", 100,
                tempDirectory.resolve("stored-name.pdf").toString(), user, Instant.now()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        NoteService.DownloadDetails details = noteService.getDownloadDetails(1L);
        assertThat(details.originalFileName()).isEqualTo("calculus.pdf");
        assertThat(details.isPresignedUrl()).isFalse();
        assertThat(details.pathOrUrl()).isEqualTo(note.getStoragePath());
    }

    @Test
    void getDownloadDetailsFailsForNonPeer() {
        User user = user("Amina", "Rahman", "amina@example.com");
        User otherUser = user("John", "Doe", "john@example.com");
        // Institution does not match user's institution ("university")
        Note note = new Note(
                "Mathematics", "different-uni", "Computer Science", "3", "2026",
                "calculus.pdf", "stored-name.pdf", "application/pdf", 100,
                tempDirectory.resolve("stored-name.pdf").toString(), otherUser, Instant.now()
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.getDownloadDetails(1L))
                .isInstanceOf(SecurityException.class)
                .hasMessage("You do not have permission to access this note");
    }
}
