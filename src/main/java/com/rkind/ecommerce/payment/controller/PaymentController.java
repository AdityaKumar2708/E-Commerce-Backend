
package com.rkind.ecommerce.payment.controller;

import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;
import com.rkind.ecommerce.order.dto.response.OrderResponse;
import com.rkind.ecommerce.order.service.OrderService;
import com.rkind.ecommerce.payment.dto.PaymentOrderResponse;
import com.rkind.ecommerce.payment.dto.request.CreatePaymentOrderRequest;
import com.rkind.ecommerce.payment.dto.request.VerifyPaymentRequest;
import com.rkind.ecommerce.payment.service.RazorpayPaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final RazorpayPaymentService razorpayPaymentService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    public PaymentController(
            RazorpayPaymentService razorpayPaymentService,
            OrderService orderService,
            UserRepository userRepository
    ) {
        this.razorpayPaymentService = razorpayPaymentService;
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @Valid @RequestBody CreatePaymentOrderRequest request
    ) throws Exception {
        return ResponseEntity.ok(
                razorpayPaymentService.createOrder(request.getAmount())
        );
    }

    @PostMapping("/verify-and-create-order")
    public ResponseEntity<OrderResponse> verifyAndCreateOrder(
            @Valid @RequestBody VerifyPaymentRequest request,
            Authentication authentication
    ) throws Exception {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Please log in before placing an order.");
        }

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("Authenticated user not found.")
                );

        int amountInPaise = razorpayPaymentService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        OrderResponse response = orderService.createPaidOrder(
                request.getOrder(),
                user.getId(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                amountInPaise
        );

        return ResponseEntity.ok(response);
    }
}
