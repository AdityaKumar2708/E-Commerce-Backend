package com.rkind.ecommerce.notification.repository;

import com.rkind.ecommerce.notification.entity.OrderNotificationEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderNotificationEmailRepository
        extends JpaRepository<OrderNotificationEmail, Long> {

    List<OrderNotificationEmail> findAllByOrderByCreatedAtAsc();

    Optional<OrderNotificationEmail> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}