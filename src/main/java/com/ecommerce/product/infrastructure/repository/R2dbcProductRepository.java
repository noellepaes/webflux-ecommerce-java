package com.ecommerce.product.infrastructure.repository;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.model.ProductStatus;
import com.ecommerce.product.domain.repository.ProductRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface R2dbcProductRepository extends ReactiveCrudRepository<Product, UUID>, ProductRepository {

    @Override
    Mono<Product> findByName(String name);

    @Override
    Flux<Product> findByStatus(ProductStatus status);
}
