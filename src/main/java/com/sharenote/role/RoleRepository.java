package com.sharenote.role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sharenote.persistence.CriteriaRepositorySupport;
import com.sharenote.security.Permission;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class RoleRepository extends CriteriaRepositorySupport<Role> {

    protected RoleRepository() {
        super(Role.class);
    }

    Optional<Role> findByName(RoleLevel roleLevel) {
        // 1. Get CriteriaBuilder from EntityManager
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 2. Create a CriteriaQuery specifying the expected result type (Role)
        CriteriaQuery<Role> cq = cb.createQuery(Role.class);

        // 3. Define the FROM clause (Root entity)
        Root<Role> role = cq.from(Role.class);

        // 4. Construct the SELECT and WHERE clauses
        // (Assuming the entity property name is "roleLevel")
        cq.select(role).where(cb.equal(role.get("roleLevel"), roleLevel));

        // 5. Build the typed query
        return entityManager.createQuery(cq).setMaxResults(1).getResultList().stream().findFirst();
    }

    List<String> findPermissionNamesByRoleNames(List<String> roleNames) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. Define the return type of the query (String)
        CriteriaQuery<String> cq = cb.createQuery(String.class);

        // 2. FROM Role role
        Root<Role> role = cq.from(Role.class);

        // 3. JOIN role.permissions permission
        // (Assuming 'permissions' is the property name in your Role entity)
        Join<Role, Permission> permission = role.join("permissions");

        // 4. SELECT DISTINCT permission.requiredPermission
        cq.select(permission.get("requiredPermission")).distinct(true);

        // 5. WHERE role.name IN :roleNames
        Predicate inClause = role.get("roleLevel").in(roleNames);
        cq.where(inClause);

        // 6. Execute query
        return entityManager.createQuery(cq).getResultList();
    }
}