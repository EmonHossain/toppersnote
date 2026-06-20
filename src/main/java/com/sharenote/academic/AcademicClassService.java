package com.sharenote.academic;

import com.sharenote.academic.dto.AcademicClassResponse;
import com.sharenote.note.CurrentUserNotFoundException;
import com.sharenote.note.Note;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class AcademicClassService implements AcademicClassRegistrar {

    private final AcademicClassRepository academicClassRepository;
    private final ClassRegistrationRepository classRegistrationRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public AcademicClassService(
            AcademicClassRepository academicClassRepository,
            ClassRegistrationRepository classRegistrationRepository,
            UserRepository userRepository
    ) {
        this.academicClassRepository = academicClassRepository;
        this.classRegistrationRepository = classRegistrationRepository;
        this.userRepository = userRepository;
        this.clock = Clock.systemUTC();
    }

    // Automatically registers users matching the academic context of the newly uploaded note.
    @Transactional
    @Override
    public AcademicClass registerMatchingUsers(Note note) {
        AcademicClass academicClass = academicClassRepository
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndYearIgnoreCaseAndSemesterIgnoreCaseAndSubjectClassIgnoreCase(
                        note.getInstitution(),
                        note.getDegreeProgram(),
                        note.getYear(),
                        note.getSemester(),
                        note.getSubjectClass()
                )
                .orElseGet(() -> academicClassRepository.save(new AcademicClass(
                        note.getInstitution(),
                        note.getDegreeProgram(),
                        note.getYear(),
                        note.getSemester(),
                        note.getSubjectClass()
                )));

        List<User> matchingUsers = userRepository
                .findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndCurrentYearIgnoreCaseAndCurrentSemesterIgnoreCase(
                        academicClass.getInstitution(),
                        academicClass.getDegreeProgram(),
                        academicClass.getYear(),
                        academicClass.getSemester()
                );

        matchingUsers.stream()
                .filter(user -> user.getId() != null)
                .filter(user -> !classRegistrationRepository.existsByAcademicClassIdAndUserId(
                        academicClass.getId(),
                        user.getId()
                ))
                .map(user -> new ClassRegistration(academicClass, user, Instant.now(clock)))
                .forEach(classRegistrationRepository::save);

        return academicClass;
    }

    /**
     * Retrieves active (non-archived) classes registered for the currently authenticated user.
     * Delegates to criteria-backed ClassRegistrationRepository queries.
     *
     * @return a list of active AcademicClassResponse
     */
    @Transactional(readOnly = true)
    public List<AcademicClassResponse> getMyClasses() {
        User currentUser = getCurrentUser();

        return classRegistrationRepository
                .findByUserIdOrderByAcademicClassDegreeProgramAscAcademicClassYearAscAcademicClassSemesterAscAcademicClassSubjectClassAsc(
                        currentUser.getId()
                )
                .stream()
                .filter(registration -> !registration.isArchived())
                .map(ClassRegistration::getAcademicClass)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Retrieves archived classes registered for the currently authenticated user.
     * Delegates to criteria-backed ClassRegistrationRepository queries.
     *
     * @return a list of archived AcademicClassResponse
     */
    @Transactional(readOnly = true)
    public List<AcademicClassResponse> getMyArchivedClasses() {
        User currentUser = getCurrentUser();

        return classRegistrationRepository
                .findByUserIdOrderByAcademicClassDegreeProgramAscAcademicClassYearAscAcademicClassSemesterAscAcademicClassSubjectClassAsc(
                        currentUser.getId()
                )
                .stream()
                .filter(ClassRegistration::isArchived)
                .map(ClassRegistration::getAcademicClass)
                .map(this::toResponse)
                .toList();
    }

    /**
     * Archives a registered class for the currently authenticated user by setting archived = true.
     * Uses a criteria-backed repository query to retrieve the registration first.
     *
     * @param classId the ID of the AcademicClass to archive
     */
    @Transactional
    public void archiveClass(Long classId) {
        User currentUser = getCurrentUser();

        ClassRegistration reg = classRegistrationRepository.findByAcademicClassIdAndUserId(classId, currentUser.getId())
                .orElseThrow(() -> new InvalidAcademicNavigationException("Class registration not found for the user"));
        reg.setArchived(true);
        classRegistrationRepository.save(reg);
    }


    @Transactional(readOnly = true)
    public List<String> getDegreePrograms() {
        User currentUser = getCurrentUser();
        return academicClassRepository.findDegreeProgramsByInstitution(currentUser.getInstitution());
    }

    @Transactional(readOnly = true)
    public List<String> getYears(String degreeProgram) {
        User currentUser = getCurrentUser();
        return academicClassRepository.findYearsByInstitutionAndDegreeProgram(
                currentUser.getInstitution(),
                requireText(degreeProgram, "Degree program is required")
        );
    }

    @Transactional(readOnly = true)
    public List<String> getSemesters(String degreeProgram, String year) {
        User currentUser = getCurrentUser();
        return academicClassRepository.findSemestersByInstitutionDegreeProgramAndYear(
                currentUser.getInstitution(),
                requireText(degreeProgram, "Degree program is required"),
                requireText(year, "Year is required")
        );
    }

    @Transactional(readOnly = true)
    public List<String> getSubjects(String degreeProgram, String year, String semester) {
        User currentUser = getCurrentUser();
        return academicClassRepository.findSubjectsByInstitutionDegreeProgramYearAndSemester(
                currentUser.getInstitution(),
                requireText(degreeProgram, "Degree program is required"),
                requireText(year, "Year is required"),
                requireText(semester, "Semester is required")
        );
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
            throw new InvalidAcademicNavigationException(message);
        }
        return value.trim();
    }

    private AcademicClassResponse toResponse(AcademicClass academicClass) {
        return new AcademicClassResponse(
                academicClass.getId(),
                academicClass.getInstitution(),
                academicClass.getDegreeProgram(),
                academicClass.getYear(),
                academicClass.getSemester(),
                academicClass.getSubjectClass()
        );
    }
}
