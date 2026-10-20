
package com.rkind.ecommerce.payment.service;

import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Refund;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RazorpayRefundService {

    private final RazorpayClient razorpayClient;

    public RazorpayRefundService(RazorpayClient razorpayClient) {
        this.razorpayClient = razorpayClient;
    }

    /**
     * Initiates a refund against an existing Razorpay payment.
     *
     * Call this only after the return workflow has confirmed
     * that the parcel has been picked up and the refund is approved.
     *
     * amount is in rupees; Razorpay expects paise.
     */
    public Map<String, Object> initiateRefund(
            String razorpayPaymentId,
            BigDecimal amount
    ) throws Exception {

        if (razorpayPaymentId == null
                || razorpayPaymentId.isBlank()) {
            throw new IllegalArgumentException(
                    "Razorpay payment ID is required."
            );
        }

        if (amount == null
                || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Refund amount must be greater than zero."
            );
        }

        int amountInPaise = amount
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();

        // Always fetch the payment directly from Razorpay.
        Payment payment =
                razorpayClient.payments.fetch(razorpayPaymentId);

        String paymentStatus = payment.get("status");
        Object rawAmount = payment.get("amount");
        Object rawAmountRefunded = payment.get("amount_refunded");

        if (!"captured".equalsIgnoreCase(paymentStatus)
                && !"partially_refunded".equalsIgnoreCase(paymentStatus)) {
            throw new IllegalStateException(
                    "Only captured or partially refunded payments can be refunded."
            );
        }

        if (!(rawAmount instanceof Number)) {
            throw new IllegalStateException(
                    "Razorpay did not return a valid payment amount."
            );
        }

        int originalAmount = ((Number) rawAmount).intValue();

        int alreadyRefunded = rawAmountRefunded instanceof Number
                ? ((Number) rawAmountRefunded).intValue()
                : 0;

        int remainingRefundable = originalAmount - alreadyRefunded;

        if (amountInPaise > remainingRefundable) {
            throw new IllegalArgumentException(
                    "Refund amount exceeds the remaining refundable balance."
            );
        }

        JSONObject refundRequest = new JSONObject();
        refundRequest.put("amount", amountInPaise);

        Refund refund = razorpayClient.payments.refund(
                razorpayPaymentId,
                refundRequest
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("refundId", refund.get("id"));
        result.put("paymentId", refund.get("payment_id"));
        result.put("amountInPaise", refund.get("amount"));
        result.put("currency", refund.get("currency"));
        result.put("status", refund.get("status"));

        return result;
    }
}
