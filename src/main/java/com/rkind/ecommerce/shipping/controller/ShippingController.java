package com.rkind.ecommerce.shipping.controller;

import com.rkind.ecommerce.shipping.dto.request.ShippingQuoteRequest;
import com.rkind.ecommerce.shipping.dto.response.ShippingQuoteResponse;
import com.rkind.ecommerce.shipping.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/quote")
    public ResponseEntity<ShippingQuoteResponse> getShippingQuote(
            @Valid @RequestBody ShippingQuoteRequest request
    ) {

        ShippingQuoteResponse response =
                shippingService.getShippingQuote(request);

        return ResponseEntity.ok(response);
    }
}