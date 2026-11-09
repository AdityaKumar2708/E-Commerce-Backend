package com.rkind.ecommerce.notification.dto.response;

import java.util.List;

public record OrderNotificationResponse(
        boolean enabled,
        List<String> emails
) {
}