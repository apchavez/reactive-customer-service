package com.apchavez.customers.domain.service;

import com.apchavez.customers.domain.exception.ClienteNoEncontradoException;
import com.apchavez.customers.domain.model.Customer;
import com.apchavez.customers.domain.port.CustomerRepositoryPort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CustomerDomainService {

    private final CustomerRepositoryPort repositoryPort;

    public CustomerDomainService(CustomerRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    public Mono<Customer> createCustomer(Customer customer) {
        return repositoryPort.save(customer);
    }

    public Mono<Customer> findById(Integer id) {
        return repositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new ClienteNoEncontradoException(id)));
    }

    public Flux<Customer> listActiveCustomers(int page, int size) {
        return repositoryPort.findAllActive(page, size);
    }

    public Mono<Long> countActiveCustomers() {
        return repositoryPort.countActive();
    }

    public Mono<Customer> updateCustomer(Integer id, Customer updatedData) {
        return repositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new ClienteNoEncontradoException(id)))
                .flatMap(existing -> repositoryPort.update(
                        new Customer(id, updatedData.nombre(), updatedData.apellido(),
                                updatedData.estado(), updatedData.edad())));
    }

    public Mono<Customer> deleteCustomer(Integer id) {
        return repositoryPort.findById(id)
                .switchIfEmpty(Mono.error(new ClienteNoEncontradoException(id)))
                .flatMap(existing -> repositoryPort.delete(id).thenReturn(existing));
    }
}
