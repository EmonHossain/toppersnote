package com.sharenote.academic;

import com.sharenote.persistence.CriteriaRepositorySupport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class ClassRegistrationRepository extends CriteriaRepositorySupport<ClassRegistration> {

    public ClassRegistrationRepository() {
        super(ClassRegistration.class);
    }

    // save
    @Transactional
    public ClassRegistration save(ClassRegistration classRegistration) {
        return saveEntity(classRegistration);
    }

    // existsClassUser
    @Transactional(readOnly = true)
    public boolean existsByAcademicClassIdAndUserId(Long academicClassId, Long userId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<ClassRegistration> registration = query.from(ClassRegistration.class);

        query.select(cb.count(registration));
        query.where(
                cb.equal(registration.get("academicClass").get("id"), academicClassId),
                cb.equal(registration.get("user").get("id"), userId)
        );

        return entityManager.createQuery(query).getSingleResult() > 0;
    }

    // findUserClasses
    @Transactional(readOnly = true)
    public List<ClassRegistration> findByUserIdOrderByAcademicClassDegreeProgramAscAcademicClassYearAscAcademicClassSemesterAscAcademicClassSubjectClassAsc(
            Long userId
    ) {
        return findByUserIdAndArchived(userId, null);
    }

    // findActiveClasses
    @Transactional(readOnly = true)
    public List<ClassRegistration> findActiveByUserId(Long userId) {
        return findByUserIdAndArchived(userId, false);
    }

    // findArchivedClasses
    @Transactional(readOnly = true)
    public List<ClassRegistration> findArchivedByUserId(Long userId) {
        return findByUserIdAndArchived(userId, true);
    }

    // findClassUser
    @Transactional(readOnly = true)
    public Optional<ClassRegistration> findByAcademicClassIdAndUserId(Long academicClassId, Long userId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClassRegistration> query = cb.createQuery(ClassRegistration.class);
        Root<ClassRegistration> registration = query.from(ClassRegistration.class);

        query.where(
                cb.equal(registration.get("academicClass").get("id"), academicClassId),
                cb.equal(registration.get("user").get("id"), userId)
        );

        return entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    private List<ClassRegistration> findByUserIdAndArchived(Long userId, Boolean archived) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<ClassRegistration> query = cb.createQuery(ClassRegistration.class);
        Root<ClassRegistration> registration = query.from(ClassRegistration.class);

        Predicate userPredicate = cb.equal(registration.get("user").get("id"), userId);
        if (archived == null) {
            query.where(userPredicate);
        } else {
            query.where(userPredicate, cb.equal(registration.get("archived"), archived));
        }
        query.orderBy(
                cb.asc(registration.get("academicClass").get("degreeProgram")),
                cb.asc(registration.get("academicClass").get("year")),
                cb.asc(registration.get("academicClass").get("semester")),
                cb.asc(registration.get("academicClass").get("subjectClass"))
        );

        return entityManager.createQuery(query).getResultList();
    }
}
