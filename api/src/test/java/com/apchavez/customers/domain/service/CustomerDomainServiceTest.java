package com.apchavez.customers.domain.service;

import com.apchavez.customers.domain.exception.ClienteNoEncontradoException;
import com.apchavez.customers.domain.model.Customer;
import com.apchavez.customers.domain.model.CustomerState;
import com.apchavez.customers.domain.port.CustomerRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerDomainServiceTest {

    @Mock
    private CustomerRepositoryPort repositoryPort;

    private CustomerDomainService domainService;

    private static final Customer CUSTOMER_WITHOUT_ID =
            new Customer(null, "Alex", "Prieto", CustomerState.ACTIVE, 30);
    private static final Customer SAVED_CUSTOMER =
            new Customer(1, "Alex", "Prieto", CustomerState.ACTIVE, 30);

    @BeforeEach
    void setUp() {
        domainService = new CustomerDomainService(repositoryPort);
    }

    // ── createCustomer ───────────────────────────────────────────────────────

    @Test
    void createCustomer_shouldDelegateToSave() {
        when(repositoryPort.save(any())).thenReturn(Mono.just(SAVED_CUSTOMER));

        StepVerifier.create(domainService.createCustomer(CUSTOMER_WITHOUT_ID))
                .expectNext(SAVED_CUSTOMER)
                .verifyComplete();

        verify(repositoryPort).save(CUSTOMER_WITHOUT_ID);
        verify(repositoryPort, never()).findById(any());
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_shouldReturnCustomer_whenExists() {
        when(repositoryPort.findById(1)).thenReturn(Mono.just(SAVED_CUSTOMER));

        StepVerifier.create(domainService.findById(1))
                .expectNext(SAVED_CUSTOMER)
                .verifyComplete();
    }

    @Test
    void findById_shouldThrowClienteNoEncontradoException_whenNotExists() {
        when(repositoryPort.findById(99)).thenReturn(Mono.empty());

        StepVerifier.create(domainService.findById(99))
                .expectErrorMatches(e -> e instanceof ClienteNoEncontradoException
                        && e.getMessage().contains("99"))
                .verify();
    }

    // ── listActiveCustomers ──────────────────────────────────────────────────

    @Test
    void listActiveCustomers_shouldDelegateToRepositoryPort() {
        Customer active1 = new Customer(1, "Carlos", "Lopez", CustomerState.ACTIVE, 22);
        Customer active2 = new Customer(3, "Ana", "Diaz", CustomerState.ACTIVE, 30);
        when(repositoryPort.findAllActive(0, 10)).thenReturn(Flux.just(active1, active2));

        StepVerifier.create(domainService.listActiveCustomers(0, 10))
                .expectNext(active1)
                .expectNext(active2)
                .verifyComplete();

        verify(repositoryPort).findAllActive(0, 10);
    }

    // ── updateCustomer ───────────────────────────────────────────────────────

    @Test
    void updateCustomer_shouldReturnUpdatedCustomer_whenExists() {
        Customer updatedData = new Customer(null, "Alexander", "Prieto Chavez", CustomerState.INACTIVE, 31);
        Customer expectedResult = new Customer(1, "Alexander", "Prieto Chavez", CustomerState.INACTIVE, 31);

        when(repositoryPort.findById(1)).thenReturn(Mono.just(SAVED_CUSTOMER));
        when(repositoryPort.update(any())).thenReturn(Mono.just(expectedResult));

        StepVerifier.create(domainService.updateCustomer(1, updatedData))
                .expectNextMatches(c -> c.nombre().equals("Alexander")
                        && c.apellido().equals("Prieto Chavez")
                        && c.estado() == CustomerState.INACTIVE
                        && c.edad() == 31
                        && c.id() == 1)
                .verifyComplete();

        verify(repositoryPort).findById(1);
        verify(repositoryPort).update(expectedResult);
    }

    @Test
    void updateCustomer_shouldThrowClienteNoEncontradoException_whenNotExists() {
        Customer updatedData = new Customer(null, "Alexander", "Prieto", CustomerState.ACTIVE, 31);
        when(repositoryPort.findById(99)).thenReturn(Mono.empty());

        StepVerifier.create(domainService.updateCustomer(99, updatedData))
                .expectErrorMatches(e -> e instanceof ClienteNoEncontradoException
                        && e.getMessage().contains("99"))
                .verify();

        verify(repositoryPort).findById(99);
        verify(repositoryPort, never()).update(any());
    }

    // ── deleteCustomer ───────────────────────────────────────────────────────

    @Test
    void deleteCustomer_shouldReturnDeletedCustomer_whenExists() {
        when(repositoryPort.findById(1)).thenReturn(Mono.just(SAVED_CUSTOMER));
        when(repositoryPort.delete(1)).thenReturn(Mono.empty());

        StepVerifier.create(domainService.deleteCustomer(1))
                .expectNext(SAVED_CUSTOMER)
                .verifyComplete();

        verify(repositoryPort).findById(1);
        verify(repositoryPort).delete(1);
    }

    @Test
    void deleteCustomer_shouldThrowClienteNoEncontradoException_whenNotExists() {
        when(repositoryPort.findById(99)).thenReturn(Mono.empty());

        StepVerifier.create(domainService.deleteCustomer(99))
                .expectErrorMatches(e -> e instanceof ClienteNoEncontradoException
                        && e.getMessage().contains("99"))
                .verify();

        verify(repositoryPort).findById(99);
        verify(repositoryPort, never()).delete(any());
    }
}
