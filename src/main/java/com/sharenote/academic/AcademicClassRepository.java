package com.sharenote.academic;

import com.sharenote.persistence.CriteriaRepositorySupport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
public class AcademicClassRepository extends CriteriaRepositorySupport<AcademicClass> {

    public AcademicClassRepository() {
        super(AcademicClass.class);
    }

    // save
    @Transactional
    public AcademicClass save(AcademicClass academicClass) {
        return saveEntity(academicClass);
    }

    // findClassKey
    @Transactional(readOnly = true)
    public Optional<AcademicClass> findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndYearIgnoreCaseAndSemesterIgnoreCaseAndSubjectClassIgnoreCase(
            String institution,
            String degreeProgram,
            String year,
            String semester,
            String subjectClass
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<AcademicClass> query = cb.createQuery(AcademicClass.class);
        Root<AcademicClass> academicClass = query.from(AcademicClass.class);

        query.where(
                cb.equal(cb.lower(academicClass.get("institution")), lower(institution)),
                cb.equal(cb.lower(academicClass.get("degreeProgram")), lower(degreeProgram)),
                cb.equal(cb.lower(academicClass.get("year")), lower(year)),
                cb.equal(cb.lower(academicClass.get("semester")), lower(semester)),
                cb.equal(cb.lower(academicClass.get("subjectClass")), lower(subjectClass))
        );

        return entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    // findPrograms
    @Transactional(readOnly = true)
    public List<String> findDegreeProgramsByInstitution(String institution) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<AcademicClass> academicClass = query.from(AcademicClass.class);

        query.select(academicClass.get("degreeProgram")).distinct(true);
        query.where(cb.equal(cb.lower(academicClass.get("institution")), lower(institution)));
        query.orderBy(cb.asc(academicClass.get("degreeProgram")));

        return entityManager.createQuery(query).getResultList();
    }

    // findYears
    @Transactional(readOnly = true)
    public List<String> findYearsByInstitutionAndDegreeProgram(String institution, String degreeProgram) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<AcademicClass> academicClass = query.from(AcademicClass.class);

        query.select(academicClass.get("year")).distinct(true);
        query.where(
                cb.equal(cb.lower(academicClass.get("institution")), lower(institution)),
                cb.equal(cb.lower(academicClass.get("degreeProgram")), lower(degreeProgram))
        );
        query.orderBy(cb.asc(academicClass.get("year")));

        return entityManager.createQuery(query).getResultList();
    }

    // findSemesters
    @Transactional(readOnly = true)
    public List<String> findSemestersByInstitutionDegreeProgramAndYear(
            String institution,
            String degreeProgram,
            String year
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<AcademicClass> academicClass = query.from(AcademicClass.class);

        query.select(academicClass.get("semester")).distinct(true);
        query.where(
                cb.equal(cb.lower(academicClass.get("institution")), lower(institution)),
                cb.equal(cb.lower(academicClass.get("degreeProgram")), lower(degreeProgram)),
                cb.equal(cb.lower(academicClass.get("year")), lower(year))
        );
        query.orderBy(cb.asc(academicClass.get("semester")));

        return entityManager.createQuery(query).getResultList();
    }

    // findSubjects
    @Transactional(readOnly = true)
    public List<String> findSubjectsByInstitutionDegreeProgramYearAndSemester(
            String institution,
            String degreeProgram,
            String year,
            String semester
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<AcademicClass> academicClass = query.from(AcademicClass.class);

        query.select(academicClass.get("subjectClass")).distinct(true);
        query.where(
                cb.equal(cb.lower(academicClass.get("institution")), lower(institution)),
                cb.equal(cb.lower(academicClass.get("degreeProgram")), lower(degreeProgram)),
                cb.equal(cb.lower(academicClass.get("year")), lower(year)),
                cb.equal(cb.lower(academicClass.get("semester")), lower(semester))
        );
        query.orderBy(cb.asc(academicClass.get("subjectClass")));

        return entityManager.createQuery(query).getResultList();
    }

    private String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
