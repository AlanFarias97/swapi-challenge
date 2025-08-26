package com.swapi.challenge.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final Key key;
    private final long expirationMinutes;

    public JwtService(
            @Value("${jwt.secret:change-me-super-secret-32chars-min}") String secret,
            @Value("${jwt.expiration-minutes:60}") long expirationMinutes) {
        // Para HS256 se recomienda >= 32 chars
        if (secret == null || secret.length() < 32) {
            secret = String.format("%-32s", Optional.ofNullable(secret).orElse("change-me")).replace(' ', 'x');
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    public String generateToken(String username, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(expirationMinutes * 60);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .claim("roles", roles)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getBody().getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object v = parseClaims(token).getBody().get("roles");
        if (v instanceof List) {
            List<?> l = (List<?>) v;
            List<String> out = new ArrayList<>();
            for (Object o : l) if (o != null) out.add(o.toString());
            return out;
        }
        return Collections.emptyList();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationEpochMillis(String token) {
        Date exp = parseClaims(token).getBody().getExpiration();
        return exp != null ? exp.getTime() : 0L;
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
