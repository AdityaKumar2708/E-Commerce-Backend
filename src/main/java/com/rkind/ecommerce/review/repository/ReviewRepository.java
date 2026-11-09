
package com.rkind.ecommerce.review.repository;

import com.rkind.ecommerce.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderItem_Id(Long orderItemId);

    List<Review> findByOrderItem_Product_IdOrderByCreatedAtDesc(
            Long productId
    );
}
