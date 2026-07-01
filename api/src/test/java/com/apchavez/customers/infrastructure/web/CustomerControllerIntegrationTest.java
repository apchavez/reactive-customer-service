package com.apchavez.customers.infrastructure.web;

import com.apchavez.customers.infrastructure.config.JwtService;
import com.apchavez.customers.infrastructure.persistence.CustomerEntity;
import com.apchavez.customers.infrastructure.persistence.CustomerR2dbcRepository;
import com.apchavez.customers.infrastructure.web.dto.CustomerRequestDTO;
import com.apchavez.customers.infrastructure.web.dto.CustomerResponseDTO;
import com.apchavez.customers.infrastructure.web.dto.CustomerUpdateRequestDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
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

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        r2dbcRepository.deleteAll().block();
        adminToken = jwtService.generateToken("test-admin", "ADMIN");
        userToken = jwtService.generateToken("test-user", "USER");
    }

    // ── POST /api/v1/customers ───────────────────────────────────────────────

    @Test
    void createCustomer_shouldReturn201_withGeneratedId() {
        CustomerRequestDTO request = new CustomerRequestDTO("Alex", "Prieto", "ACTIVE", 30);

        webTestClient.post()
                .uri("/api/v1/customers")
                .header("Authorization", "Bearer " + adminToken)
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
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errores").isArray();
    }

    @Test
    void createCustomer_shouldReturn401_whenNoToken() {
        CustomerRequestDTO request = new CustomerRequestDTO("Alex", "Prieto", "ACTIVE", 30);

        webTestClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void createCustomer_shouldReturn403_whenUserRole() {
        CustomerRequestDTO request = new CustomerRequestDTO("Alex", "Prieto", "ACTIVE", 30);

        webTestClient.post()
                .uri("/api/v1/customers")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isForbidden();
    }

    // ── GET /api/v1/customers/active ─────────────────────────────────────────

    @Test
    void listActiveCustomers_shouldReturn200_withOnlyActiveCustomers() {
        r2dbcRepository.save(new CustomerEntity(null, "Carlos", "Lopez", "ACTIVE", 22)).block();
        r2dbcRepository.save(new CustomerEntity(null, "Maria", "Gomez", "INACTIVE", 20)).block();
        r2dbcRepository.save(new CustomerEntity(null, "Ana", "Diaz", "ACTIVE", 30)).block();

        webTestClient.get()
                .uri("/api/v1/customers/active")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(2)
                .jsonPath("$.content[0].estado").isEqualTo("ACTIVE")
                .jsonPath("$.content[1].estado").isEqualTo("ACTIVE")
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(20)
                .jsonPath("$.totalElements").isEqualTo(2);
    }

    @Test
    void listActiveCustomers_shouldReturn200_withEmptyArray_whenNoActiveCustomers() {
        r2dbcRepository.save(new CustomerEntity(null, "Maria", "Gomez", "INACTIVE", 20)).block();

        webTestClient.get()
                .uri("/api/v1/customers/active")
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.content.length()").isEqualTo(0)
                .jsonPath("$.totalElements").isEqualTo(0);
    }

    @Test
    void listActiveCustomers_shouldReturn401_whenNoToken() {
        webTestClient.get()
                .uri("/api/v1/customers/active")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    // ── GET /api/v1/customers/{id} ───────────────────────────────────────────

    @Test
    void findById_shouldReturn200_whenCustomerExists() {
        CustomerEntity saved = r2dbcRepository
                .save(new CustomerEntity(null, "Alex", "Prieto", "ACTIVE", 30))
                .block();

        webTestClient.get()
                .uri("/api/v1/customers/{id}", saved.getId())
                .header("Authorization", "Bearer " + userToken)
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
                .header("Authorization", "Bearer " + userToken)
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
                .header("Authorization", "Bearer " + userToken)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void findById_shouldReturn400_whenIdIsZero() {
        webTestClient.get()
                .uri("/api/v1/customers/{id}", 0)
                .header("Authorization", "Bearer " + userToken)
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
                .header("Authorization", "Bearer " + adminToken)
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
                .header("Authorization", "Bearer " + adminToken)
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
                .header("Authorization", "Bearer " + adminToken)
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
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NO_CONTENT);

        webTestClient.get()
                .uri("/api/v1/customers/{id}", saved.getId())
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteCustomer_shouldReturn404_whenNotFound() {
        webTestClient.delete()
                .uri("/api/v1/customers/{id}", 9999)
                .header("Authorization", "Bearer " + adminToken)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404);
    }
}
