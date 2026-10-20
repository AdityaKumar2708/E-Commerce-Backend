
package com.rkind.ecommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long jwtExpiration;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long jwtExpiration
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is missing or empty"
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "JWT secret must be a valid Base64 string",
                    e
            );
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret must decode to at least 32 bytes"
            );
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtExpiration = jwtExpiration;
    }

    // Generate JWT token
    public String generateToken(String email) {

        Date now = new Date();
        Date expiryDate = new Date(
                now.getTime() + jwtExpiration
        );

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    // Extract email from JWT
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract expiration date
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    // Validate JWT token
    public boolean isTokenValid(String token, String email) {
        try {
            Claims claims = extractAllClaims(token);

            return email != null
                    && email.equals(claims.getSubject())
                    && claims.getExpiration() != null
                    && claims.getExpiration().after(new Date());

        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Extract all claims
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
