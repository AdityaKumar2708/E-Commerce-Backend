package com.rkind.ecommerce.order.entity;

import com.rkind.ecommerce.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Order to which this item belongs
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Product reference
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Product details snapshot
    @Column(nullable = false, length = 200)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @PrePersist
    @PreUpdate
    protected void calculateTotalPrice() {

        if (unitPrice != null && quantity != null) {
            totalPrice = unitPrice.multiply(
                    BigDecimal.valueOf(quantity)
            );
        }
    }
}