
package com.rkind.ecommerce.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReturnExchangeRequestDto(

        @NotBlank
        String requestType,

        @NotBlank
        String reason,

        @Size(max = 2000)
        String customerDescription

) {}
