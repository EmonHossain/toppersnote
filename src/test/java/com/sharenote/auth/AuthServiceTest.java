package com.sharenote.auth;

import com.sharenote.audit.AuditPublisher;
import com.sharenote.auth.dto.AuthResponse;
import com.sharenote.auth.dto.LoginRequest;
import com.sharenote.user.Role;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditPublisher auditPublisher;

    private AuthService authService;
    private TestJwtService jwtService;
    private TestRefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        jwtService = new TestJwtService();
        refreshTokenService = new TestRefreshTokenService();
        authService = new AuthService(authenticationManager, jwtService, refreshTokenService, userRepository, auditPublisher);
    }

    @Test
    void loginValidatesCredentialsAndReturnsTokens() {
        LoginRequest request = new LoginRequest("AMINA@example.com", "StrongPass123");
        User user = user();

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("amina@example.com", "StrongPass123")
        );
        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
        assertThat(jwtService.accessTokenUser).isSameAs(user);
        assertThat(refreshTokenService.refreshTokenUser).isSameAs(user);
    }

    @Test
    void loginThrowsInvalidCredentialsWhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("amina@example.com", "wrong-password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        verify(userRepository, never()).findByEmailIgnoreCase(any());
        assertThat(jwtService.accessTokenUser).isNull();
        assertThat(refreshTokenService.refreshTokenUser).isNull();
    }

    @Test
    void loginThrowsInvalidCredentialsWhenUserLookupFails() {
        LoginRequest request = new LoginRequest("missing@example.com", "StrongPass123");
        when(userRepository.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");

        assertThat(jwtService.accessTokenUser).isNull();
        assertThat(refreshTokenService.refreshTokenUser).isNull();
    }

    private User user() {
        return new User(
                "Amina",
                null,
                "Rahman",
                "amina@example.com",
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

    private static class TestJwtService extends JwtService {

        private User accessTokenUser;

        TestJwtService() {
            super(new JwtProperties("test-secret-that-is-long-enough-for-hs256-signing", 15, 7));
        }

        @Override
        public String generateAccessToken(User user) {
            this.accessTokenUser = user;
            return "access-token";
        }

        @Override
        public long getAccessTokenExpiresInSeconds() {
            return 900L;
        }
    }

    private static class TestRefreshTokenService extends RefreshTokenService {

        private User refreshTokenUser;

        TestRefreshTokenService() {
            super(null, new JwtProperties("test-secret-that-is-long-enough-for-hs256-signing", 15, 7));
        }

        @Override
        public String createRefreshToken(User user) {
            this.refreshTokenUser = user;
            return "refresh-token";
        }
    }
}
