package com.apchavez.customers.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Datos de un cliente en la respuesta")
public record CustomerResponseDTO(

        @Schema(description = "ID del cliente", example = "1")
        Integer id,

        @Schema(description = "Nombre del cliente", example = "Alex")
        String nombre,

        @Schema(description = "Apellido del cliente", example = "Prieto")
        String apellido,

        @Schema(description = "Estado del cliente", example = "ACTIVE")
        String estado,

        @Schema(description = "Edad del cliente", example = "30")
        Integer edad) {
}
