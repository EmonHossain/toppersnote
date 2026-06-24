package com.sharenote.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sharenote.persistence.CriteriaRepositorySupport;
import com.sharenote.user.entities.User;
import com.sharenote.user.entities.User_;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;

@Repository
public class UserRepository extends CriteriaRepositorySupport<User> {

    public UserRepository() {
        super(User.class);
    }

    // save
    @Transactional
    public User save(User user) {
        return saveEntity(user);
    }

    // findById
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return findEntityById(id);
    }

    // findAll
    @Transactional(readOnly = true)
    public List<User> findAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        query.orderBy(cb.asc(user.get("id")));

        return entityManager.createQuery(query).getResultList();
    }

    // findAll
    @Transactional(readOnly = true)
    public List<User> findAllAsOrdered(SingularAttribute<?,?> property, Direction direction) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        query.orderBy(cb.asc(user.get("id")));

        return entityManager.createQuery(query).getResultList();
    }

    // findAllIds
    @Transactional(readOnly = true)
    public List<User> findAllById(Iterable<Long> ids) {
        List<Long> userIds = new ArrayList<>();
        ids.forEach(userIds::add);
        if (userIds.isEmpty()) {
            return List.of();
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        query.where(user.get("id").in(userIds));

        return entityManager.createQuery(query).getResultList();
    }

    // findEmail
    @Transactional(readOnly = true)
    public Optional<User> findByEmailIgnoreCase(String email) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        query.where(cb.equal(cb.lower(user.get("email")), lower(email)));

        return entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }

    // existsEmail
    @Transactional(readOnly = true)
    public boolean existsByEmailIgnoreCase(String email) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<User> user = query.from(User.class);

        query.select(cb.count(user));
        query.where(cb.equal(cb.lower(user.get("email")), lower(email)));

        return entityManager.createQuery(query).getSingleResult() > 0;
    }

    // findAcademicUsers
    @Transactional(readOnly = true)
    public List<User> findByInstitutionIgnoreCaseAndDegreeProgramIgnoreCaseAndCurrentYearIgnoreCaseAndCurrentSemesterIgnoreCase(
            String institution,
            String degreeProgram,
            String currentYear,
            String currentSemester) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);

        query.where(
                cb.equal(cb.lower(user.get("institution")), lower(institution)),
                cb.equal(cb.lower(user.get("degreeProgram")), lower(degreeProgram)),
                cb.equal(cb.lower(user.get("currentYear")), lower(currentYear)),
                cb.equal(cb.lower(user.get("currentSemester")), lower(currentSemester)));
        query.orderBy(cb.asc(user.get("id")));

        return entityManager.createQuery(query).getResultList();
    }

    private String lower(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    public Optional<User> findByUsername(String username) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> root = query.from(User.class);
        query.where(cb.equal(root.get(User_.username), username));
        return entityManager.createQuery(query).setMaxResults(1).getResultList().stream().findFirst();
    }
}
