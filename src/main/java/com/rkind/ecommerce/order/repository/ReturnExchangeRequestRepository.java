
package com.rkind.ecommerce.order.repository;

import com.rkind.ecommerce.order.entity.ReturnExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnExchangeRequestRepository
        extends JpaRepository<ReturnExchangeRequest, Long> {

    List<ReturnExchangeRequest>
    findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<ReturnExchangeRequest>
    findByStatusOrderByCreatedAtAsc(
            com.rkind.ecommerce.order.entity.ReturnExchangeStatus status
    );

    boolean existsByOrder_IdAndStatusIn(
            Long orderId,
            List<com.rkind.ecommerce.order.entity.ReturnExchangeStatus> statuses
    );
}
