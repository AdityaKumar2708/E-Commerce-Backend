package com.rkind.ecommerce.payment.dto;

public class PaymentOrderResponse {

    private String orderId;
    private String currency;
    private String keyId;
    private Integer amount;

    public PaymentOrderResponse() {
    }

    public PaymentOrderResponse(
            String orderId,
            String currency,
            String keyId,
            Integer amount
    ) {
        this.orderId = orderId;
        this.currency = currency;
        this.keyId = keyId;
        this.amount = amount;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Integer getAmount() {
        return amount;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}