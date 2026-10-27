package com.rkind.ecommerce.auth.repository;

import com.rkind.ecommerce.auth.entity.RefreshToken;
import com.rkind.ecommerce.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteAllByUser(User user);
}