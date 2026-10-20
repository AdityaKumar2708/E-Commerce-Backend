
package com.rkind.ecommerce.order.service;

import com.rkind.ecommerce.auth.entity.User;
import com.rkind.ecommerce.order.dto.request.ReturnExchangeRequestDto;
import com.rkind.ecommerce.order.entity.*;
import com.rkind.ecommerce.order.repository.OrderRepository;
import com.rkind.ecommerce.order.repository.ReturnExchangeRequestRepository;
import com.rkind.ecommerce.email.EmailService;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnExchangeService {

    private final OrderRepository orderRepository;
    private final ReturnExchangeRequestRepository requestRepository;
    private final EmailService emailService;

    private static final Set<ReturnExchangeStatus> ACTIVE_STATUSES =
            Set.of(
                    ReturnExchangeStatus.PENDING_ADMIN_REVIEW,
                    ReturnExchangeStatus.APPROVED,
                    ReturnExchangeStatus.AWAITING_CUSTOMER_CONSENT,
                    ReturnExchangeStatus.PICKUP_REQUESTED,
                    ReturnExchangeStatus.PICKED_UP,
                    ReturnExchangeStatus.REFUND_INITIATED,
                    ReturnExchangeStatus.REFUND_FAILED,
                    ReturnExchangeStatus.EXCHANGE_PROCESSING
            );

    public ReturnExchangeRequest createRequest(
            Long orderId,
            User customer,
            ReturnExchangeRequestDto dto
    ) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Order not found."));

        if (order.getUser() == null
                || !order.getUser().getId().equals(customer.getId())) {
            throw new AccessDeniedException(
                    "You cannot request a return for this order.");
        }

        if (order.getFulfillmentType() != FulfillmentType.ONLINE
                || order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Return/exchange is available only for delivered online orders.");
        }

        ReturnExchangeType type;
        try {
            type = ReturnExchangeType.valueOf(
                    dto.requestType().trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException(
                    "Request type must be RETURN or EXCHANGE.");
        }

        String reason = dto.reason().trim().toUpperCase();

        if (reason.equals("COLOR_CHANGE")) {
            throw new IllegalArgumentException(
                    "Color-change requests are not eligible for return or exchange.");
        }

        if (type == ReturnExchangeType.EXCHANGE
                && !Set.of(
                "WRONG_OR_MISMATCHED_ITEM",
                "DAMAGED_OR_DEFECTIVE"
        ).contains(reason)) {
            throw new IllegalArgumentException(
                    "Exchange is available only for wrong/mismatched or damaged/defective items.");
        }

        if (!Set.of(
                "WRONG_OR_MISMATCHED_ITEM",
                "DAMAGED_OR_DEFECTIVE",
                "DID_NOT_LIKE_PRODUCT"
        ).contains(reason)) {
            throw new IllegalArgumentException("Invalid request reason.");
        }

        boolean existing = requestRepository
                .existsByOrder_IdAndStatusIn(orderId,
                        List.copyOf(ACTIVE_STATUSES));

        if (existing) {
            throw new IllegalStateException(
                    "An active return/exchange request already exists for this order.");
        }

        ReturnExchangeRequest request = new ReturnExchangeRequest();
        request.setOrder(order);
        request.setCustomer(customer);
        request.setRequestType(type);
        request.setReason(reason);
        request.setCustomerDescription(dto.customerDescription());
        request.setStatus(ReturnExchangeStatus.PENDING_ADMIN_REVIEW);

        ReturnExchangeRequest saved = requestRepository.save(request);

        sendEmail(
                order.getCustomerEmail(),
                "Return/Exchange Request Received",
                "Your request for order " + order.getOrderNumber()
                        + " has been submitted and is pending admin review."
        );

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ReturnExchangeRequest> getMyRequests(Long customerId) {
        return requestRepository
                .findByCustomer_IdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<ReturnExchangeRequest> getPendingRequests() {
        return requestRepository.findByStatusOrderByCreatedAtAsc(
                ReturnExchangeStatus.PENDING_ADMIN_REVIEW);
    }

    public ReturnExchangeRequest approve(Long requestId, String adminNotes) {
        ReturnExchangeRequest request = findRequest(requestId);

        if (request.getStatus()
                != ReturnExchangeStatus.PENDING_ADMIN_REVIEW) {
            throw new IllegalStateException(
                    "Only pending requests can be approved.");
        }

        request.setStatus(ReturnExchangeStatus.APPROVED);
        request.setAdminNotes(adminNotes);

        ReturnExchangeRequest saved = requestRepository.save(request);

        sendEmail(
                request.getOrder().getCustomerEmail(),
                "Return/Exchange Request Approved",
                "Your request for order " + request.getOrder().getOrderNumber()
                        + " has been approved. Further instructions will follow."
        );

        return saved;
    }

    public ReturnExchangeRequest reject(
            Long requestId,
            String rejectionReason
    ) {
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new IllegalArgumentException(
                    "A rejection reason is required.");
        }

        ReturnExchangeRequest request = findRequest(requestId);

        if (request.getStatus()
                != ReturnExchangeStatus.PENDING_ADMIN_REVIEW) {
            throw new IllegalStateException(
                    "Only pending requests can be rejected.");
        }

        request.setStatus(ReturnExchangeStatus.REJECTED);
        request.setRejectionReason(rejectionReason.trim());

        ReturnExchangeRequest saved = requestRepository.save(request);

        sendEmail(
                request.getOrder().getCustomerEmail(),
                "Return/Exchange Request Rejected",
                "Your request for order " + request.getOrder().getOrderNumber()
                        + " was rejected. Reason: " + rejectionReason.trim()
        );

        return saved;
    }

    private ReturnExchangeRequest findRequest(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Return/exchange request not found."));
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            emailService.sendEmail(to, subject, body);
        } catch (Exception ignored) {
            // Email failure must not undo the saved request decision.
        }
    }
}
