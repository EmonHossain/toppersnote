package com.sharenote.auth;

import com.sharenote.role.Role;
import com.sharenote.role.RoleLevel;
import com.sharenote.security.Permission;
import com.sharenote.security.PermissionAuthorityService;
import com.sharenote.user.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.NonNull;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "userId";
    private static final String USER_ROLE_CLAIM = "userRoles";
    private static final String USER_PERMISSION_CLAIM = "userPermission";

    private final JwtProperties jwtProperties;
    private final PermissionAuthorityService permissionAuthorityService;
    private final Clock clock;

    public JwtService(JwtProperties jwtProperties, PermissionAuthorityService permissionAuthorityService) {
        this.jwtProperties = jwtProperties;
        this.permissionAuthorityService = permissionAuthorityService;
        this.clock = Clock.systemUTC();
    }

    public JwtService(JwtProperties jwtProperties) {
        this(jwtProperties, null);
    }

    // Generates a signed access token that includes user identity, roles, and dynamic permissions.
    public String generateAccessToken(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(getAccessTokenLifetime());

        return Jwts.builder()
                .subject(user.getUsername())
                .claims(Map.of(USER_ID_CLAIM, user.getId(),
                USER_ROLE_CLAIM, user.getRoles().stream().map(r->r.getId()).collect(Collectors.toSet()),
                USER_PERMISSION_CLAIM, user.getRoles().stream().flatMap(r->r.getPermissions().stream()).map(p-> p.getId()).collect(Collectors.toSet())
            ))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();
    }

    // Returns the configured access token lifetime in seconds for API responses.
    public long getAccessTokenExpiresInSeconds() {
        return getAccessTokenLifetime().toSeconds();
    }

    // Extracts the token subject, which is the normalized user email.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extracts role names from a verified token claim for compatibility with role-aware code.
    public List<String> extractUserId(String token) {
        return extractStringListClaim(token, USER_ID_CLAIM);
    }

    // Validates signature and expiration without requiring a database lookup.
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    // Validates signature, expiration, and subject against an existing UserDetails object.
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    // Parses and verifies signed JWT claims using the configured HMAC key.
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Checks whether the token expiration has already passed.
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(Date.from(Instant.now(clock)));
    }

    // Extracts one verified claim through the provided resolver.
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    // Reads list-style claims defensively because JWT parsers expose collection values as raw objects.
    private List<String> extractStringListClaim(String token, String claimName) {
        Object claim = parseClaims(token).get(claimName);
        if (!(claim instanceof Collection<?> values)) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .toList();
    }

    // Resolves role permissions from the database-backed permission authority service.
    private Set<String> resolvePermissions(Long userId) {
        if (permissionAuthorityService == null) {
            return Set.of();
        }
        return permissionAuthorityService.resolvePermissionAuthorities(user.getRoles());
    }

    // Returns the configured access token lifetime as a Duration.
    private Duration getAccessTokenLifetime() {
        return Duration.ofMinutes(jwtProperties.accessTokenExpirationMinutes());
    }

    // Builds the HMAC signing key from the configured secret.
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
