package com.swapi.challenge.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
public class JwtTestUtils {
    public static String gen(String username, String secret, long ttlSeconds){
        long now = System.currentTimeMillis();
        Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); // requiere >=32 bytes

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlSeconds * 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}