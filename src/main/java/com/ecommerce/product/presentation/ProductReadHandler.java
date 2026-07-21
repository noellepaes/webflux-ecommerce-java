package com.ecommerce.product.presentation;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.application.usecase.GetProductUseCase;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler funcional dos GETs de produto.
 * <ul>
 *   <li>Ponto 1 — sem {@code @RestController} no hot path</li>
 *   <li>Ponto 2 — só R2DBC via use case (sem JDBC/block)</li>
 *   <li>Ponto 3 — {@code list} faz stream do {@code Flux} com {@code limitRate}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ProductReadHandler {

    private final GetProductUseCase getProductUseCase;

    public Mono<ServerResponse> getById(ServerRequest request) {
        UUID id;
        try {
            id = UUID.fromString(request.pathVariable("id"));
        } catch (IllegalArgumentException ex) {
            return ServerResponse.badRequest().build();
        }

        return getProductUseCase.findById(id)
                .flatMap(product -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(product))
                .onErrorResume(BusinessException.class, ex -> ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        // JSON array com streaming + contrapressão (limitRate), compatível com k6/clients atuais
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(getProductUseCase.findAll().limitRate(256), ProductDTO.class);
    }
}
