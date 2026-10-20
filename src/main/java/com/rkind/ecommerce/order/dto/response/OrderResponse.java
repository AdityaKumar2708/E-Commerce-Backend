package com.rkind.ecommerce.order.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;

    private String orderNumber;

    private String fulfillmentType;

    private String shiprocketStatus;
    private String shiprocketShipmentId;
    private String shiprocketAwbCode;
    private String shiprocketError;

    private String orderStatus;

    private String paymentStatus;

    private String paymentMethod;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    // Amount details
    private BigDecimal productTotal;

    private BigDecimal deliveryCharge;

    private BigDecimal discount;

    private BigDecimal totalAmount;

    // Customer details
    private String customerName;

    private String customerEmail;

    private String customerPhone;

    // Delivery address
    private String addressLine1;

    private String addressLine2;

    private String city;

    private String state;

    private String pinCode;

    // Store pickup
    private String pickupStore;

    private LocalDateTime expectedPickupTime;

    private LocalDateTime pickedUpAt;

    // Order items
    private List<OrderItemResponse> items;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}