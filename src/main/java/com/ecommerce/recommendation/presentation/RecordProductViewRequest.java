package com.ecommerce.recommendation.presentation;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecordProductViewRequest(@NotNull UUID productId) {
}
