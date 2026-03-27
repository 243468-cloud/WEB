package com.squadup.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utilidad para crear y validar tokens JWT.
 * Usa la API de jjwt 0.12.x (fluent builder sin métodos deprecated).
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Requiere mínimo 256 bits (32 chars) en el secret
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationMs = expirationMs;
    }

    /** Genera token con el email del usuario como subject */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email) // jjwt 0.12
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key) // jjwt 0.12: infiere algoritmo de la clave
                .compact();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser() // jjwt 0.12: ya no existe parserBuilder()
                .verifyWith(key) // reemplaza setSigningKey()
                .build()
                .parseSignedClaims(token) // reemplaza parseClaimsJws()
                .getPayload(); // reemplaza getBody()
    }
}
