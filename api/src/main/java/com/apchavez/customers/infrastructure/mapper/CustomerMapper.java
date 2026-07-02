package com.apchavez.customers.infrastructure.mapper;

import com.apchavez.customers.domain.model.Customer;
import com.apchavez.customers.domain.model.CustomerState;
import com.apchavez.customers.infrastructure.persistence.CustomerEntity;
import com.apchavez.customers.infrastructure.web.dto.CustomerRequestDTO;
import com.apchavez.customers.infrastructure.web.dto.CustomerResponseDTO;
import com.apchavez.customers.infrastructure.web.dto.CustomerUpdateRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class CustomerMapper {

    public Customer toDomain(CustomerRequestDTO dto) {
        return new Customer(
                null,
                dto.nombre(),
                dto.apellido(),
                CustomerState.fromString(dto.estado()),
                dto.edad());
    }

    public Customer toDomain(CustomerUpdateRequestDTO dto) {
        return new Customer(
                null,
                dto.nombre(),
                dto.apellido(),
                CustomerState.fromString(dto.estado()),
                dto.edad());
    }

    public Customer toDomain(CustomerEntity entity) {
        return new Customer(
                entity.getId(),
                entity.getNombre(),
                entity.getApellido(),
                CustomerState.fromString(entity.getEstado()),
                entity.getEdad());
    }

    public CustomerEntity toEntity(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(customer.id());
        entity.setNombre(customer.nombre());
        entity.setApellido(customer.apellido());
        entity.setEstado(customer.estado().name());
        entity.setEdad(customer.edad());
        return entity;
    }

    public CustomerResponseDTO toResponseDTO(Customer customer) {
        return new CustomerResponseDTO(
                customer.id(),
                customer.nombre(),
                customer.apellido(),
                customer.estado().name(),
                customer.edad());
    }
}
