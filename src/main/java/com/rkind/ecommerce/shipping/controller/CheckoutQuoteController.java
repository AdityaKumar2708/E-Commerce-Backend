package com.rkind.ecommerce.shipping.controller;

import com.rkind.ecommerce.shipping.dto.request.CheckoutQuoteRequest;
import com.rkind.ecommerce.shipping.dto.response.CheckoutQuoteResponse;
import com.rkind.ecommerce.shipping.service.CheckoutQuoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
public class CheckoutQuoteController {

    private final CheckoutQuoteService checkoutQuoteService;

    @PostMapping("/quote")
    public ResponseEntity<CheckoutQuoteResponse> calculateQuote(
            @Valid @RequestBody CheckoutQuoteRequest request
    ) {

        CheckoutQuoteResponse response =
                checkoutQuoteService.calculateQuote(request);

        return ResponseEntity.ok(response);
    }
}