
package com.rkind.ecommerce.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {

    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, OtpEntry> otpStore =
            new ConcurrentHashMap<>();

    @Value("${app.otp.expiration-ms:300000}")
    private long otpExpirationMs;

    @Value("${app.otp.resend-cooldown-ms:60000}")
    private long resendCooldownMs;

    private static final int MAX_ATTEMPTS = 5;

    public OtpService(
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder
    ) {
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    // Generate and send OTP
    public synchronized void sendOtp(String email) {

        long now = System.currentTimeMillis();

        OtpEntry existing = otpStore.get(email);

        if (existing != null &&
                now - existing.sentAt < resendCooldownMs) {
            throw new IllegalStateException(
                    "Please wait before requesting another OTP"
            );
        }

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Your E-Commerce Login OTP");
        message.setText(
                "Hello,\n\n" +
                        "Your verification OTP is: " + otp + "\n\n" +
                        "This OTP is valid for 5 minutes.\n" +
                        "Do not share this OTP with anyone.\n\n" +
                        "Regards,\nE-Commerce Team"
        );

        // Send email before saving the OTP
        mailSender.send(message);

        // Store only the hashed OTP
        otpStore.put(
                email,
                new OtpEntry(
                        passwordEncoder.encode(otp),
                        now + otpExpirationMs,
                        now,
                        0
                )
        );
    }

    // Verify OTP
    public synchronized boolean verifyOtp(
            String email,
            String otp
    ) {
        OtpEntry entry = otpStore.get(email);

        if (entry == null) {
            return false;
        }

        long now = System.currentTimeMillis();

        // Check OTP expiry
        if (now > entry.expiresAt) {
            otpStore.remove(email);
            return false;
        }

        // Check maximum attempts
        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(email);
            return false;
        }

        // Verify hashed OTP
        if (!passwordEncoder.matches(otp, entry.otpHash)) {
            entry.attempts++;

            if (entry.attempts >= MAX_ATTEMPTS) {
                otpStore.remove(email);
            }

            return false;
        }

        // OTP can only be used once
        otpStore.remove(email);

        return true;
    }

    // OTP storage model
    private static class OtpEntry {

        private final String otpHash;
        private final long expiresAt;
        private final long sentAt;
        private int attempts;

        private OtpEntry(
                String otpHash,
                long expiresAt,
                long sentAt,
                int attempts
        ) {
            this.otpHash = otpHash;
            this.expiresAt = expiresAt;
            this.sentAt = sentAt;
            this.attempts = attempts;
        }
    }
}
