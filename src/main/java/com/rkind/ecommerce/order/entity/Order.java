package com.rkind.ecommerce.order.entity;

import com.rkind.ecommerce.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique customer-facing order number
    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    // Customer who placed the order
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Customer details snapshot at the time of order
    @Column(nullable = false, length = 100)
    private String customerName;

    @Column(nullable = false, length = 150)
    private String customerEmail;

    @Column(length = 15)
    private String customerPhone;

    // Shiprocket integration
    @Column(length = 100)
    private String shiprocketOrderId;

    @Column(length = 100)
    private String shiprocketShipmentId;

    @Column(length = 100)
    private String shiprocketAwbCode;

    @Column(length = 40)
    private String shiprocketStatus;

    @Column(columnDefinition = "TEXT")
    private String shiprocketError;

    private Boolean shiprocketManifestGenerated = false;

    // Fulfillment
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FulfillmentType fulfillmentType;

    // Order status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus;

    // Payment
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Column(length = 50)
    private String paymentMethod;

    // Razorpay details
    @Column(length = 100)
    private String razorpayOrderId;

    @Column(length = 100)
    private String razorpayPaymentId;

    // Amounts
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal productTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal deliveryCharge;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    // Delivery address snapshot
    @Column(length = 200)
    private String addressLine1;

    @Column(length = 200)
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 10)
    private String pinCode;

    // Store pickup details
    @Column(length = 100)
    private String pickupStore;

    private LocalDateTime expectedPickupTime;

    private LocalDateTime pickedUpAt;

    // Timestamps
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (orderStatus == null) {
            orderStatus = OrderStatus.PENDING;
        }

        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }

        if (deliveryCharge == null) {
            deliveryCharge = BigDecimal.ZERO;
        }

        if (discount == null) {
            discount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}