package com.rkind.ecommerce.shipping.dto.response;

import java.math.BigDecimal;

public class ShippingQuoteResponse {

    private BigDecimal shippingCharge;

    private String courierName;

    private BigDecimal chargeWeight;

    private String estimatedDeliveryDays;

    public BigDecimal getShippingCharge() {
        return shippingCharge;
    }

    public void setShippingCharge(BigDecimal shippingCharge) {
        this.shippingCharge = shippingCharge;
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
}