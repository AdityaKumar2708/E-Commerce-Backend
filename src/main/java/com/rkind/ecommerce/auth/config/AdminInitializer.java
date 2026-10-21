
package com.rkind.ecommerce.auth.config;

import com.rkind.ecommerce.auth.entity.Role;
import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // ADMIN CREDENTIALS
    @Value("${app.admin.email:rajeshstationery1978@gmail.com}")
    private String adminEmail;

    private final String adminPassword =
            System.getenv("RK_Stationery@20_10_2026");

    @Value("${app.admin.full-name:Rahul Kumar}")
    private String adminFullName;

    @Value("${app.admin.phone:9792342742}")
    private String adminPhone;

    @Value("${app.store.name:Rajesh Stationery & Novelty}")
    private String storeName;

    public AdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (adminEmail.isBlank() || adminPassword.isBlank()
                || adminPassword.equals("CHANGE_THIS_PASSWORD")) {
            System.out.println(
                    "Admin initialization skipped: configure admin credentials."
            );
            return;
        }

        String email = adminEmail.trim().toLowerCase(Locale.ROOT);

        var existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            User admin = existingUser.get();

            if (admin.getRole() != Role.ADMIN) {
                throw new IllegalStateException(
                        "Configured admin email already belongs to a non-admin account."
                );
            }

            admin.setFullName(adminFullName);
            admin.setPhone(adminPhone);
            admin.setEnabled(true);

            // Update password to the one configured in this file
            admin.setPassword(passwordEncoder.encode(adminPassword));

            userRepository.save(admin);

            System.out.println("Admin account already exists. Details updated.");
            System.out.println("Store: " + storeName);
            return;
        }

        User admin = User.builder()
                .fullName(adminFullName)
                .email(email)
                .phone(adminPhone)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .build();

        userRepository.save(admin);

        System.out.println("Initial admin account created successfully.");
        System.out.println("Store: " + storeName);
    }
}
