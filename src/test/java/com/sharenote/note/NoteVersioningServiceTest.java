package com.sharenote.note;

import com.sharenote.audit.AuditPublisher;
import com.sharenote.note.dto.NoteEditProposalResponse;
import com.sharenote.note.dto.NoteResponse;
import com.sharenote.note.dto.NoteVersionResponse;
import com.sharenote.note.dto.RejectProposalRequest;
import com.sharenote.notification.NotificationPublisher;
import com.sharenote.storage.FileValidationService;
import com.sharenote.storage.NoteFileStorage;
import com.sharenote.storage.StoredFile;
import com.sharenote.storage.ValidatedFile;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import com.sharenote.verification.EmailNotVerifiedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteVersioningServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NoteFileStorage noteFileStorage;

    @Mock
    private NoteCommentRepository noteCommentRepository;

    @Mock
    private NoteUpvoteRepository noteUpvoteRepository;

    @Mock
    private NoteTakeALookSuggestionRepository takeALookSuggestionRepository;

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private AuditPublisher auditPublisher;

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private NoteEditProposalRepository noteEditProposalRepository;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(
                noteRepository,
                userRepository,
                noteFileStorage,
                noteCommentRepository,
                noteUpvoteRepository,
                takeALookSuggestionRepository,
                notificationPublisher,
                null, // AcademicClassRegistrar not needed
                auditPublisher,
                fileValidationService,
                noteEditProposalRepository
        );
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "author@mit.edu",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadVersionDirectlySucceedsForAuthor() {
        User author = user(1L, "author@mit.edu", "MIT", "CS", true);
        Note rootNote = note(10L, "Mathematics", "MIT", "CS", author);

        MockMultipartFile file = new MockMultipartFile("file", "v2.pdf", "application/pdf", new byte[]{1, 2, 3});
        ValidatedFile validatedFile = new ValidatedFile("v2.pdf", "pdf", "application/pdf", 3L, new byte[]{1, 2, 3});
        StoredFile storedFile = new StoredFile("v2.pdf", "stored-v2.pdf", "application/pdf", 3L, "key", "uploads/v2.pdf");

        when(userRepository.findByEmailIgnoreCase("author@mit.edu")).thenReturn(Optional.of(author));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(rootNote));
        when(fileValidationService.validate(file)).thenReturn(validatedFile);
        when(noteRepository.findFirstByFileHash(anyString())).thenReturn(Optional.empty());
        when(noteFileStorage.store(file)).thenReturn(storedFile);
        when(noteRepository.findAllVersions(10L)).thenReturn(List.of(rootNote));
        verify(noteRepository).saveNewNote(any(Note.class)); // For setting root latest=false

        NoteResponse response = noteService.uploadVersionDirectly(10L, file, "Fixed page 3 typo");

        assertThat(response.id()).isEqualTo(11L);
        assertThat(rootNote.isLatest()).isFalse();
        verify(noteRepository, times(2)).saveNewNote(any(Note.class)); // 1 for setting root latest=false, 1 for saving new version
    }

    @Test
    void uploadVersionDirectlyRejectsNonAuthor() {
        User nonAuthor = user(2L, "other@mit.edu", "MIT", "CS", true);
        User author = user(1L, "author@mit.edu", "MIT", "CS", true);
        Note rootNote = note(10L, "Mathematics", "MIT", "CS", author);

        // Authenticate as other user
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "other@mit.edu", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));

        MockMultipartFile file = new MockMultipartFile("file", "v2.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(userRepository.findByEmailIgnoreCase("other@mit.edu")).thenReturn(Optional.of(nonAuthor));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(rootNote));

        assertThatThrownBy(() -> noteService.uploadVersionDirectly(10L, file, "Update"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Only the original author");
    }

    @Test
    void listNoteVersionsReturnsChain() {
        User author = user(1L, "author@mit.edu", "MIT", "CS", true);
        Note rootNote = note(10L, "Mathematics", "MIT", "CS", author);
        Note v2 = note(11L, "Mathematics", "MIT", "CS", author);
        v2.setParentNote(rootNote);
        v2.setVersionNumber(2);

        when(userRepository.findByEmailIgnoreCase("author@mit.edu")).thenReturn(Optional.of(author));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(rootNote));
        when(noteRepository.findAllVersions(10L)).thenReturn(List.of(rootNote, v2));

        List<NoteVersionResponse> list = noteService.listNoteVersions(10L);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).versionNumber()).isEqualTo(1);
        assertThat(list.get(1).versionNumber()).isEqualTo(2);
    }

    @Test
    void createProposalSucceedsForClassmate() {
        User classmate = user(2L, "classmate@mit.edu", "MIT", "CS", true);
        User author = user(1L, "author@mit.edu", "MIT", "CS", true);
        Note rootNote = note(10L, "Mathematics", "MIT", "CS", author);

        // Authenticate classmate
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "classmate@mit.edu", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));

        MockMultipartFile file = new MockMultipartFile("file", "proposal.pdf", "application/pdf", new byte[]{1, 2, 3});
        ValidatedFile validatedFile = new ValidatedFile("proposal.pdf", "pdf", "application/pdf", 3L, new byte[]{1, 2, 3});
        StoredFile storedFile = new StoredFile("proposal.pdf", "stored-prop.pdf", "application/pdf", 3L, "key", "uploads/prop.pdf");

        when(userRepository.findByEmailIgnoreCase("classmate@mit.edu")).thenReturn(Optional.of(classmate));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(rootNote));
        when(fileValidationService.validate(file)).thenReturn(validatedFile);
        when(noteRepository.findFirstByFileHash(anyString())).thenReturn(Optional.empty());
        when(noteFileStorage.store(file)).thenReturn(storedFile);
        when(noteEditProposalRepository.save(any(NoteEditProposal.class))).thenAnswer(inv -> {
            NoteEditProposal p = inv.getArgument(0);
            ReflectionTestUtils.setField(p, "id", 50L);
            return p;
        });

        NoteEditProposalResponse response = noteService.createProposal(10L, file, "Corrected theorem 2.4");
        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.proposerId()).isEqualTo(2L);
    }

    @Test
    void createProposalRejectsOriginalAuthor() {
        User author = user(1L, "author@mit.edu", "MIT", "CS", true);
        Note rootNote = note(10L, "Mathematics", "MIT", "CS", author);
        MockMultipartFile file = new MockMultipartFile("file", "proposal.pdf", "application/pdf", new byte[]{1, 2, 3});

        when(userRepository.findByEmailIgnoreCase("author@mit.edu")).thenReturn(Optional.of(author));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(rootNote));

        assertThatThrownBy(() -> noteService.createProposal(10L, file, "Author should use direct version upload"))
                .isInstanceOf(InvalidNoteInteractionException.class)
                .hasMessageContaining("directly");

        verify(fileValidationService, never()).validate(any());
    }

    @Test
    void approveProposalSucceedsForAuthor() {
        User author = user(1L, "author@mit.edu", "MIT", "CS", true);
        User proposer = user(2L, "classmate@mit.edu", "MIT", "CS", true);
        Note rootNote = note(10L, "Mathematics", "MIT", "CS", author);

        NoteEditProposal proposal = new NoteEditProposal(
                rootNote, proposer, "Typo fix", "p.pdf", "sp.pdf", "application/pdf", 3L, "path", Instant.now()
        );
        ReflectionTestUtils.setField(proposal, "id", 50L);

        when(userRepository.findByEmailIgnoreCase("author@mit.edu")).thenReturn(Optional.of(author));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(rootNote));
        when(noteEditProposalRepository.findById(50L)).thenReturn(Optional.of(proposal));
        when(noteRepository.findAllVersions(10L)).thenReturn(List.of(rootNote));
        verify(noteRepository).saveNewNote(any(Note.class));

        NoteResponse response = noteService.approveProposal(10L, 50L);

        assertThat(response.id()).isEqualTo(12L);
        assertThat(proposal.getStatus()).isEqualTo(ProposalStatus.APPROVED);
        assertThat(rootNote.isLatest()).isFalse();
    }

    @Test
    void approveProposalRejectsProposalFromDifferentNote() {
        User author = user(1L, "author@mit.edu", "MIT", "CS", true);
        User proposer = user(2L, "classmate@mit.edu", "MIT", "CS", true);
        Note rootNote = note(10L, "Mathematics", "MIT", "CS", author);
        Note otherNote = note(20L, "Physics", "MIT", "CS", author);

        NoteEditProposal proposal = new NoteEditProposal(
                otherNote, proposer, "Wrong note", "p.pdf", "sp.pdf", "application/pdf", 3L, "path", Instant.now()
        );
        ReflectionTestUtils.setField(proposal, "id", 50L);

        when(userRepository.findByEmailIgnoreCase("author@mit.edu")).thenReturn(Optional.of(author));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(rootNote));
        when(noteEditProposalRepository.findById(50L)).thenReturn(Optional.of(proposal));

        assertThatThrownBy(() -> noteService.approveProposal(10L, 50L))
                .isInstanceOf(ProposalNotFoundException.class);

        verify(noteRepository, never()).findAllVersions(any());
    }

    @Test
    void rejectProposalSucceedsForAuthor() {
        User author = user(1L, "author@mit.edu", "MIT", "CS", true);
        User proposer = user(2L, "classmate@mit.edu", "MIT", "CS", true);
        Note rootNote = note(10L, "Mathematics", "MIT", "CS", author);

        NoteEditProposal proposal = new NoteEditProposal(
                rootNote, proposer, "Typo fix", "p.pdf", "sp.pdf", "application/pdf", 3L, "path", Instant.now()
        );
        ReflectionTestUtils.setField(proposal, "id", 50L);

        when(userRepository.findByEmailIgnoreCase("author@mit.edu")).thenReturn(Optional.of(author));
        when(noteRepository.findById(10L)).thenReturn(Optional.of(rootNote));
        when(noteEditProposalRepository.findById(50L)).thenReturn(Optional.of(proposal));
        when(noteEditProposalRepository.save(any(NoteEditProposal.class))).thenAnswer(inv -> inv.getArgument(0));

        NoteEditProposalResponse response = noteService.rejectProposal(10L, 50L, new RejectProposalRequest("Not a typo"));

        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.rejectionReason()).isEqualTo("Not a typo");
    }

    private Note note(Long id, String subject, String institution, String degreeProgram, User author) {
        Note note = new Note(
                subject,
                institution,
                degreeProgram,
                "1",
                "2026",
                "original.pdf",
                "stored.pdf",
                "application/pdf",
                100L,
                "uploads/stored.pdf",
                author,
                Instant.now()
        );
        ReflectionTestUtils.setField(note, "id", id);
        return note;
    }

    private User user(Long id, String email, String institution, String degreeProgram, boolean emailVerified) {
        User user = new User(
                "Test",
                null,
                "User",
                email,
                "password",
                institution,
                degreeProgram,
                "1",
                "2026",
                "1",
                "123456",
                "USA",
                Set.of(Role.USER)
        );
        ReflectionTestUtils.setField(user, "id", id);
        if (emailVerified) {
            user.markEmailVerified(Instant.now());
        }
        return user;
    }
}
