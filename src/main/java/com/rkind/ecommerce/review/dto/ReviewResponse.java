
package com.rkind.ecommerce.review.dto;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        String customerName,
        Integer rating,
        String message,
        String photoUrl,
        LocalDateTime createdAt
) {}
