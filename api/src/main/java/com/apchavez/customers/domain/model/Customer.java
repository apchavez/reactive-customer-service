package com.apchavez.customers.domain.model;

import com.apchavez.customers.domain.exception.ClienteDominioInvalidoException;

public record Customer(
        Integer id,
        String nombre,
        String apellido,
        CustomerState estado,
        Integer edad) {

    public Customer {
        if (nombre == null || nombre.isBlank()) {
            throw new ClienteDominioInvalidoException("El nombre no puede estar vacío");
        }
        if (nombre.length() > 150) {
            throw new ClienteDominioInvalidoException("El nombre no puede superar los 150 caracteres");
        }
        if (apellido == null || apellido.isBlank()) {
            throw new ClienteDominioInvalidoException("El apellido no puede estar vacío");
        }
        if (apellido.length() > 150) {
            throw new ClienteDominioInvalidoException("El apellido no puede superar los 150 caracteres");
        }
        if (edad == null || edad <= 0 || edad > 150) {
            throw new ClienteDominioInvalidoException("La edad debe ser mayor que cero");
        }
        if (estado == null) {
            throw new ClienteDominioInvalidoException("El estado debe ser 'ACTIVE' o 'INACTIVE'");
        }
    }
}
