package com.rkind.ecommerce.order.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long productId;

    private String productName;

    private String productImage;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal totalPrice;
    private Long orderItemId;
}