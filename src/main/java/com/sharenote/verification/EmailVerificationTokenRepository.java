package com.sharenote.verification;

import com.sharenote.persistence.CriteriaRepositorySupport;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class EmailVerificationTokenRepository extends CriteriaRepositorySupport<EmailVerificationToken> {

    public EmailVerificationTokenRepository() {
        super(EmailVerificationToken.class);
    }

    // save
    @Transactional
    public EmailVerificationToken save(EmailVerificationToken token) {
        return saveEntity(token);
    }

    // findHash
    @Transactional(readOnly = true)
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<EmailVerificationToken> query = cb.createQuery(EmailVerificationToken.class);
        Root<EmailVerificationToken> token = query.from(EmailVerificationToken.class);

        query.where(cb.equal(token.get("tokenHash"), tokenHash));

        return entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst();
    }
}
