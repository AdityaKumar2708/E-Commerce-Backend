
package com.rkind.ecommerce.payment.dto.request;

import com.rkind.ecommerce.order.dto.request.CreateOrderRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPaymentRequest {

    @NotBlank
    private String razorpayOrderId;

    @NotBlank
    private String razorpayPaymentId;

    @NotBlank
    private String razorpaySignature;

    @Valid
    @NotNull
    private CreateOrderRequest order;
}
