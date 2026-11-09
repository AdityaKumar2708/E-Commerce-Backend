package com.rkind.ecommerce.notification.service;

import com.rkind.ecommerce.notification.entity.OrderNotificationEmail;
import com.rkind.ecommerce.notification.entity.OrderNotificationSetting;
import com.rkind.ecommerce.notification.repository.OrderNotificationEmailRepository;
import com.rkind.ecommerce.notification.repository.OrderNotificationSettingRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderNotificationService {

    private static final Long SETTINGS_ID = 1L;

    private final OrderNotificationSettingRepository settingRepository;
    private final OrderNotificationEmailRepository emailRepository;

    @Transactional(readOnly = true)
    public boolean isNotificationEnabled() {
        return settingRepository.findById(SETTINGS_ID)
                .map(setting -> Boolean.TRUE.equals(setting.getEnabled()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<String> getNotificationEmails() {
        return emailRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(OrderNotificationEmail::getEmail)
                .toList();
    }

    public boolean updateNotificationStatus(boolean enabled) {

        OrderNotificationSetting setting =
                settingRepository.findById(SETTINGS_ID)
                        .orElseGet(() -> OrderNotificationSetting.builder()
                                .id(SETTINGS_ID)
                                .enabled(false)
                                .build());

        setting.setEnabled(enabled);

        settingRepository.save(setting);

        return Boolean.TRUE.equals(setting.getEnabled());
    }

    public String addNotificationEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (!normalizedEmail.matches(
                "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$"
        )) {
            throw new IllegalArgumentException(
                    "Please enter a valid email address."
            );
        }

        if (emailRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new IllegalArgumentException("This email already exists.");
        }

        OrderNotificationEmail notificationEmail =
                OrderNotificationEmail.builder()
                        .email(normalizedEmail)
                        .build();

        emailRepository.save(notificationEmail);

        return normalizedEmail;
    }

    public void removeNotificationEmail(String email) {

        OrderNotificationEmail notificationEmail =
                emailRepository.findByEmailIgnoreCase(email.trim())
                        .orElseThrow(() ->
                                new IllegalArgumentException("Email not found."));

        emailRepository.delete(notificationEmail);
    }
}