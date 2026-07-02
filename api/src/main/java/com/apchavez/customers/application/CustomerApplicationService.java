package com.apchavez.customers.application;

import com.apchavez.customers.domain.event.CustomerEvent;
import com.apchavez.customers.domain.model.Customer;
import com.apchavez.customers.domain.port.CustomerEventPublisherPort;
import com.apchavez.customers.domain.service.CustomerDomainService;
import com.apchavez.customers.infrastructure.config.RequestLoggingFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static com.apchavez.customers.domain.event.CustomerEventType.*;

@Service
public class CustomerApplicationService {

    private static final Logger log = LoggerFactory.getLogger(CustomerApplicationService.class);

    private final CustomerDomainService domainService;
    private final CustomerEventPublisherPort eventPublisher;

    public CustomerApplicationService(CustomerDomainService domainService,
                                       CustomerEventPublisherPort eventPublisher) {
        this.domainService = domainService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Mono<Customer> createCustomer(Customer customer) {
        return Mono.deferContextual(ctx -> {
            String rid = ctx.getOrDefault(RequestLoggingFilter.REQUEST_ID_CONTEXT_KEY, "-");
            log.info("[{}] Crear cliente — nombre='{}', apellido='{}'",
                    rid, customer.nombre(), customer.apellido());
            return domainService.createCustomer(customer)
                    .flatMap(saved -> eventPublisher.publish(CustomerEvent.of(CUSTOMER_CREATED, saved))
                            .thenReturn(saved))
                    .doOnSuccess(saved -> log.info("[{}] Cliente creado — id={}", rid, saved.id()));
        });
    }

    public Mono<Customer> findById(Integer id) {
        return Mono.deferContextual(ctx -> {
            log.debug("[{}] Buscar cliente — id={}",
                    ctx.getOrDefault(RequestLoggingFilter.REQUEST_ID_CONTEXT_KEY, "-"), id);
            return domainService.findById(id);
        });
    }

    public Flux<Customer> listActiveCustomers(int page, int size) {
        log.debug("Listar clientes activos — página={}, tamaño={}", page, size);
        return domainService.listActiveCustomers(page, size);
    }

    public Mono<Long> countActiveCustomers() {
        return domainService.countActiveCustomers();
    }

    @Transactional
    public Mono<Customer> updateCustomer(Integer id, Customer updatedData) {
        return Mono.deferContextual(ctx -> {
            String rid = ctx.getOrDefault(RequestLoggingFilter.REQUEST_ID_CONTEXT_KEY, "-");
            log.info("[{}] Actualizar cliente — id={}", rid, id);
            return domainService.updateCustomer(id, updatedData)
                    .flatMap(updated -> eventPublisher.publish(CustomerEvent.of(CUSTOMER_UPDATED, updated))
                            .thenReturn(updated))
                    .doOnSuccess(updated -> log.info("[{}] Cliente actualizado — id={}", rid, updated.id()));
        });
    }

    @Transactional
    public Mono<Void> deleteCustomer(Integer id) {
        return Mono.deferContextual(ctx -> {
            String rid = ctx.getOrDefault(RequestLoggingFilter.REQUEST_ID_CONTEXT_KEY, "-");
            log.info("[{}] Eliminar cliente — id={}", rid, id);
            return domainService.deleteCustomer(id)
                    .flatMap(deleted -> eventPublisher.publish(CustomerEvent.of(CUSTOMER_DELETED, deleted)))
                    .doOnSuccess(v -> log.info("[{}] Cliente eliminado — id={}", rid, id));
        });
    }
}
