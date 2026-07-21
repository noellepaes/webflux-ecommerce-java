package com.ecommerce.product.infrastructure.repository;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.model.ProductStatus;
import com.ecommerce.product.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final R2dbcProductRepository delegate;

    @Override
    public Mono<Product> save(Product product) {
        return delegate.save(product);
    }

    @Override
    public Mono<Product> findById(UUID id) {
        return delegate.findById(id);
    }

    @Override
    public Mono<Product> findByName(String name) {
        return delegate.findByName(name);
    }

    @Override
    public Flux<Product> findAll() {
        return delegate.findAll();
    }

    @Override
    public Flux<Product> findByStatus(ProductStatus status) {
        return delegate.findByStatus(status);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return delegate.deleteById(id);
    }
}
