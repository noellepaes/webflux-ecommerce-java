package com.ecommerce.product.application.usecase;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.domain.repository.ProductRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetProductUseCase {

    private final ProductRepository repository;

    @Transactional(readOnly = true)
    public Mono<ProductDTO> findById(UUID id) {
        return repository.findById(id)
                .map(ProductDTO::from)
                .switchIfEmpty(Mono.error(new BusinessException("Produto não encontrado")));
    }

    @Transactional(readOnly = true)
    public Flux<ProductDTO> findAll() {
        return repository.findAll().map(ProductDTO::from);
    }
}
