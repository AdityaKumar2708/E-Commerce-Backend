package com.rkind.ecommerce.coupon.controller;

import com.rkind.ecommerce.coupon.entity.Coupon;
import com.rkind.ecommerce.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
@CrossOrigin
public class CustomerCouponController {

    private final CouponService couponService;

    @GetMapping("/available")
    public ResponseEntity<List<Coupon>> getAvailableCoupons() {
        return ResponseEntity.ok(
                couponService.getAvailableCoupons()
        );
    }

    @GetMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal subtotal
    ) {
        return ResponseEntity.ok(
                couponService.applyCoupon(code, subtotal)
        );
    }
}