package com.apchavez.customers.application;

import com.apchavez.customers.domain.event.CustomerEvent;
import com.apchavez.customers.domain.exception.ClienteNoEncontradoException;
import com.apchavez.customers.domain.model.Customer;
import com.apchavez.customers.domain.model.CustomerState;
import com.apchavez.customers.domain.port.CustomerEventPublisherPort;
import com.apchavez.customers.domain.port.CustomerRepositoryPort;
import com.apchavez.customers.domain.service.CustomerDomainService;
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
class CustomerApplicationServiceTest {

    @Mock
    private CustomerRepositoryPort repositoryPort;

    @Mock
    private CustomerEventPublisherPort eventPublisher;

    private CustomerApplicationService applicationService;

    private static final Customer CUSTOMER_WITHOUT_ID =
            new Customer(null, "Alex", "Prieto", CustomerState.ACTIVE, 30);
    private static final Customer SAVED_CUSTOMER =
            new Customer(1, "Alex", "Prieto", CustomerState.ACTIVE, 30);

    @BeforeEach
    void setUp() {
        CustomerDomainService domainService = new CustomerDomainService(repositoryPort);
        applicationService = new CustomerApplicationService(domainService, eventPublisher);
        lenient().when(eventPublisher.publish(any(CustomerEvent.class))).thenReturn(Mono.empty());
    }

    // ── createCustomer ───────────────────────────────────────────────────────

    @Test
    void createCustomer_shouldSaveAndPublishEvent() {
        when(repositoryPort.save(any())).thenReturn(Mono.just(SAVED_CUSTOMER));

        StepVerifier.create(applicationService.createCustomer(CUSTOMER_WITHOUT_ID))
                .expectNext(SAVED_CUSTOMER)
                .verifyComplete();

        verify(repositoryPort).save(CUSTOMER_WITHOUT_ID);
        verify(eventPublisher).publish(any(CustomerEvent.class));
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_shouldReturnCustomer_whenExists() {
        when(repositoryPort.findById(1)).thenReturn(Mono.just(SAVED_CUSTOMER));

        StepVerifier.create(applicationService.findById(1))
                .expectNext(SAVED_CUSTOMER)
                .verifyComplete();
    }

    @Test
    void findById_shouldThrowClienteNoEncontradoException_whenNotExists() {
        when(repositoryPort.findById(99)).thenReturn(Mono.empty());

        StepVerifier.create(applicationService.findById(99))
                .expectErrorMatches(e -> e instanceof ClienteNoEncontradoException
                        && e.getMessage().contains("99"))
                .verify();
    }

    // ── listActiveCustomers ──────────────────────────────────────────────────

    @Test
    void listActiveCustomers_shouldDelegateToPort() {
        Customer active1 = new Customer(1, "Carlos", "Lopez", CustomerState.ACTIVE, 22);
        Customer active2 = new Customer(3, "Ana", "Diaz", CustomerState.ACTIVE, 30);
        when(repositoryPort.findAllActive(0, 10)).thenReturn(Flux.just(active1, active2));

        StepVerifier.create(applicationService.listActiveCustomers(0, 10))
                .expectNext(active1)
                .expectNext(active2)
                .verifyComplete();

        verify(repositoryPort).findAllActive(0, 10);
    }

    // ── updateCustomer ───────────────────────────────────────────────────────

    @Test
    void updateCustomer_shouldReturnUpdatedCustomerAndPublishEvent() {
        Customer updatedData = new Customer(null, "Alexander", "Prieto Chavez", CustomerState.INACTIVE, 31);
        Customer expectedResult = new Customer(1, "Alexander", "Prieto Chavez", CustomerState.INACTIVE, 31);

        when(repositoryPort.findById(1)).thenReturn(Mono.just(SAVED_CUSTOMER));
        when(repositoryPort.update(any())).thenReturn(Mono.just(expectedResult));

        StepVerifier.create(applicationService.updateCustomer(1, updatedData))
                .expectNextMatches(c -> c.nombre().equals("Alexander") && c.id() == 1)
                .verifyComplete();

        verify(repositoryPort).findById(1);
        verify(repositoryPort).update(expectedResult);
        verify(eventPublisher).publish(any(CustomerEvent.class));
    }

    @Test
    void updateCustomer_shouldThrowClienteNoEncontradoException_whenNotExists() {
        Customer updatedData = new Customer(null, "Alexander", "Prieto", CustomerState.ACTIVE, 31);
        when(repositoryPort.findById(99)).thenReturn(Mono.empty());

        StepVerifier.create(applicationService.updateCustomer(99, updatedData))
                .expectErrorMatches(e -> e instanceof ClienteNoEncontradoException
                        && e.getMessage().contains("99"))
                .verify();
    }

    // ── deleteCustomer ───────────────────────────────────────────────────────

    @Test
    void deleteCustomer_shouldCompleteAndPublishEvent() {
        when(repositoryPort.findById(1)).thenReturn(Mono.just(SAVED_CUSTOMER));
        when(repositoryPort.delete(1)).thenReturn(Mono.empty());

        StepVerifier.create(applicationService.deleteCustomer(1))
                .verifyComplete();

        verify(repositoryPort).findById(1);
        verify(repositoryPort).delete(1);
        verify(eventPublisher).publish(any(CustomerEvent.class));
    }

    @Test
    void deleteCustomer_shouldThrowClienteNoEncontradoException_whenNotExists() {
        when(repositoryPort.findById(99)).thenReturn(Mono.empty());

        StepVerifier.create(applicationService.deleteCustomer(99))
                .expectErrorMatches(e -> e instanceof ClienteNoEncontradoException
                        && e.getMessage().contains("99"))
                .verify();
    }
}
