package com.sharenote.auth;

import com.sharenote.user.AccountBannedException;
import com.sharenote.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private static final int REFRESH_TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom;
    private final Clock clock;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.secureRandom = new SecureRandom();
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public String createRefreshToken(User user) {
        Instant now = Instant.now(clock);
        String rawToken = generateRawToken();
        refreshTokenRepository.save(new RefreshToken(
                hashToken(rawToken),
                user,
                now.plus(jwtProperties.refreshTokenExpirationDays(), ChronoUnit.DAYS),
                now
        ));
        return rawToken;
    }

    @Transactional
    public RefreshTokenRotation rotate(String rawToken) {
        RefreshToken existingToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(hashToken(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (existingToken.isRevoked() || existingToken.isExpired(Instant.now(clock))) {
            existingToken.revoke();
            throw new InvalidRefreshTokenException();
        }

        existingToken.revoke();
        User user = existingToken.getUser();
        user.getRoles().size();
        if (user.isCurrentlyBanned(Instant.now(clock))) {
            throw new AccountBannedException("Account is banned. Refresh token cannot be used.");
        }

        String newRawToken = createRefreshToken(user);
        return new RefreshTokenRotation(user, newRawToken);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
