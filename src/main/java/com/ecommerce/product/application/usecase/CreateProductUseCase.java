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
                .filter(p -> p.getName().equals(productDTO.name()))
                .hasElements()
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new BusinessException("Produto com este nome já existe"));
                    }
                    Product product = new Product();
                    product.setName(productDTO.name());
                    product.setDescription(productDTO.description());
                    product.setPrice(productDTO.price());
                    product.setStock(productDTO.stock());
                    return repository.save(product).map(ProductDTO::from);
                });
    }
}
