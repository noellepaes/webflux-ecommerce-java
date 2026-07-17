package com.ecommerce.customer.domain.repository;

import com.ecommerce.customer.domain.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface CustomerRepository {
    Mono<Customer> save(Customer customer);
    Mono<Customer> findById(UUID id);
    Mono<Customer> findByEmail(String email);
    Mono<Customer> findByCpf(String cpf);
    Flux<Customer> findAll();
    Mono<Void> deleteById(UUID id);
}
