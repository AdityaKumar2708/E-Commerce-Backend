package com.rkind.ecommerce.review.service;

import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.auth.repository.UserRepository;
import com.rkind.ecommerce.order.entity.OrderItem;
import com.rkind.ecommerce.order.entity.OrderStatus;
import com.rkind.ecommerce.order.repository.OrderItemRepository;
import com.rkind.ecommerce.review.dto.CreateReviewRequest;
import com.rkind.ecommerce.review.entity.Review;

import com.rkind.ecommerce.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            OrderItemRepository orderItemRepository,
            UserRepository userRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Review createReview(
            Long orderItemId,
            String customerEmail,
            CreateReviewRequest request
    ) {
        if (request.getRating() == null
                || request.getRating() < 1
                || request.getRating() > 5) {
            throw new IllegalArgumentException(
                    "Rating must be between 1 and 5."
            );
        }

        User user = userRepository.findByEmail(customerEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException("Customer not found.")
                );

        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order item not found.")
                );

        // Confirm this order item belongs to the logged-in customer.
        if (item.getOrder() == null
                || item.getOrder().getUser() == null
                || !item.getOrder().getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "You cannot review this order item."
            );
        }

        // Only delivered orders are eligible.
        if (item.getOrder().getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException(
                    "You can review products only after delivery."
            );
        }

        if (reviewRepository.existsByOrderItem_Id(orderItemId)) {
            throw new IllegalArgumentException(
                    "You have already reviewed this purchased item."
            );
        }

        String reviewText = request.getReviewText();

        if (reviewText != null && reviewText.isBlank()) {
            reviewText = null;
        }

        String photoUrl = request.getPhotoUrl();

        if (photoUrl != null && photoUrl.isBlank()) {
            photoUrl = null;
        }

        Review review = Review.builder()
                .orderItem(item)
                .user(user)
                .rating(request.getRating())
                .reviewText(reviewText)
                .photoUrl(photoUrl)
                .build();

        return reviewRepository.save(review);
    }
}