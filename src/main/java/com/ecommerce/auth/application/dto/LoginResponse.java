package com.ecommerce.auth.application.dto;

import java.util.UUID;

public record LoginResponse(
        UUID customerId,
        String name,
        String email
) {
}
