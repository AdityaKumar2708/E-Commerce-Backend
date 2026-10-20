package com.rkind.ecommerce.coupon.controller;

import com.rkind.ecommerce.coupon.entity.Coupon;
import com.rkind.ecommerce.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // =========================
    // CREATE COUPON
    // =========================
    @PostMapping
    public ResponseEntity<Coupon> createCoupon(
            @RequestBody Coupon coupon
    ) {

        Coupon createdCoupon =
                couponService.createCoupon(coupon);

        return ResponseEntity.ok(createdCoupon);
    }

    // =========================
    // GET ALL COUPONS
    // =========================
    @GetMapping
    public ResponseEntity<List<Coupon>> getAllCoupons() {

        return ResponseEntity.ok(
                couponService.getAllCoupons()
        );
    }

    // =========================
    // GET COUPON BY ID
    // =========================
    @GetMapping("/{id}")
    public ResponseEntity<Coupon> getCouponById(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                couponService.getCouponById(id)
        );
    }

    // =========================
    // UPDATE COUPON
    // =========================
    @PutMapping("/{id}")
    public ResponseEntity<Coupon> updateCoupon(
            @PathVariable Long id,
            @RequestBody Coupon coupon
    ) {

        return ResponseEntity.ok(
                couponService.updateCoupon(id, coupon)
        );
    }

    // =========================
    // ACTIVATE / DEACTIVATE
    // =========================
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Coupon> toggleCoupon(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                couponService.toggleCoupon(id)
        );
    }

    // =========================
    // DELETE COUPON
    // =========================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCoupon(
            @PathVariable Long id
    ) {

        couponService.deleteCoupon(id);

        return ResponseEntity.noContent().build();
    }
}