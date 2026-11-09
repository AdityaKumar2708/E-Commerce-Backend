
package com.rkind.ecommerce.review.controller;

import com.rkind.ecommerce.review.dto.ReviewResponse;
import com.rkind.ecommerce.review.service.ReviewService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductReviewController {

    private final ReviewService reviewService;

    public ProductReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/{productId}/reviews")
    public List<ReviewResponse> getProductReviews(
            @PathVariable Long productId
    ) {
        return reviewService.getProductReviews(productId);
    }
}
