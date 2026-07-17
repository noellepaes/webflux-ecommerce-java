package com.ecommerce.product.application.usecase;

import com.ecommerce.product.application.dto.ProductDTO;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.repository.ProductRepository;
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
        return repository.findByName(productDTO.name())
                .flatMap(existing -> Mono.<ProductDTO>error(new BusinessException("Produto com este nome já existe")))
                .switchIfEmpty(Mono.defer(() -> {
                    Product product = new Product();
                    product.setName(productDTO.name());
                    product.setDescription(productDTO.description());
                    product.setPrice(productDTO.price());
                    product.setStock(productDTO.stock());
                    return repository.save(product).map(ProductDTO::from);
                }));
    }
}
