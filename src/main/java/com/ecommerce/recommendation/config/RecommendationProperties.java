package com.ecommerce.recommendation.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.recommendation", ignoreUnknownFields = false)
public record RecommendationProperties(

        /**
         * Tempo até a chave Redis do histórico expirar.
         * A cada nova visualização o TTL é renovado (janela deslizante).
         * Obrigatório em application.yml — sem default na classe.
         */
        @NotNull Duration customerHistoryTtl,

        /**
         * Quantidade máxima de produtos sugeridos por resposta.
         * Obrigatório em application.yml — sem default na classe.
         */
        @Positive int suggestionLimit
) {
}
