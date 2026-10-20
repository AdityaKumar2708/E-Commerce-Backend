package com.rkind.ecommerce.auth.dto.response;

import com.rkind.ecommerce.auth.entity.Role;

public record AuthResponse(

        String token,

        String tokenType,

        Long userId,

        String fullName,

        String email,

        Role role

) {}
