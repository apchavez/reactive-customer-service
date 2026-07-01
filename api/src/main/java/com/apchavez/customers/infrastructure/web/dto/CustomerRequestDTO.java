package com.apchavez.customers.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear un cliente")
public record CustomerRequestDTO(

        @NotBlank(message = "El nombre es requerido")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        @Schema(description = "Nombre del cliente", example = "Alex")
        String nombre,

        @NotBlank(message = "El apellido es requerido")
        @Size(max = 150, message = "El apellido no puede superar 150 caracteres")
        @Schema(description = "Apellido del cliente", example = "Prieto")
        String apellido,

        @NotBlank(message = "El estado es requerido")
        @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "El estado debe ser 'ACTIVE' o 'INACTIVE'")
        @Schema(description = "Estado del cliente", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
        String estado,

        @NotNull(message = "La edad es requerida")
        @Min(value = 1, message = "La edad debe ser mayor que cero")
        @Max(value = 150, message = "La edad no puede superar 150")
        @Schema(description = "Edad del cliente", example = "30", minimum = "1", maximum = "150")
        Integer edad) {
}
