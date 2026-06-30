package com.apchavez.customers.infrastructure.web;

import com.apchavez.customers.application.CustomerApplicationService;
import com.apchavez.customers.infrastructure.mapper.CustomerMapper;
import com.apchavez.customers.infrastructure.web.dto.CustomerRequestDTO;
import com.apchavez.customers.infrastructure.web.dto.CustomerResponseDTO;
import com.apchavez.customers.infrastructure.web.dto.CustomerUpdateRequestDTO;
import com.apchavez.customers.infrastructure.web.dto.PageResponse;
import com.apchavez.customers.infrastructure.web.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Operaciones de gestión de clientes")
public class CustomerController {

    private final CustomerApplicationService applicationService;
    private final CustomerMapper mapper;

    public CustomerController(CustomerApplicationService applicationService, CustomerMapper mapper) {
        this.applicationService = applicationService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Crear cliente", description = "Crea un nuevo cliente con ID generado automáticamente.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente creado",
                    content = @Content(schema = @Schema(implementation = CustomerResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Campos inválidos (Bean Validation)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Violación de regla de dominio",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<CustomerResponseDTO>> createCustomer(
            @Valid @RequestBody CustomerRequestDTO dto) {
        return applicationService.createCustomer(mapper.toDomain(dto))
                .map(saved -> ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponseDTO(saved)));
    }

    @GetMapping("/active")
    @Operation(summary = "Listar clientes activos", description = "Retorna clientes con estado ACTIVE. Paginado con page (0-based) y size (max 100).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de clientes activos")
    })
    public Mono<PageResponse<CustomerResponseDTO>> listActiveCustomers(
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Positive @Max(100) int size) {
        return Mono.zip(
                applicationService.countActiveCustomers(),
                applicationService.listActiveCustomers(page, size)
                        .map(mapper::toResponseDTO)
                        .collectList()
        ).map(tuple -> PageResponse.of(tuple.getT2(), page, size, tuple.getT1()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID", description = "Retorna el cliente con el ID indicado o 404 si no existe.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = CustomerResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "ID inválido (debe ser mayor que cero)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<CustomerResponseDTO>> findById(
            @PathVariable @Positive(message = "El ID debe ser mayor que cero") Integer id) {
        return applicationService.findById(id)
                .map(customer -> ResponseEntity.ok(mapper.toResponseDTO(customer)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente", description = "Reemplaza todos los datos del cliente con el ID indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado",
                    content = @Content(schema = @Schema(implementation = CustomerResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "ID o campos inválidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Violación de regla de dominio",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<CustomerResponseDTO>> updateCustomer(
            @PathVariable @Positive(message = "El ID debe ser mayor que cero") Integer id,
            @Valid @RequestBody CustomerUpdateRequestDTO dto) {
        return applicationService.updateCustomer(id, mapper.toDomain(dto))
                .map(updated -> ResponseEntity.ok(mapper.toResponseDTO(updated)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cliente", description = "Elimina el cliente con el ID indicado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente eliminado"),
            @ApiResponse(responseCode = "400", description = "ID inválido (debe ser mayor que cero)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public Mono<ResponseEntity<Void>> deleteCustomer(
            @PathVariable @Positive(message = "El ID debe ser mayor que cero") Integer id) {
        return applicationService.deleteCustomer(id)
                .then(Mono.just(ResponseEntity.<Void>noContent().build()));
    }
}
