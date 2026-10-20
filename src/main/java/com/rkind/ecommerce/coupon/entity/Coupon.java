package com.rkind.ecommerce.coupon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupons",
        indexes = {
                @Index(name = "idx_coupon_code", columnList = "code")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            nullable = false,
            unique = true,
            length = 50
    )
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private CouponType type;

    @Column(
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal discountValue;

    /*
     * Minimum product subtotal required
     * to use this coupon.
     */
    @Column(
            precision = 12,
            scale = 2
    )
    private BigDecimal minimumOrderValue;

    /*
     * Maximum discount allowed.
     *
     * Mainly useful for percentage coupons.
     *
     * Example:
     * 20% OFF, maximum ₹500
     */
    @Column(
            precision = 12,
            scale = 2
    )
    private BigDecimal maximumDiscount;

    @Column(nullable = false)
    private Boolean active = true;

    /*
     * Total number of times this coupon
     * can be used.
     *
     * NULL = unlimited
     */
    private Integer usageLimit;

    /*
     * How many times this coupon has
     * already been used.
     */
    @Column(nullable = false)
    private Integer usedCount = 0;

    /*
     * Maximum number of times one customer
     * can use this coupon.
     *
     * NULL = unlimited
     */
    private Integer perCustomerLimit;

    private LocalDateTime startAt;

    private LocalDateTime expiresAt;

    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (active == null) {
            active = true;
        }

        if (usedCount == null) {
            usedCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}