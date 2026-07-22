package com.ecommerce.product.application.usecase;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.infrastructure.repository.ProductRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/** Reads reativos (R2DBC). Sem {@code @Transactional} no Flux/Mono de leitura. */
@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductRepository repository;

    public Mono<ProductDTO> findById(UUID id) {
        return repository.findById(id)
                .map(ProductDTO::from)
                .switchIfEmpty(Mono.error(new BusinessException("Produto não encontrado")));
    }

    public Flux<ProductDTO> findAll() {
        return repository.findAll().map(ProductDTO::from);
    }
}
