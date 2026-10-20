
package com.rkind.ecommerce.auth.repository;

import com.rkind.ecommerce.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by email for login
    Optional<User> findByEmail(String email);

    // Check whether email already exists
    boolean existsByEmail(String email);

    // Find user by phone number
    Optional<User> findByPhone(String phone);

    // Check whether phone number already exists
    boolean existsByPhone(String phone);
}
