package com.ecommerce.product.domain.repository;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.model.ProductStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

public interface ProductRepository {
    Mono<Product> save(Product product);
    Mono<Product> findById(UUID id);
    Flux<Product> findByIdIn(Collection<UUID> ids);
    Mono<Product> findByName(String name);
    Flux<Product> findAll();
    Flux<Product> findByStatus(ProductStatus status);
    Mono<Void> deleteById(UUID id);
}
