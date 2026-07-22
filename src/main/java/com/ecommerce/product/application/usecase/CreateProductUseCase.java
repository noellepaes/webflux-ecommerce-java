package com.ecommerce.product.application.usecase;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.infrastructure.repository.ProductRepository;
import com.ecommerce.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository repository;

    @Transactional
    public Mono<ProductDTO> execute(ProductDTO productDTO) {
        return repository.findAll()
                .any(p -> p.getName().equals(productDTO.name()))
                .flatMap(exists -> Boolean.TRUE.equals(exists)
                        ? Mono.error(new BusinessException("Produto com este nome já existe"))
                        : Mono.just(productDTO))
                .map(CreateProductUseCase::toEntity)
                .flatMap(repository::save)
                .map(ProductDTO::from);
    }

    private static Product toEntity(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setStock(dto.stock());
        return product;
    }
}
