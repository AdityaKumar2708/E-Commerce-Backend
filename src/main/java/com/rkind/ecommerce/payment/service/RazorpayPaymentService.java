package com.rkind.ecommerce.payment.service;

import com.rkind.ecommerce.payment.dto.PaymentOrderResponse;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.razorpay.Payment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class RazorpayPaymentService {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    public RazorpayPaymentService(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    public PaymentOrderResponse createOrder(BigDecimal amount) throws Exception {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Payment amount must be greater than zero.");
        }

        // Razorpay expects amount in paise
        int amountInPaise = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();

        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "RKind_" + System.currentTimeMillis());

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        return new PaymentOrderResponse(
                razorpayOrder.get("id"),
                "INR",
                razorpayKeyId,
                amountInPaise
        );
    }


    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    public int verifyPayment(
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature
    ) throws Exception {

        if (razorpayOrderId == null || razorpayOrderId.isBlank()
                || razorpayPaymentId == null || razorpayPaymentId.isBlank()
                || razorpaySignature == null || razorpaySignature.isBlank()) {
            throw new IllegalArgumentException("Payment details are incomplete.");
        }

        String payload = razorpayOrderId + "|" + razorpayPaymentId;

        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(
                razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        mac.init(secretKey);

        byte[] digest = mac.doFinal(
                payload.getBytes(StandardCharsets.UTF_8)
        );

        StringBuilder expectedSignature = new StringBuilder();
        for (byte b : digest) {
            expectedSignature.append(String.format("%02x", b & 0xff));
        }

        boolean signatureValid = MessageDigest.isEqual(
                expectedSignature.toString().getBytes(StandardCharsets.UTF_8),
                razorpaySignature.getBytes(StandardCharsets.UTF_8)
        );

        if (!signatureValid) {
            throw new IllegalArgumentException("Invalid Razorpay payment signature.");
        }

        // Fetch payment details from Razorpay, not from the browser.
        Payment payment = razorpayClient.payments.fetch(razorpayPaymentId);

        String actualOrderId = payment.get("order_id");
        String status = payment.get("status");
        String currency = payment.get("currency");
        Object rawAmount = payment.get("amount");

        if (!razorpayOrderId.equals(actualOrderId)) {
            throw new IllegalArgumentException(
                    "Payment does not belong to this Razorpay order."
            );
        }

        if (!"captured".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException(
                    "Payment has not been captured."
            );
        }

        if (!"INR".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Unexpected payment currency.");
        }

        if (!(rawAmount instanceof Number)) {
            throw new IllegalArgumentException("Invalid payment amount from Razorpay.");
        }

        return ((Number) rawAmount).intValue();
    }

}