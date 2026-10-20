package com.rkind.ecommerce.coupon.service;

import com.rkind.ecommerce.coupon.entity.Coupon;
import com.rkind.ecommerce.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    // =========================
    // CREATE COUPON
    // =========================
    @Transactional
    public Coupon createCoupon(Coupon coupon) {

        if (coupon.getCode() == null || coupon.getCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Coupon code is required.");
        }

        String code = coupon.getCode().trim().toUpperCase();

        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new IllegalArgumentException("Coupon code already exists.");
        }

        if (coupon.getType() == null) {
            throw new IllegalArgumentException("Coupon type is required.");
        }

        if (coupon.getDiscountValue() == null ||
                coupon.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Discount value must be greater than zero.");
        }

        if (coupon.getMinimumOrderValue() != null &&
                coupon.getMinimumOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order value cannot be negative."
            );
        }

        if (coupon.getMaximumDiscount() != null &&
                coupon.getMaximumDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Maximum discount must be greater than zero."
            );
        }

        if (coupon.getUsageLimit() != null &&
                coupon.getUsageLimit() <= 0) {
            throw new IllegalArgumentException(
                    "Usage limit must be greater than zero."
            );
        }

        if (coupon.getPerCustomerLimit() != null &&
                coupon.getPerCustomerLimit() <= 0) {
            throw new IllegalArgumentException(
                    "Per customer limit must be greater than zero."
            );
        }

        if (coupon.getStartAt() != null &&
                coupon.getExpiresAt() != null &&
                !coupon.getExpiresAt().isAfter(coupon.getStartAt())) {

            throw new IllegalArgumentException(
                    "Expiry time must be after start time."
            );
        }

        // Percentage coupon cannot exceed 100%
        if (coupon.getType().name().equals("PERCENTAGE") &&
                coupon.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {

            throw new IllegalArgumentException(
                    "Percentage discount cannot exceed 100%."
            );
        }

        coupon.setCode(code);

        if (coupon.getActive() == null) {
            coupon.setActive(true);
        }

        if (coupon.getUsedCount() == null) {
            coupon.setUsedCount(0);
        }

        return couponRepository.save(coupon);
    }

    // =========================
    // GET ALL COUPONS
    // =========================
    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    // =========================
    // GET COUPON BY ID
    // =========================
    public Coupon getCouponById(Long id) {

        return couponRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Coupon not found."
                        )
                );
    }

    // =========================
    // UPDATE COUPON
    // =========================
    @Transactional
    public Coupon updateCoupon(Long id, Coupon updatedCoupon) {

        Coupon existingCoupon = getCouponById(id);

        if (updatedCoupon.getCode() == null ||
                updatedCoupon.getCode().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Coupon code is required."
            );
        }

        String newCode =
                updatedCoupon.getCode().trim().toUpperCase();

        if (!existingCoupon.getCode().equalsIgnoreCase(newCode)
                && couponRepository.existsByCodeIgnoreCase(newCode)) {

            throw new IllegalArgumentException(
                    "Coupon code already exists."
            );
        }

        if (updatedCoupon.getDiscountValue() == null ||
                updatedCoupon.getDiscountValue()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Discount value must be greater than zero."
            );
        }

        if (updatedCoupon.getType() == null) {
            throw new IllegalArgumentException(
                    "Coupon type is required."
            );
        }

        if (updatedCoupon.getType().name().equals("PERCENTAGE") &&
                updatedCoupon.getDiscountValue()
                        .compareTo(new BigDecimal("100")) > 0) {

            throw new IllegalArgumentException(
                    "Percentage discount cannot exceed 100%."
            );
        }

        if (updatedCoupon.getStartAt() != null &&
                updatedCoupon.getExpiresAt() != null &&
                !updatedCoupon.getExpiresAt()
                        .isAfter(updatedCoupon.getStartAt())) {

            throw new IllegalArgumentException(
                    "Expiry time must be after start time."
            );
        }

        existingCoupon.setCode(newCode);
        existingCoupon.setType(updatedCoupon.getType());
        existingCoupon.setDiscountValue(
                updatedCoupon.getDiscountValue()
        );
        existingCoupon.setMinimumOrderValue(
                updatedCoupon.getMinimumOrderValue()
        );
        existingCoupon.setMaximumDiscount(
                updatedCoupon.getMaximumDiscount()
        );
        existingCoupon.setActive(
                updatedCoupon.getActive() != null
                        ? updatedCoupon.getActive()
                        : existingCoupon.getActive()
        );
        existingCoupon.setUsageLimit(
                updatedCoupon.getUsageLimit()
        );
        existingCoupon.setPerCustomerLimit(
                updatedCoupon.getPerCustomerLimit()
        );
        existingCoupon.setStartAt(
                updatedCoupon.getStartAt()
        );
        existingCoupon.setExpiresAt(
                updatedCoupon.getExpiresAt()
        );

        return couponRepository.save(existingCoupon);
    }

    // =========================
    // ACTIVATE / DEACTIVATE
    // =========================
    @Transactional
    public Coupon toggleCoupon(Long id) {

        Coupon coupon = getCouponById(id);

        coupon.setActive(!Boolean.TRUE.equals(coupon.getActive()));

        return couponRepository.save(coupon);
    }

    // =========================
    // DELETE COUPON
    // =========================
    @Transactional
    public void deleteCoupon(Long id) {

        Coupon coupon = getCouponById(id);

        couponRepository.delete(coupon);
    }


    // =========================
// CUSTOMER: AVAILABLE COUPONS
// =========================
    public List<Coupon> getAvailableCoupons() {

        LocalDateTime now = LocalDateTime.now();

        return couponRepository.findAll()
                .stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .filter(c -> c.getStartAt() == null
                        || !c.getStartAt().isAfter(now))
                .filter(c -> c.getExpiresAt() == null
                        || c.getExpiresAt().isAfter(now))
                .filter(c -> c.getUsageLimit() == null
                        || c.getUsedCount() < c.getUsageLimit())
                .toList();
    }


    // =========================
// CUSTOMER: APPLY COUPON
// =========================
    public java.util.Map<String, Object> applyCoupon(
            String code,
            BigDecimal subtotal
    ) {

        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Please enter a coupon code."
            );
        }

        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Invalid order subtotal."
            );
        }

        Coupon coupon = couponRepository
                .findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid coupon code."
                ));

        LocalDateTime now = LocalDateTime.now();

        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new IllegalArgumentException(
                    "This coupon is currently inactive."
            );
        }

        if (coupon.getStartAt() != null
                && now.isBefore(coupon.getStartAt())) {
            throw new IllegalArgumentException(
                    "This coupon is not active yet."
            );
        }

        if (coupon.getExpiresAt() != null
                && !now.isBefore(coupon.getExpiresAt())) {
            throw new IllegalArgumentException(
                    "This coupon has expired."
            );
        }

        if (coupon.getUsageLimit() != null
                && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new IllegalArgumentException(
                    "This coupon has reached its usage limit."
            );
        }

        if (coupon.getMinimumOrderValue() != null
                && subtotal.compareTo(coupon.getMinimumOrderValue()) < 0) {
            throw new IllegalArgumentException(
                    "Minimum order value for this coupon is ₹"
                            + coupon.getMinimumOrderValue()
            );
        }

        BigDecimal discount;

        if ("PERCENTAGE".equals(coupon.getType().name())) {

            discount = subtotal
                    .multiply(coupon.getDiscountValue())
                    .divide(new BigDecimal("100"), 2,
                            java.math.RoundingMode.HALF_UP);

            if (coupon.getMaximumDiscount() != null) {
                discount = discount.min(coupon.getMaximumDiscount());
            }

        } else if ("FIXED".equals(coupon.getType().name())) {

            discount = coupon.getDiscountValue().min(subtotal);

        } else {
            throw new IllegalArgumentException(
                    "Unsupported coupon type."
            );
        }

        discount = discount.max(BigDecimal.ZERO).min(subtotal);

        BigDecimal finalSubtotal = subtotal.subtract(discount);

        java.util.Map<String, Object> response =
                new java.util.LinkedHashMap<>();

        response.put("code", coupon.getCode());
        response.put("type", coupon.getType().name());
        response.put("discountValue", coupon.getDiscountValue());
        response.put("discountAmount", discount);
        response.put("subtotal", subtotal);
        response.put("finalSubtotal", finalSubtotal);
        response.put("message", "Coupon applied successfully.");

        return response;
    }
}