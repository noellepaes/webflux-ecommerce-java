package com.ecommerce.product.infrastructure.repository;

import com.ecommerce.product.domain.model.Product;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * R2DBC puro (estilo artigo): só {@link ReactiveCrudRepository}.
 * Filtros ({@code name}, {@code status}, batch) ficam no service com {@code Mono}/{@code Flux}.
 */
@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, UUID> {
}
