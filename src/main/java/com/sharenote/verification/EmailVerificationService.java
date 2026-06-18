package com.sharenote.verification;

import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import com.sharenote.verification.messaging.EmailVerificationDispatchEvent;
import com.sharenote.verification.messaging.EmailVerificationMessage;
import com.sharenote.verification.dto.EmailVerificationResponse;
import com.sharenote.verification.dto.ResendEmailVerificationRequest;
import com.sharenote.verification.dto.VerifyEmailRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String GENERIC_RESEND_MESSAGE =
            "If the account exists and is eligible, a verification email has been queued";

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailVerificationProperties properties;
    private final AuditPublisher auditPublisher;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock = Clock.systemUTC();

    // Creates a token and raises a delivery event that is processed only after commit.
    @Transactional
    public void sendVerification(User user) {
        String rawToken = generateToken();
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(Duration.ofMinutes(properties.getTokenExpirationMinutes()));
        EmailVerificationToken token = new EmailVerificationToken(user, hashToken(rawToken), now, expiresAt);

        tokenRepository.save(token);
        eventPublisher.publishEvent(new EmailVerificationDispatchEvent(new EmailVerificationMessage(
                UUID.randomUUID().toString(),
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                buildVerificationUrl(rawToken),
                expiresAt
        )));
        auditPublisher.publish(
                AuditAction.EMAIL_VERIFICATION_SENT,
                user,
                "USER",
                user.getId(),
                "Email verification queued"
        );
    }

    // Marks a user's email verified when the supplied token is valid and unused.
    @Transactional
    public EmailVerificationResponse verify(VerifyEmailRequest request) {
        String tokenHash = hashToken(request.token().trim());
        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    auditPublisher.publishAnonymous(
                            AuditAction.EMAIL_VERIFICATION_FAILED,
                            null,
                            "USER",
                            null,
                            "Email verification failed"
                    );
                    return new InvalidEmailVerificationTokenException();
                });

        Instant now = Instant.now(clock);
        User user = token.getUser();
        if (token.isUsed() || token.isExpired(now)) {
            auditPublisher.publish(
                    AuditAction.EMAIL_VERIFICATION_FAILED,
                    user,
                    "USER",
                    user.getId(),
                    "Email verification token rejected"
            );
            throw new InvalidEmailVerificationTokenException();
        }

        token.markUsed(now);
        if (!user.isEmailVerified()) {
            user.markEmailVerified(now);
            userRepository.save(user);
        }
        tokenRepository.save(token);
        auditPublisher.publish(AuditAction.EMAIL_VERIFIED, user, "USER", user.getId(), "Email verified");

        return new EmailVerificationResponse(true, "Email verified successfully");
    }

    // Queues a fresh verification email while preserving a generic anti-enumeration response.
    @Transactional
    public EmailVerificationResponse resend(ResendEmailVerificationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        validateInstitutionEmail(normalizedEmail);

        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .filter(user -> !user.isEmailVerified())
                .ifPresent(this::sendVerification);

        return new EmailVerificationResponse(false, GENERIC_RESEND_MESSAGE);
    }

    // Ensures an email belongs to an approved school domain.
    public void validateInstitutionEmail(String email) {
        String domain = extractDomain(email);
        if (domain.endsWith(".edu") || allowedDomains().contains(domain)) {
            return;
        }
        throw new InvalidInstitutionEmailException();
    }

    // Normalizes configured institution domains for case-insensitive matching.
    private Set<String> allowedDomains() {
        return properties.getAllowedDomains().stream()
                .filter(StringUtils::hasText)
                .map(domain -> domain.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    // Reads and validates the domain portion of an email address.
    private String extractDomain(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex < 0 || atIndex == email.length() - 1) {
            throw new InvalidInstitutionEmailException();
        }
        return email.substring(atIndex + 1).toLowerCase(Locale.ROOT);
    }

    // Creates a cryptographically random URL-safe verification token.
    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Hashes a raw verification token so the database never stores the usable secret.
    private String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    // Builds the public callback URL from the configured API version prefix.
    private String buildVerificationUrl(String rawToken) {
        String baseUrl = properties.getBaseUrl();
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String apiBasePath = properties.getApiBasePath();
        String normalizedApiBasePath = apiBasePath.startsWith("/") ? apiBasePath : "/" + apiBasePath;
        return normalizedBaseUrl + normalizedApiBasePath + "/auth/verify-email?token="
                + URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
