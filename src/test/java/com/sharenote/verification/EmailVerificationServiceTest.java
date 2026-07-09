package com.sharenote.verification;

import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditRecorder;
import com.sharenote.role.Role;
import com.sharenote.user.UserRepository;
import com.sharenote.user.entities.User;
import com.sharenote.verification.messaging.EmailVerificationDispatchEvent;
import com.sharenote.verification.dto.ResendEmailVerificationRequest;
import com.sharenote.verification.dto.VerifyEmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AuditRecorder auditPublisher;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        EmailVerificationProperties properties = new EmailVerificationProperties();
        properties.setBaseUrl("https://sharenote.test");
        properties.setTokenExpirationMinutes(60);
        properties.setAllowedDomains(Set.of("college.org"));
        emailVerificationService = new EmailVerificationService(
                tokenRepository,
                userRepository,
                properties,
                auditPublisher,
                eventPublisher
        );
    }

    // Verifies that only a token hash is stored and delivery is represented by an event.
    @Test
    void sendVerificationStoresHashedTokenAndPublishesLinkEvent() {
        User user = user("amina@university.edu");

        emailVerificationService.sendVerification(user);

        ArgumentCaptor<EmailVerificationToken> tokenCaptor = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        EmailVerificationToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getExpiresAt()).isAfter(savedToken.getCreatedAt());
        ArgumentCaptor<EmailVerificationDispatchEvent> eventCaptor =
                ArgumentCaptor.forClass(EmailVerificationDispatchEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().message().verificationUrl())
                .contains("/api/v1/auth/verify-email?token=");
        assertThat(eventCaptor.getValue().message().recipientEmail()).isEqualTo(user.getEmail());
    }

    // Verifies that a valid token updates both the user and token records.
    @Test
    void verifyMarksUserEmailVerified() {
        User user = user("amina@university.edu");
        EmailVerificationToken token = new EmailVerificationToken(
                user,
                "hash",
                Instant.now().minusSeconds(60),
                Instant.now().plusSeconds(3600)
        );

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        var response = emailVerificationService.verify(new VerifyEmailRequest("raw-token"));

        assertThat(response.emailVerified()).isTrue();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNotNull();
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
        verify(auditPublisher).publish(AuditAction.EMAIL_VERIFIED, user, "USER", user.getId(), "Email verified");
    }

    // Verifies that expired verification tokens cannot activate an account.
    @Test
    void verifyRejectsExpiredToken() {
        User user = user("amina@university.edu");
        EmailVerificationToken token = new EmailVerificationToken(
                user,
                "hash",
                Instant.now().minusSeconds(3600),
                Instant.now().minusSeconds(60)
        );

        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> emailVerificationService.verify(new VerifyEmailRequest("raw-token")))
                .isInstanceOf(InvalidEmailVerificationTokenException.class)
                .hasMessage("Email verification token is invalid or expired");

        assertThat(user.isEmailVerified()).isFalse();
        verify(userRepository, never()).save(any());
    }

    // Verifies resend remains anti-enumeration-safe and queues a fresh event.
    @Test
    void resendUsesGenericResponseAndQueuesForUnverifiedUser() {
        User user = user("amina@university.edu");
        when(userRepository.findByEmailIgnoreCase("amina@university.edu")).thenReturn(Optional.of(user));

        var response = emailVerificationService.resend(new ResendEmailVerificationRequest("AMINA@university.edu"));

        assertThat(response.emailVerified()).isFalse();
        assertThat(response.message()).contains("If the account exists");
        verify(eventPublisher).publishEvent(any(EmailVerificationDispatchEvent.class));
    }

    // Verifies explicitly configured institution domains are accepted.
    @Test
    void validateInstitutionEmailAllowsConfiguredInstitutionDomain() {
        emailVerificationService.validateInstitutionEmail("student@college.org");
    }

    // Verifies consumer email domains are rejected when they are not configured.
    @Test
    void validateInstitutionEmailRejectsConsumerDomain() {
        assertThatThrownBy(() -> emailVerificationService.validateInstitutionEmail("student@example.com"))
                .isInstanceOf(InvalidInstitutionEmailException.class)
                .hasMessage("Email must use a .edu or approved institution domain");
    }

    // Creates an unverified user fixture for token service tests.
    private User user(String email) {
        return new User(
                "Amina",
                null,
                "Rahman",
                email,
                "hashed-password",
                "university",
                "Computer Science",
                "3",
                "2026",
                "3",
                "+491234567890",
                "Germany",
                Set.of(Role.USER)
        );
    }
}
