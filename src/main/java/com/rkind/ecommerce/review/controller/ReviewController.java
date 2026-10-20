package com.rkind.ecommerce.review.controller;

import com.rkind.ecommerce.review.dto.CreateReviewRequest;
import com.rkind.ecommerce.review.entity.Review;
import com.rkind.ecommerce.review.service.ReviewService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customer/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/order-item/{orderItemId}")
    public ResponseEntity<?> createReview(
            @PathVariable Long orderItemId,
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        try {
            if (authentication == null
                    || authentication.getName() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Please log in again."));
            }

            Review review = reviewService.createReview(
                    orderItemId,
                    authentication.getName(),
                    request
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Review submitted successfully.",
                            "reviewId", review.getId(),
                            "orderItemId", orderItemId,
                            "rating", review.getRating()
                    )
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", exception.getMessage())
            );
        }
    }
}