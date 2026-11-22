package com.omar.beautycenter.auth;

import com.omar.beautycenter.common.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(
                properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public GeneratedAccessToken generateAccessToken(UserPrincipal principal) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.accessTokenExpiration());
        String token = Jwts.builder()
                .issuer(properties.issuer())
                .subject(principal.getUsername())
                .claim("userId", principal.id())
                .claim("displayName", principal.displayName())
                .claim("roles", principal.roles().stream().map(Enum::name).sorted().toList())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new GeneratedAccessToken(
                token,
                expiresAt,
                properties.accessTokenExpiration().toSeconds());
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        return userDetails.isEnabled()
                && userDetails.getUsername().equals(claims.getSubject())
                && properties.issuer().equals(claims.getIssuer())
                && claims.getExpiration().toInstant().isAfter(clock.instant());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record GeneratedAccessToken(
            String value,
            Instant expiresAt,
            long expiresInSeconds) {
    }
}
