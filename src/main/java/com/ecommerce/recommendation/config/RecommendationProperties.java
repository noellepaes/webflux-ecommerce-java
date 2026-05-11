package com.ecommerce.recommendation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.recommendation")
public class RecommendationProperties {

    /**
     * Tempo até a chave Redis do histórico de escolhas do cliente expirar.
     * A cada nova escolha o TTL é renovado para este valor (janela deslizante).
     */
    private Duration customerHistoryTtl = Duration.ofDays(30);

    /**
     * Quantidade máxima de produtos sugeridos por resposta.
     */
    private int suggestionLimit = 5;
}
