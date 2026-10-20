package com.rkind.ecommerce.shipping.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class ShippingQuoteRequest {

    @NotBlank(message = "Delivery pincode is required")
    private String deliveryPincode;

    @NotEmpty(message = "At least one product is required")
    private List<Item> items;

    private boolean cod = false;

    public String getDeliveryPincode() {
        return deliveryPincode;
    }

    public void setDeliveryPincode(String deliveryPincode) {
        this.deliveryPincode = deliveryPincode;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public boolean isCod() {
        return cod;
    }

    public void setCod(boolean cod) {
        this.cod = cod;
    }

    public static class Item {

        private Long productId;

        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}