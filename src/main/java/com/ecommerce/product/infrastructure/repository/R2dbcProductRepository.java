package com.ecommerce.product.infrastructure.repository;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.model.ProductStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface R2dbcProductRepository extends ReactiveCrudRepository<Product, UUID> {
    Mono<Product> findByName(String name);
    Flux<Product> findByStatus(ProductStatus status);
}
