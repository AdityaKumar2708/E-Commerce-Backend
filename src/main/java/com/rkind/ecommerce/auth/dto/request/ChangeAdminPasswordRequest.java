package com.rkind.ecommerce.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeAdminPasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {}