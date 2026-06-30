package com.apchavez.customers.infrastructure.web;

import com.apchavez.customers.infrastructure.persistence.CustomerEntity;
import com.apchavez.customers.infrastructure.persistence.CustomerR2dbcRepository;
import com.apchavez.customers.infrastructure.web.dto.CustomerRequestDTO;
import com.apchavez.customers.infrastructure.web.dto.CustomerResponseDTO;
import com.apchavez.customers.infrastructure.web.dto.CustomerUpdateRequestDTO;
import com.apchavez.customers.infrastructure.web.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class CustomerControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CustomerR2dbcRepository r2dbcRepository;

    @BeforeEach
    void clearDatabase() {
        r2dbcRepository.deleteAll().block();
    }

    // ── POST /api/v1/customers ───────────────────────────────────────────────

    @Test
    void createCustomer_shouldReturn201_withGeneratedId() {
        CustomerRequestDTO request = new CustomerRequestDTO("Alex", "Prieto", "ACTIVE", 30);

        webTestClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CustomerResponseDTO.class)
                .value(response -> {
                    assertThat(response.id()).isNotNull();
                    assertThat(response.nombre()).isEqualTo("Alex");
                    assertThat(response.apellido()).isEqualTo("Prieto");
                    assertThat(response.estado()).isEqualTo("ACTIVE");
                    assertThat(response.edad()).isEqualTo(30);
                });
    }

    @Test
    void createCustomer_shouldReturn400_whenRequestIsInvalid() {
        CustomerRequestDTO request = new CustomerRequestDTO("", null, "INVALID", -1);

        webTestClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errores").isArray();
    }

    // ── GET /api/v1/customers/active ─────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void listActiveCustomers_shouldReturn200_withOnlyActiveCustomers() {
        r2dbcRepository.save(new CustomerEntity(null, "Carlos", "Lopez", "ACTIVE", 22)).block();
        r2dbcRepository.save(new CustomerEntity(null, "Maria", "Gomez", "INACTIVE", 20)).block();
        r2dbcRepository.save(new CustomerEntity(null, "Ana", "Diaz", "ACTIVE", 30)).block();

        webTestClient.get()
                .uri("/api/v1/customers/active")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PageResponse.class)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(2L);
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.page()).isEqualTo(0);
                    assertThat(page.size()).isEqualTo(20);
                });
    }

    @Test
    void listActiveCustomers_shouldReturn200_withEmptyPage_whenNoActiveCustomers() {
        r2dbcRepository.save(new CustomerEntity(null, "Maria", "Gomez", "INACTIVE", 20)).block();

        webTestClient.get()
                .uri("/api/v1/customers/active")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(0)
                .jsonPath("$.content").isArray();
    }

    @Test
    @SuppressWarnings("unchecked")
    void listActiveCustomers_shouldReturn200_withPaginatedResults() {
        for (int i = 1; i <= 5; i++) {
            r2dbcRepository.save(new CustomerEntity(null, "Customer" + i, "Last" + i, "ACTIVE", 20 + i)).block();
        }

        webTestClient.get()
                .uri("/api/v1/customers/active?page=0&size=2")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PageResponse.class)
                .value(page -> {
                    assertThat(page.totalElements()).isEqualTo(5L);
                    assertThat(page.content()).hasSize(2);
                    assertThat(page.totalPages()).isEqualTo(3);
                    assertThat(page.last()).isFalse();
                });
    }

    // ── GET /api/v1/customers/{id} ───────────────────────────────────────────

    @Test
    void findById_shouldReturn200_whenCustomerExists() {
        CustomerEntity saved = r2dbcRepository
                .save(new CustomerEntity(null, "Alex", "Prieto", "ACTIVE", 30))
                .block();

        webTestClient.get()
                .uri("/api/v1/customers/{id}", saved.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerResponseDTO.class)
                .value(response -> {
                    assertThat(response.id()).isEqualTo(saved.getId());
                    assertThat(response.nombre()).isEqualTo("Alex");
                });
    }

    @Test
    void findById_shouldReturn404_whenCustomerNotFound() {
        webTestClient.get()
                .uri("/api/v1/customers/{id}", 9999)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.mensaje").isNotEmpty();
    }

    @Test
    void findById_shouldReturn400_whenIdIsNegative() {
        webTestClient.get()
                .uri("/api/v1/customers/{id}", -1)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void findById_shouldReturn400_whenIdIsZero() {
        webTestClient.get()
                .uri("/api/v1/customers/{id}", 0)
                .exchange()
                .expectStatus().isBadRequest();
    }

    // ── PUT /api/v1/customers/{id} ───────────────────────────────────────────

    @Test
    void updateCustomer_shouldReturn200_withUpdatedData_whenExists() {
        CustomerEntity saved = r2dbcRepository
                .save(new CustomerEntity(null, "Alex", "Prieto", "ACTIVE", 30))
                .block();

        CustomerUpdateRequestDTO request =
                new CustomerUpdateRequestDTO("Alexander", "Prieto Chavez", "INACTIVE", 31);

        webTestClient.put()
                .uri("/api/v1/customers/{id}", saved.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(CustomerResponseDTO.class)
                .value(response -> {
                    assertThat(response.id()).isEqualTo(saved.getId());
                    assertThat(response.nombre()).isEqualTo("Alexander");
                    assertThat(response.apellido()).isEqualTo("Prieto Chavez");
                    assertThat(response.estado()).isEqualTo("INACTIVE");
                    assertThat(response.edad()).isEqualTo(31);
                });
    }

    @Test
    void updateCustomer_shouldReturn404_whenNotFound() {
        CustomerUpdateRequestDTO request =
                new CustomerUpdateRequestDTO("Alexander", "Prieto", "ACTIVE", 30);

        webTestClient.put()
                .uri("/api/v1/customers/{id}", 9999)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }

    @Test
    void updateCustomer_shouldReturn400_whenRequestIsInvalid() {
        CustomerUpdateRequestDTO request =
                new CustomerUpdateRequestDTO("", null, "INVALID", -1);

        webTestClient.put()
                .uri("/api/v1/customers/{id}", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errores").isArray();
    }

    // ── DELETE /api/v1/customers/{id} ────────────────────────────────────────

    @Test
    void deleteCustomer_shouldReturn204_whenExists() {
        CustomerEntity saved = r2dbcRepository
                .save(new CustomerEntity(null, "Alex", "Prieto", "ACTIVE", 30))
                .block();

        webTestClient.delete()
                .uri("/api/v1/customers/{id}", saved.getId())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

        webTestClient.get()
                .uri("/api/v1/customers/{id}", saved.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteCustomer_shouldReturn404_whenNotFound() {
        webTestClient.delete()
                .uri("/api/v1/customers/{id}", 9999)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }
}
