package com.rkind.ecommerce.order.entity;

public enum OrderStatus {

    PENDING,
    CONFIRMED,
    PROCESSING,
    PACKED,
    SHIPPED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    READY_FOR_PICKUP,
    PICKED_UP,
    CANCELLED
}