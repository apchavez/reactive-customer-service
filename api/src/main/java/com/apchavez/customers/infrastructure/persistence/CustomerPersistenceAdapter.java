package com.apchavez.customers.infrastructure.persistence;

import com.apchavez.customers.domain.exception.ClienteNoEncontradoException;
import com.apchavez.customers.domain.model.Customer;
import com.apchavez.customers.domain.model.CustomerState;
import com.apchavez.customers.domain.port.CustomerRepositoryPort;
import com.apchavez.customers.infrastructure.mapper.CustomerMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final CustomerR2dbcRepository r2dbcRepository;
    private final R2dbcEntityTemplate r2dbcTemplate;
    private final CustomerMapper mapper;

    public CustomerPersistenceAdapter(CustomerR2dbcRepository r2dbcRepository,
                                       R2dbcEntityTemplate r2dbcTemplate,
                                       CustomerMapper mapper) {
        this.r2dbcRepository = r2dbcRepository;
        this.r2dbcTemplate = r2dbcTemplate;
        this.mapper = mapper;
    }

    @Override
    public Mono<Customer> save(Customer customer) {
        return r2dbcRepository.save(mapper.toEntity(customer))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Customer> update(Customer customer) {
        return r2dbcTemplate.update(mapper.toEntity(customer))
                .map(mapper::toDomain)
                .switchIfEmpty(Mono.error(new ClienteNoEncontradoException(customer.id())));
    }

    @Override
    public Mono<Customer> findById(Integer id) {
        return r2dbcRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Flux<Customer> findAllActive(int page, int size) {
        return r2dbcRepository.findAllByEstado(CustomerState.ACTIVE.name(), PageRequest.of(page, size))
                .map(mapper::toDomain);
    }

    @Override
    public Mono<Long> countActive() {
        return r2dbcRepository.countByEstado(CustomerState.ACTIVE.name());
    }

    @Override
    public Mono<Void> delete(Integer id) {
        return r2dbcRepository.deleteById(id);
    }
}
