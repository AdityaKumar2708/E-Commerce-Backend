
package com.rkind.ecommerce.order.repository;

import com.rkind.ecommerce.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    // Fetch logged-in customer's orders, newest first
    List<Order> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
