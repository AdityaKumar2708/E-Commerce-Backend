package com.rkind.ecommerce.review.repository;

import com.rkind.ecommerce.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository
        extends JpaRepository<Review, Long> {

    boolean existsByOrderItem_Id(Long orderItemId);
}