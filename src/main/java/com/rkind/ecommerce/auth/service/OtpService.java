
package com.rkind.ecommerce.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.security.SecureRandom;

@Service
public class OtpService {

    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    private final Map<String, OtpEntry> otpStore =
            new ConcurrentHashMap<>();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${RESEND_API_KEY:}")
    private String resendApiKey;

    @Value("${RESEND_FROM_EMAIL:otp@rajeshstaionery.store}")
    private String resendFromEmail;

    @Value("${app.otp.expiration-ms:300000}")
    private long otpExpirationMs;

    @Value("${app.otp.resend-cooldown-ms:60000}")
    private long resendCooldownMs;

    private static final int MAX_ATTEMPTS = 5;

    public OtpService(
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    // Generate and send OTP through Resend API
    public synchronized void sendOtp(String email) {

        long now = System.currentTimeMillis();

        OtpEntry existing = otpStore.get(email);

        if (existing != null &&
                now - existing.sentAt < resendCooldownMs) {
            throw new IllegalStateException(
                    "Please wait before requesting another OTP"
            );
        }

        if (resendApiKey == null || resendApiKey.isBlank()) {
            throw new IllegalStateException(
                    "Resend API key is not configured"
            );
        }

        String otp = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        Map<String, Object> payload = Map.of(
                "from", "Rajesh Stationery <" + resendFromEmail + ">",
                "to", List.of(email),
                "subject", "Your E-Commerce Login OTP",
                "text",
                "Hello,\n\n" +
                        "Your verification OTP is: " + otp + "\n\n" +
                        "This OTP is valid for 5 minutes.\n" +
                        "Do not share this OTP with anyone.\n\n" +
                        "Regards,\nE-Commerce Team"
        );

        try {
            String requestBody =
                    objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 ||
                    response.statusCode() >= 300) {

                System.err.println(
                        "Resend email failed. HTTP status: " +
                                response.statusCode() +
                                ", response: " + response.body()
                );

                throw new IllegalStateException(
                        "Unable to send OTP email. Please try again later."
                );
            }

            // Save OTP only after Resend accepts the email request
            long sentAt = System.currentTimeMillis();

            otpStore.put(
                    email,
                    new OtpEntry(
                            passwordEncoder.encode(otp),
                            sentAt + otpExpirationMs,
                            sentAt,
                            0
                    )
            );

            System.out.println("OTP email accepted by Resend for: " + email);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "OTP email request was interrupted"
            );
        } catch (IOException e) {
            System.err.println(
                    "Resend API request failed: " + e.getMessage()
            );
            throw new IllegalStateException(
                    "Unable to send OTP email. Please try again later."
            );
        }
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

        if (now > entry.expiresAt) {
            otpStore.remove(email);
            return false;
        }

        if (entry.attempts >= MAX_ATTEMPTS) {
            otpStore.remove(email);
            return false;
        }

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
