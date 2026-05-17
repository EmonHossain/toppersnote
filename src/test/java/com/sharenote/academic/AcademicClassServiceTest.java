package com.sharenote.academic;

import com.sharenote.academic.dto.AcademicClassResponse;
import com.sharenote.note.Note;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicClassServiceTest {

    @Mock
    private AcademicClassRepository academicClassRepository;

    @Mock
    private ClassRegistrationRepository classRegistrationRepository;

    @Mock
    private UserRepository userRepository;

    private AcademicClassService academicClassService;

    @BeforeEach
    void setUp() {
        academicClassService = new AcademicClassService(
                academicClassRepository,
                classRegistrationRepository,
                userRepository
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
    void registerMatchingUsersCreatesClassAndRegistersOnlyUnregisteredMatches() {
        User uploader = user(1L, "Amina", "Rahman", "amina@example.com");
        User classmate = user(2L, "Karim", "Said", "karim@example.com");
        AcademicClass academicClass = academicClass(10L);
        Note note = note(100L, uploader);

        when(academicClassRepository
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndYearIgnoreCaseAndSemesterIgnoreCaseAndSubjectClassIgnoreCase(
                        "university",
                        "Computer Science",
                        "2026",
                        "3",
                        "Mathematics"
                ))
                .thenReturn(Optional.empty());
        when(academicClassRepository.save(any(AcademicClass.class))).thenReturn(academicClass);
        when(userRepository.findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndCurrentYearIgnoreCaseAndCurrentSemesterIgnoreCase(
                "university",
                "Computer Science",
                "2026",
                "3"
        )).thenReturn(List.of(uploader, classmate));
        when(classRegistrationRepository.existsByAcademicClassIdAndUserId(10L, 1L)).thenReturn(true);
        when(classRegistrationRepository.existsByAcademicClassIdAndUserId(10L, 2L)).thenReturn(false);

        AcademicClass response = academicClassService.registerMatchingUsers(note);

        assertThat(response).isSameAs(academicClass);
        ArgumentCaptor<ClassRegistration> registrationCaptor = ArgumentCaptor.forClass(ClassRegistration.class);
        verify(classRegistrationRepository).save(registrationCaptor.capture());
        assertThat(registrationCaptor.getValue().getUser()).isSameAs(classmate);
        assertThat(registrationCaptor.getValue().getAcademicClass()).isSameAs(academicClass);
    }

    @Test
    void getMyClassesReturnsRegisteredClassesForCurrentUser() {
        User currentUser = user(1L, "Amina", "Rahman", "amina@example.com");
        AcademicClass academicClass = academicClass(10L);
        ClassRegistration registration = new ClassRegistration(
                academicClass,
                currentUser,
                Instant.parse("2026-05-18T10:15:30Z")
        );

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(currentUser));
        when(classRegistrationRepository
                .findByUserIdOrderByAcademicClassDegreeProgramAscAcademicClassYearAscAcademicClassSemesterAscAcademicClassSubjectClassAsc(
                        1L
                ))
                .thenReturn(List.of(registration));

        List<AcademicClassResponse> responses = academicClassService.getMyClasses();

        assertThat(responses).hasSize(1);
        AcademicClassResponse response = responses.get(0);
        assertThat(response.degreeProgram()).isEqualTo("Computer Science");
        assertThat(response.year()).isEqualTo("2026");
        assertThat(response.semester()).isEqualTo("3");
        assertThat(response.subjectClass()).isEqualTo("Mathematics");
    }

    private AcademicClass academicClass(Long id) {
        AcademicClass academicClass = new AcademicClass(
                "university",
                "Computer Science",
                "2026",
                "3",
                "Mathematics"
        );
        ReflectionTestUtils.setField(academicClass, "id", id);
        return academicClass;
    }

    private Note note(Long id, User uploadedBy) {
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
                "uploads/notes/stored.pdf",
                uploadedBy,
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
                "Computer Science",
                "3",
                "2026",
                "3",
                "+491234567890",
                "Germany",
                Set.of(Role.USER)
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
