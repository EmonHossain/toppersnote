package com.sharenote.academic;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassRegistrationRepository extends JpaRepository<ClassRegistration, Long> {

    boolean existsByAcademicClassIdAndUserId(Long academicClassId, Long userId);

    List<ClassRegistration> findByUserIdOrderByAcademicClassDegreeProgramAscAcademicClassYearAscAcademicClassSemesterAscAcademicClassSubjectClassAsc(
            Long userId
    );
}
