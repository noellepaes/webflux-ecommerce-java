package com.ecommerce.product.application.usecase;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.infrastructure.repository.ProductRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {

    private final ProductRepository repository;

    @Transactional
    public Mono<ProductDTO> execute(UUID id, ProductDTO productDTO) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new BusinessException("Produto não encontrado")))
                .map(product -> apply(product, productDTO))
                .flatMap(repository::save)
                .map(ProductDTO::from);
    }

    private static Product apply(Product product, ProductDTO dto) {
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        return product;
    }
}
