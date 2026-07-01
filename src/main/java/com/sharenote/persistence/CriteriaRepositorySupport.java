package com.sharenote.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Optional;

public abstract class CriteriaRepositorySupport<T> {

    @PersistenceContext
    protected EntityManager entityManager;
    private final Class<T> entityClass;

    protected CriteriaRepositorySupport(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected T save(T entity) {
        return entityManager.merge(entity);
    }

    protected List<T> saveAll(Iterable<T> entities) {
        java.util.ArrayList<T> savedEntities = new java.util.ArrayList<>();
        for (T entity : entities) {
            savedEntities.add(save(entity));
        }
        return savedEntities;
    }

    protected Optional<T> findById(Long id) {
        return Optional.ofNullable(entityManager.find(entityClass, id));
    }

    protected void delete(T entity) {
        T managedEntity = entityManager.contains(entity) ? entity : entityManager.merge(entity);
        entityManager.remove(managedEntity);
    }

    protected void deleteAll(Iterable<T> entities) {
        for (T entity : entities) {
            delete(entity);
        }
    }

    protected List<T> findAll() {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(entityClass);
        Root<T> rootEntry = cq.from(entityClass);
        CriteriaQuery<T> all = cq.select(rootEntry);
        return entityManager.createQuery(all).getResultList();
    }
}
