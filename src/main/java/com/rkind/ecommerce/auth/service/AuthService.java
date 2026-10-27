
package com.rkind.ecommerce.auth.service;

import com.rkind.ecommerce.auth.dto.request.AdminLoginRequest;
import com.rkind.ecommerce.auth.dto.request.SendOtpRequest;
import com.rkind.ecommerce.auth.dto.request.VerifyOtpRequest;
import com.rkind.ecommerce.auth.dto.response.AuthResponse;
import com.rkind.ecommerce.auth.entity.RefreshToken;
import com.rkind.ecommerce.auth.entity.Role;
import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;
import com.rkind.ecommerce.security.JwtService;

import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@AllArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    // CUSTOMER: Send OTP
    public void sendCustomerOtp(SendOtpRequest request) {

        String email = normalizeEmail(request.email());

        otpService.sendOtp(email);
    }

    // CUSTOMER: Verify OTP and login/register
    @Transactional
    public AuthResponse verifyCustomerOtp(VerifyOtpRequest request) {

        String email = normalizeEmail(request.email());

        boolean verified = otpService.verifyOtp(
                email,
                request.otp()
        );

        if (!verified) {
            throw new IllegalArgumentException(
                    "Invalid or expired OTP"
            );
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createCustomer(email));

        if (user.getRole() != Role.CUSTOMER) {
            throw new IllegalArgumentException(
                    "This email is not registered as a customer"
            );
        }

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException(
                    "Your account is disabled"
            );
        }

        String token = jwtService.generateToken(user.getEmail());

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user, "web");

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                refreshToken.getToken()
        );
    }

    // ADMIN: Email and password login
    @Transactional
    public AuthResponse adminLogin(AdminLoginRequest request) {

        String email = normalizeEmail(request.email());

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password"
                        )
                );

        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        if (!Boolean.TRUE.equals(admin.getEnabled())) {
            throw new IllegalArgumentException(
                    "Admin account is disabled"
            );
        }

        if (admin.getPassword() == null ||
                !passwordEncoder.matches(
                        request.password(),
                        admin.getPassword()
                )) {
            throw new IllegalArgumentException(
                    "Invalid email or password"
            );
        }

        String token = jwtService.generateToken(admin.getEmail());

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(admin, "web");

        return new AuthResponse(
                token,
                "Bearer",
                admin.getId(),
                admin.getFullName(),
                admin.getEmail(),
                admin.getRole(),
                refreshToken.getToken()
        );
    }

    // Create a new customer after successful OTP verification
    private User createCustomer(String email) {

        User customer = User.builder()
                .fullName("Customer")
                .email(email)
                .password(null)
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        return userRepository.save(customer);
    }

    // Normalize email to avoid case-sensitive duplicates
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }


    // ADMIN: Send OTP for forgotten password
    public void sendAdminPasswordResetOtp(String rawEmail) {

        String email = normalizeEmail(rawEmail);

        User admin = userRepository.findByEmail(email).orElse(null);

        // Do not send OTP to non-admin or disabled accounts
        if (admin == null ||
                admin.getRole() != Role.ADMIN ||
                !Boolean.TRUE.equals(admin.getEnabled())) {
            return;
        }

        otpService.sendOtp(email);
    }

    // ADMIN: Reset password after OTP verification
    @Transactional
    public void resetAdminPassword(
            String rawEmail,
            String otp,
            String newPassword
    ) {
        String email = normalizeEmail(rawEmail);

        if (newPassword == null || newPassword.isBlank()
                || newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters long"
            );
        }

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid or expired OTP"
                ));

        if (admin.getRole() != Role.ADMIN ||
                !Boolean.TRUE.equals(admin.getEnabled())) {
            throw new IllegalArgumentException(
                    "Invalid or expired OTP"
            );
        }

        boolean verified = otpService.verifyOtp(email, otp);

        if (!verified) {
            throw new IllegalArgumentException(
                    "Invalid or expired OTP"
            );
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
    }

    // ADMIN: Change password while logged in
    @Transactional
    public void changeAdminPassword(
            String rawEmail,
            String currentPassword,
            String newPassword
    ) {
        String email = normalizeEmail(rawEmail);

        User admin = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Admin account not found"
                ));

        if (admin.getRole() != Role.ADMIN ||
                !Boolean.TRUE.equals(admin.getEnabled())) {
            throw new IllegalArgumentException(
                    "Admin account is not available"
            );
        }

        if (admin.getPassword() == null ||
                !passwordEncoder.matches(
                        currentPassword,
                        admin.getPassword()
                )) {
            throw new IllegalArgumentException(
                    "Current password is incorrect"
            );
        }

        if (newPassword == null || newPassword.isBlank()
                || newPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "New password must be at least 8 characters long"
            );
        }

        if (passwordEncoder.matches(
                newPassword,
                admin.getPassword()
        )) {
            throw new IllegalArgumentException(
                    "New password must be different from current password"
            );
        }

        admin.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(admin);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        RefreshToken refreshToken =
                refreshTokenService.verifyRefreshToken(rawRefreshToken);

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getEmail());

        return new AuthResponse(
                newAccessToken,
                "Bearer",
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                refreshToken.getToken()
        );
    }

}
