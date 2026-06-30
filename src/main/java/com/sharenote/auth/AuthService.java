package com.sharenote.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import com.sharenote.audit.AuditAction;
import com.sharenote.audit.AuditRecorder;
import com.sharenote.auth.dto.AuthResponse;
import com.sharenote.auth.dto.LoginRequest;
import com.sharenote.auth.dto.RefreshTokenRequest;
import com.sharenote.user.AccountBannedException;
import com.sharenote.user.UserRepository;
import com.sharenote.user.entities.User;
import com.sharenote.user.enums.ProfileHealth;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final AuditRecorder auditRecorder;
    private final RedisTemplate redisTemplate;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserRepository userRepository,
            AuditRecorder auditRecorder,
            RedisTemplate redisTemplate) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.auditRecorder = auditRecorder;
        this.redisTemplate = redisTemplate;
    }

    public AuthResponse login(LoginRequest request) {
        String username = request.username().trim();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.password()));
        } catch (AuthenticationException exception) {
            auditRecorder.recordAnonymous(
                    AuditAction.LOGIN_FAILED,
                    username,
                    "USER",
                    null,
                    "Login failed"
            );
            throw new InvalidCredentialsException();
        }

        User user = userRepository.findUserByUsername(username)
                .orElseThrow(InvalidCredentialsException::new);
        if (user.getProfileHealth().getCode()>1) {
            auditRecorder.record(AuditAction.LOGIN_FAILED, user, "USER", user.getId(), "Login blocked for banned user");
            throw new AccountBannedException(buildBanMessage(user));
        }

        auditRecorder.record(AuditAction.LOGIN_SUCCEEDED, user, "USER", user.getId(), "Login succeeded");

        return new AuthResponse(
                jwtService.generateAccessToken(user),
                refreshTokenService.createRefreshToken(user),
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds());
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenRotation rotation = refreshTokenService.rotate(request.refreshToken());
        auditRecorder.record(
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
        if (user.getProfileHealth().getCode() == 3) {
            return "Account is permanently banned. Notice: " + user.getUserPolicyStatus().getBanNotice();
        }
        return "Account is temporarily banned until " + user.getUserPolicyStatus().getBannedUntil() + ". Notice: " + user.getUserPolicyStatus().getBanNotice();
    }
}
