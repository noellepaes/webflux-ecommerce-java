package com.ecommerce.product.application.usecase;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.infrastructure.repository.ProductRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DecreaseStockUseCase {

    private final ProductRepository repository;

    @Transactional
    public Mono<ProductDTO> execute(UUID id, Integer quantity) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("Produto não encontrado")))
                .flatMap(product -> {
                    product.decreaseStock(quantity);
                    return repository.save(product);
                })
                .map(ProductDTO::from);
    }
}
