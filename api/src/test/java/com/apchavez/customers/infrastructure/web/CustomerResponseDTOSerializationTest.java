package com.apchavez.customers.infrastructure.web;

import com.apchavez.customers.infrastructure.web.dto.CustomerResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerResponseDTOSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Provide
    Arbitrary<CustomerResponseDTO> validResponseDTOs() {
        return Combinators.combine(
                Arbitraries.integers().greaterOrEqual(1),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(150),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(150),
                Arbitraries.of("ACTIVE", "INACTIVE"),
                Arbitraries.integers().between(1, 150))
                .as(CustomerResponseDTO::new);
    }

    @Property
    void json_roundtrip_should_preserve_all_fields(@ForAll("validResponseDTOs") CustomerResponseDTO dto)
            throws Exception {
        String json = objectMapper.writeValueAsString(dto);
        CustomerResponseDTO deserialized = objectMapper.readValue(json, CustomerResponseDTO.class);
        assertThat(deserialized).isEqualTo(dto);
    }
}
