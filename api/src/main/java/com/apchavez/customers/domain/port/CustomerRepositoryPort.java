package com.apchavez.customers.domain.port;

import com.apchavez.customers.domain.model.Customer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CustomerRepositoryPort {
    Mono<Customer> save(Customer customer);
    Mono<Customer> update(Customer customer);
    Mono<Customer> findById(Integer id);
    Flux<Customer> findAllActive(int page, int size);
    Mono<Long> countActive();
    Mono<Void> delete(Integer id);
}
