package com.rkind.ecommerce.shipping.dto.response;

import java.math.BigDecimal;

public class CheckoutQuoteResponse {

    private BigDecimal productTotal;

    private BigDecimal discount;

    private BigDecimal shippingCharge;

    private BigDecimal finalAmount;

    private boolean freeDelivery;

    private String courierName;

    private BigDecimal chargeWeight;

    private String estimatedDeliveryDays;

    private int totalItems;

    public BigDecimal getProductTotal() {
        return productTotal;
    }

    public void setProductTotal(BigDecimal productTotal) {
        this.productTotal = productTotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public BigDecimal getShippingCharge() {
        return shippingCharge;
    }

    public void setShippingCharge(BigDecimal shippingCharge) {
        this.shippingCharge = shippingCharge;
    }

    public BigDecimal getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(BigDecimal finalAmount) {
        this.finalAmount = finalAmount;
    }

    public boolean isFreeDelivery() {
        return freeDelivery;
    }

    public void setFreeDelivery(boolean freeDelivery) {
        this.freeDelivery = freeDelivery;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public BigDecimal getChargeWeight() {
        return chargeWeight;
    }

    public void setChargeWeight(BigDecimal chargeWeight) {
        this.chargeWeight = chargeWeight;
    }

    public String getEstimatedDeliveryDays() {
        return estimatedDeliveryDays;
    }

    public void setEstimatedDeliveryDays(String estimatedDeliveryDays) {
        this.estimatedDeliveryDays = estimatedDeliveryDays;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }
}