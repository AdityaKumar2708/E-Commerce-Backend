package com.rkind.ecommerce.order.ccontroller;

import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;
import com.rkind.ecommerce.order.dto.request.CreateOrderRequest;
import com.rkind.ecommerce.order.dto.response.OrderResponse;
import com.rkind.ecommerce.order.service.OrderService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customer/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(
            OrderService orderService,
            UserRepository userRepository
    ) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    // =========================================================
    // CREATE ORDER
    // =========================================================

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication
    ) {

        // JWT authentication name = logged-in user's email
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found."
                        )
                );

        OrderResponse response =
                orderService.createOrder(
                        request,
                        user.getId()
                );

        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<java.util.List<OrderResponse>> getMyOrders(
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated user not found."
                        )
                );

        return ResponseEntity.ok(
                orderService.getMyOrders(user.getId())
        );
    }


    @GetMapping("/{orderId}/tracking")
    public ResponseEntity<java.util.Map<String, Object>> getMyOrderTracking(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated user not found."
                        )
                );

        return ResponseEntity.ok(
                orderService.getMyOrderTracking(
                        orderId,
                        user.getId()
                )
        );
    }


    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in to cancel this order."
            );
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated user not found."
                        )
                );

        OrderResponse response =
                orderService.cancelOrder(orderId, user.getId());

        return ResponseEntity.ok(response);
    }

}