package com.rkind.ecommerce.auth.service;

import com.rkind.ecommerce.auth.entity.RefreshToken;
import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.RefreshTokenRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long webExpiration;
    private final long androidExpiration;

    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-expiration-web:604800000}") long webExpiration,
            @Value("${jwt.refresh-expiration-android:2592000000}") long androidExpiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.webExpiration = webExpiration;
        this.androidExpiration = androidExpiration;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user, String platform) {
        String normalizedPlatform = platform == null
                ? "web"
                : platform.trim().toLowerCase();

        long expiration;

        if ("web".equals(normalizedPlatform)) {
            expiration = webExpiration;
        } else if ("android".equals(normalizedPlatform)) {
            expiration = androidExpiration;
        } else {
            throw new IllegalArgumentException(
                    "Unsupported platform. Use web or android."
            );
        }

        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes));
        refreshToken.setExpiryDate(
                LocalDateTime.now().plusNanos(expiration * 1_000_000)
        );
        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Refresh token not found. Please log in again."
                ));

        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException(
                    "Refresh token has been revoked. Please log in again."
            );
        }

        if (!refreshToken.getExpiryDate().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Refresh token expired. Please log in again."
            );
        }

        if (!Boolean.TRUE.equals(refreshToken.getUser().getEnabled())) {
            throw new IllegalArgumentException(
                    "Account is disabled."
            );
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Refresh token not found."
                ));

        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
    }
}