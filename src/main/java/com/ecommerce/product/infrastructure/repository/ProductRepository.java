package com.ecommerce.product.infrastructure.repository;

import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.domain.model.ProductStatus;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.UUID;

/**
 * Repositório R2DBC direto (estilo artigo): sem adapter / porta de domínio.
 * {@code findBy*} são queries derivadas do Spring Data — sem overrides manuais.
 */
@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, UUID> {

    Mono<Product> findByName(String name);

    Flux<Product> findByStatus(ProductStatus status);

    Flux<Product> findByIdIn(Collection<UUID> ids);
}
