package com.sharenote.auth;

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

import javax.crypto.SecretKey;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.sharenote.cache.CacheManager;
import com.sharenote.security.registry.PermissionRegistry;
import com.sharenote.security.registry.RoleRegistry;
import com.sharenote.user.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "userId";
    private static final String USER_ROLE_CLAIM = "userRoles";
    private static final String USER_PERMISSION_CLAIM = "userPermission";

    private final JwtProperties jwtProperties;

    private final Clock clock;
    private final CacheManager cache;
    private final PermissionRegistry permissionRegistry;
    private final RoleRegistry roleRegistry;

    public JwtService(JwtProperties jwtProperties,
            CacheManager cache, PermissionRegistry permissionRegistry, RoleRegistry roleRegistry) {
        this.jwtProperties = jwtProperties;

        this.clock = Clock.systemUTC();
        this.cache = cache;
        this.permissionRegistry = permissionRegistry;
        this.roleRegistry = roleRegistry;
    }

    public JwtService(JwtProperties jwtProperties) {
        this(jwtProperties, null, null, null);
    }

    // Generates a signed access token that includes user identity, roles, and
    // dynamic permissions.
    public String generateAccessToken(User user) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(getAccessTokenLifetime());

        Set<Long> userRoles = user.getRoles().stream().map(r -> r.getId()).collect(Collectors.toSet());
        Set<Long> userPermissions = user.getRoles().stream().flatMap(r -> r.getPermissions().stream())
                .map(p -> p.getId()).collect(Collectors.toSet());

        String authToken = Jwts.builder()
                .subject(user.getUsername())
                .claims(Map.of(
                        USER_ID_CLAIM, user.getId(),
                        USER_ROLE_CLAIM, userRoles,
                        USER_PERMISSION_CLAIM, userPermissions))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(getSigningKey())
                .compact();

        // this.cacheAuthToken(user.getId(), authToken);
        return authToken;
    }

    // Returns the configured access token lifetime in seconds for API responses.
    public long getAccessTokenExpiresInSeconds() {
        return getAccessTokenLifetime().toSeconds();
    }

    // Extracts the token subject, which is the normalized user email.
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Extracts role names from a verified token claim for compatibility with
    // role-aware code.
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

    // Validates signature, expiration, and subject against an existing UserDetails
    // object.
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

    // Reads list-style claims defensively because JWT parsers expose collection
    // values as raw objects.
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

    // Returns the configured access token lifetime as a Duration.
    private Duration getAccessTokenLifetime() {
        return Duration.ofMinutes(jwtProperties.accessTokenExpirationMinutes());
    }

    // Builds the HMAC signing key from the configured secret.
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public List<String> extractPermissions(String token) {
        List<String> permissionIds = extractStringListClaim(token, USER_PERMISSION_CLAIM);

        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }

        return permissionIds.stream()
                .filter(this::isNumeric)
                .map(Long::valueOf)
                .map(permissionRegistry::getDefinition)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<String> extractRoles(String token) {
        List<String> roleIds = extractStringListClaim(token, USER_ROLE_CLAIM);

        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }

        return roleIds.stream()
                .filter(this::isNumeric)
                .map(Long::valueOf)
                .map(roleRegistry::getDefinition)
                .filter(Objects::nonNull)
                .toList();
    }

    private boolean isNumeric(String str) {
        if (str == null)
            return false;
        return str.matches("\\d+");
    }
}
