
package com.rkind.ecommerce.order.entity;

import com.rkind.ecommerce.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_exchange_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReturnExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnExchangeType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ReturnExchangeStatus status =
            ReturnExchangeStatus.PENDING_ADMIN_REVIEW;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String customerDescription;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    // Return shipping quote, stored in rupees.
    @Column(precision = 12, scale = 2)
    private BigDecimal returnShippingCharge;

    // Estimated refund in rupees, calculated by backend.
    @Column(precision = 12, scale = 2)
    private BigDecimal estimatedRefundAmount;

    private Boolean customerAcceptedQuote = false;

    private LocalDateTime customerConsentAt;

    // Shiprocket return shipment tracking.
    @Column(length = 100)
    private String returnShipmentId;

    @Column(length = 100)
    private String returnAwbCode;

    @Column(length = 40)
    private String returnPickupStatus;

    // Razorpay refund tracking.
    @Column(length = 100)
    private String razorpayRefundId;

    @Column(length = 40)
    private String refundStatus;

    @Column(precision = 12, scale = 2)
    private BigDecimal actualRefundAmount;

    @Column(columnDefinition = "TEXT")
    private String refundError;

    private LocalDateTime pickupConfirmedAt;

    private LocalDateTime refundInitiatedAt;

    private LocalDateTime refundCompletedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = ReturnExchangeStatus.PENDING_ADMIN_REVIEW;
        }

        if (customerAcceptedQuote == null) {
            customerAcceptedQuote = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
