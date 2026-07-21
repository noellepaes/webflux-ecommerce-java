package com.ecommerce.customer.infrastructure.repository;

import com.ecommerce.customer.domain.model.Customer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface R2dbcCustomerRepository extends ReactiveCrudRepository<Customer, UUID> {
    Mono<Customer> findByEmail(String email);
    Mono<Customer> findByCpf(String cpf);
}
