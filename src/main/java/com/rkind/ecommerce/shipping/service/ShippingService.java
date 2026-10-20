package com.rkind.ecommerce.shipping.service;

import com.rkind.ecommerce.product.entity.Product;
import com.rkind.ecommerce.product.entity.SalesChannel;
import com.rkind.ecommerce.product.repository.ProductRepository;
import com.rkind.ecommerce.shipping.dto.request.ShippingQuoteRequest;
import com.rkind.ecommerce.shipping.dto.response.ShippingQuoteResponse;
import com.rkind.ecommerce.shipping.dto.response.ShiprocketRateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShippingService {

    private final ProductRepository productRepository;
    private final ShiprocketService shiprocketService;

    public ShippingQuoteResponse getShippingQuote(
            ShippingQuoteRequest request
    ) {

        if (request.getItems() == null
                || request.getItems().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one product is required."
            );
        }

        BigDecimal totalWeight = BigDecimal.ZERO;

        BigDecimal maxLength = BigDecimal.ZERO;
        BigDecimal maxWidth = BigDecimal.ZERO;
        BigDecimal totalHeight = BigDecimal.ZERO;

        BigDecimal declaredValue = BigDecimal.ZERO;

        for (ShippingQuoteRequest.Item item : request.getItems()) {

            if (item.getProductId() == null) {
                throw new IllegalArgumentException(
                        "Product ID is required."
                );
            }

            if (item.getQuantity() == null
                    || item.getQuantity() <= 0) {

                throw new IllegalArgumentException(
                        "Product quantity must be greater than zero."
                );
            }

            Product product = productRepository
                    .findById(item.getProductId())
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Product not found: "
                                            + item.getProductId()
                            )
                    );

            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "Product is inactive: "
                                + product.getName()
                );
            }

            if (product.getSalesChannel() != SalesChannel.ONLINE) {
                throw new IllegalArgumentException(
                        "Store pickup products cannot be shipped: "
                                + product.getName()
                );
            }

            if (product.getWeight() == null
                    || product.getWeight()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Shipping weight is missing for product: "
                                + product.getName()
                );
            }

            BigDecimal quantity =
                    BigDecimal.valueOf(item.getQuantity());

            // Total weight of all units
            totalWeight = totalWeight.add(
                    product.getWeight().multiply(quantity)
            );

            /*
             * Package dimensions:
             *
             * For the current implementation we use:
             * - largest length
             * - largest width
             * - accumulated height
             *
             * This gives us a practical package estimate for
             * multiple products.
             */
            if (product.getLength() != null) {
                maxLength = max(
                        maxLength,
                        product.getLength()
                );
            }

            if (product.getWidth() != null) {
                maxWidth = max(
                        maxWidth,
                        product.getWidth()
                );
            }

            if (product.getHeight() != null) {
                totalHeight = totalHeight.add(
                        product.getHeight().multiply(quantity)
                );
            }

            // Used as declared shipment value
            if (product.getPrice() != null) {

                declaredValue = declaredValue.add(
                        product.getPrice().multiply(quantity)
                );
            }
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Total shipment weight must be greater than zero."
            );
        }

        if (maxLength.compareTo(BigDecimal.ZERO) <= 0
                || maxWidth.compareTo(BigDecimal.ZERO) <= 0
                || totalHeight.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Valid shipping dimensions are required for all online products."
            );
        }

        ShiprocketRateResponse.CourierCompany courier =
                shiprocketService.getShippingRate(
                        request.getDeliveryPincode(),
                        totalWeight,
                        maxLength,
                        maxWidth,
                        totalHeight,
                        request.isCod(),
                        declaredValue
                );

        ShippingQuoteResponse response =
                new ShippingQuoteResponse();

        response.setShippingCharge(
                courier.getRate()
        );

        response.setCourierName(
                courier.getCourier_name()
        );

        response.setChargeWeight(
                courier.getCharge_weight()
        );

        response.setEstimatedDeliveryDays(
                courier.getEstimated_delivery_days()
        );

        return response;
    }

    private BigDecimal max(
            BigDecimal first,
            BigDecimal second
    ) {

        return first.compareTo(second) >= 0
                ? first
                : second;
    }
}