package com.rkind.ecommerce.payment.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CreatePaymentOrderRequest {

    @NotNull
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}