
package com.rkind.ecommerce.auth.controller;

import com.rkind.ecommerce.auth.dto.request.*;
import com.rkind.ecommerce.auth.dto.response.AuthResponse;
import com.rkind.ecommerce.auth.service.AuthService;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // CUSTOMER: Send OTP
    @PostMapping("/customer/send-otp")
    public ResponseEntity<Map<String, String>> sendCustomerOtp(
            @Valid @RequestBody SendOtpRequest request
    ) {
        authService.sendCustomerOtp(request);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "OTP sent successfully to your email"
                )
        );
    }

    // CUSTOMER: Verify OTP and register/login
    @PostMapping("/customer/verify-otp")
    public ResponseEntity<AuthResponse> verifyCustomerOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        AuthResponse response =
                authService.verifyCustomerOtp(request);

        return ResponseEntity.ok(response);
    }

    // ADMIN: Email and password login
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        AuthResponse response =
                authService.adminLogin(request);

        return ResponseEntity.ok(response);
    }

    // Handle authentication errors
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorized(
            IllegalArgumentException ex
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", ex.getMessage()));
    }

    // Handle OTP resend cooldown
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(
            IllegalStateException ex
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("message", ex.getMessage()));
    }

    // ADMIN: Forgot password - send OTP
    @PostMapping("/admin/forgot-password")
    public ResponseEntity<Map<String, String>> forgotAdminPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        authService.sendAdminPasswordResetOtp(request.email());

        return ResponseEntity.ok(
                Map.of("message", "If the admin account exists, an OTP has been sent.")
        );
    }


    // ADMIN: Reset password using OTP
    @PostMapping("/admin/reset-password")
    public ResponseEntity<Map<String, String>> resetAdminPassword(
            @Valid @RequestBody ResetAdminPasswordRequest request
    ) {
        authService.resetAdminPassword(
                request.email(),
                request.otp(),
                request.newPassword()
        );

        return ResponseEntity.ok(
                Map.of("message", "Password reset successfully")
        );
    }


    // ADMIN: Change password while logged in
    @PostMapping("/admin/change-password")
    public ResponseEntity<Map<String, String>> changeAdminPassword(
            @Valid @RequestBody ChangeAdminPasswordRequest request,
            Authentication authentication
    ) {
        authService.changeAdminPassword(
                authentication.getName(),
                request.currentPassword(),
                request.newPassword()
        );

        return ResponseEntity.ok(
                Map.of("message", "Password changed successfully")
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResponse response =
                authService.refreshAccessToken(request.refreshToken());

        return ResponseEntity.ok(response);
    }
}
