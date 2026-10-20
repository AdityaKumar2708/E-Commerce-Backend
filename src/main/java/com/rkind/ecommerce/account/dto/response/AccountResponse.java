package com.rkind.ecommerce.account.dto.response;

import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String fullName,
        String email,
        String phone,
        String profileImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}