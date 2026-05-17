package com.sharenote.auth;

import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditPublisher;
import com.sharenote.auth.dto.AuthResponse;
import com.sharenote.auth.dto.LoginRequest;
import com.sharenote.auth.dto.RefreshTokenRequest;
import com.sharenote.user.AccountBannedException;
import com.sharenote.user.User;
import com.sharenote.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final AuditPublisher auditPublisher;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            AuditPublisher auditPublisher) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.auditPublisher = auditPublisher;
    }

    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password()));
        } catch (AuthenticationException exception) {
            auditPublisher.publishAnonymous(
                    AuditAction.LOGIN_FAILED,
                    normalizedEmail,
                    "USER",
                    null,
                    "Login failed"
            );
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(InvalidCredentialsException::new);
        if (user.isCurrentlyBanned(java.time.Instant.now())) {
            auditPublisher.publish(AuditAction.LOGIN_FAILED, user, "USER", user.getId(), "Login blocked for banned user");
            throw new AccountBannedException(buildBanMessage(user));
        }

        auditPublisher.publish(AuditAction.LOGIN_SUCCEEDED, user, "USER", user.getId(), "Login succeeded");

        return new AuthResponse(
                jwtService.generateAccessToken(user),
                refreshTokenService.createRefreshToken(user),
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenRotation rotation = refreshTokenService.rotate(request.refreshToken());
        auditPublisher.publish(
                AuditAction.REFRESH_TOKEN_USED,
                rotation.user(),
                "USER",
                rotation.user().getId(),
                "Refresh token rotated"
        );

        return new AuthResponse(
                jwtService.generateAccessToken(rotation.user()),
                rotation.refreshToken(),
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds());
    }

    private String buildBanMessage(User user) {
        if (user.isPermanentlyBanned()) {
            return "Account is permanently banned. Notice: " + user.getBanNotice();
        }
        return "Account is temporarily banned until " + user.getBannedUntil() + ". Notice: " + user.getBanNotice();
    }
}
