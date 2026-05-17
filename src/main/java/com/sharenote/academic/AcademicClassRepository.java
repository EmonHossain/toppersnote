package com.sharenote.academic;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AcademicClassRepository extends JpaRepository<AcademicClass, Long> {

    Optional<AcademicClass> findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndYearIgnoreCaseAndSemesterIgnoreCaseAndSubjectClassIgnoreCase(
            String institution,
            String degreeProgram,
            String year,
            String semester,
            String subjectClass
    );

    @Query("""
            select distinct academicClass.degreeProgram
            from AcademicClass academicClass
            where lower(academicClass.institution) = lower(:institution)
            order by academicClass.degreeProgram
            """)
    List<String> findDegreeProgramsByInstitution(@Param("institution") String institution);

    @Query("""
            select distinct academicClass.year
            from AcademicClass academicClass
            where lower(academicClass.institution) = lower(:institution)
              and lower(academicClass.degreeProgram) = lower(:degreeProgram)
            order by academicClass.year
            """)
    List<String> findYearsByInstitutionAndDegreeProgram(
            @Param("institution") String institution,
            @Param("degreeProgram") String degreeProgram
    );

    @Query("""
            select distinct academicClass.semester
            from AcademicClass academicClass
            where lower(academicClass.institution) = lower(:institution)
              and lower(academicClass.degreeProgram) = lower(:degreeProgram)
              and lower(academicClass.year) = lower(:year)
            order by academicClass.semester
            """)
    List<String> findSemestersByInstitutionDegreeProgramAndYear(
            @Param("institution") String institution,
            @Param("degreeProgram") String degreeProgram,
            @Param("year") String year
    );

    @Query("""
            select distinct academicClass.subjectClass
            from AcademicClass academicClass
            where lower(academicClass.institution) = lower(:institution)
              and lower(academicClass.degreeProgram) = lower(:degreeProgram)
              and lower(academicClass.year) = lower(:year)
              and lower(academicClass.semester) = lower(:semester)
            order by academicClass.subjectClass
            """)
    List<String> findSubjectsByInstitutionDegreeProgramYearAndSemester(
            @Param("institution") String institution,
            @Param("degreeProgram") String degreeProgram,
            @Param("year") String year,
            @Param("semester") String semester
    );
}
