package com.rkind.ecommerce.shipping.service;

import com.rkind.ecommerce.product.entity.Product;
import com.rkind.ecommerce.product.entity.SalesChannel;
import com.rkind.ecommerce.product.repository.ProductRepository;
import com.rkind.ecommerce.shipping.dto.request.CheckoutQuoteRequest;
import com.rkind.ecommerce.shipping.dto.response.CheckoutQuoteResponse;
import com.rkind.ecommerce.shipping.dto.response.ShiprocketRateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckoutQuoteService {

    private final ProductRepository productRepository;
    private final ShiprocketService shiprocketService;

    public CheckoutQuoteResponse calculateQuote(
            CheckoutQuoteRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Checkout quote request is required."
            );
        }

        if (request.getItems() == null
                || request.getItems().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one product is required."
            );
        }

        BigDecimal productTotal = BigDecimal.ZERO;

        BigDecimal totalWeight = BigDecimal.ZERO;

        BigDecimal maxLength = BigDecimal.ZERO;
        BigDecimal maxWidth = BigDecimal.ZERO;
        BigDecimal totalHeight = BigDecimal.ZERO;

        BigDecimal declaredValue = BigDecimal.ZERO;

        int totalItems = 0;

        for (CheckoutQuoteRequest.Item item
                : request.getItems()) {

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
                        "Product is currently unavailable: "
                                + product.getName()
                );
            }

            if (product.getSalesChannel()
                    != SalesChannel.ONLINE) {

                throw new IllegalArgumentException(
                        "Store pickup products cannot be shipped: "
                                + product.getName()
                );
            }

            if (product.getPrice() == null
                    || product.getPrice()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Invalid product price: "
                                + product.getName()
                );
            }

            if (product.getWeight() == null
                    || product.getWeight()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Shipping weight is missing for: "
                                + product.getName()
                );
            }

            BigDecimal quantity =
                    BigDecimal.valueOf(item.getQuantity());

            BigDecimal itemTotal =
                    product.getPrice().multiply(quantity);

            productTotal =
                    productTotal.add(itemTotal);

            totalItems += item.getQuantity();

            totalWeight =
                    totalWeight.add(
                            product.getWeight()
                                    .multiply(quantity)
                    );

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
                totalHeight =
                        totalHeight.add(
                                product.getHeight()
                                        .multiply(quantity)
                        );
            }

            declaredValue =
                    declaredValue.add(itemTotal);
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
                    "Valid shipping dimensions are required."
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

        BigDecimal shippingCharge =
                courier.getRate();

        /*
         * Free-delivery eligibility will be finalized
         * with the 20% minimum-margin calculation.
         *
         * For now, do NOT give free delivery automatically.
         */
        boolean freeDelivery = false;

        BigDecimal finalShippingCharge =
                shippingCharge;

        BigDecimal discount = BigDecimal.ZERO;

        BigDecimal finalAmount =
                productTotal
                        .subtract(discount)
                        .add(finalShippingCharge);

        CheckoutQuoteResponse response =
                new CheckoutQuoteResponse();

        response.setProductTotal(productTotal);
        response.setDiscount(discount);
        response.setShippingCharge(finalShippingCharge);
        response.setFinalAmount(finalAmount);
        response.setFreeDelivery(freeDelivery);

        response.setCourierName(
                courier.getCourier_name()
        );

        response.setChargeWeight(
                courier.getCharge_weight()
        );

        response.setEstimatedDeliveryDays(
                courier.getEstimated_delivery_days()
        );

        response.setTotalItems(totalItems);

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