package com.rkind.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotEmpty(message = "Order items are required")
    private List<OrderItemRequest> items;

    @NotNull(message = "Fulfillment type is required")
    private String fulfillmentType;

    private String paymentMethod;

    private BigDecimal deliveryCharge;

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

    // Frontend calculated/displayed values.
    // Backend will NOT blindly trust these.
    private BigDecimal discount;
}