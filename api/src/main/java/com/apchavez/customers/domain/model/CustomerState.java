package com.apchavez.customers.domain.model;

import com.apchavez.customers.domain.exception.ClienteDominioInvalidoException;

public enum CustomerState {
    ACTIVE, INACTIVE;

    public static CustomerState fromString(String value) {
        if (value == null) {
            throw new ClienteDominioInvalidoException("El estado debe ser 'ACTIVE' o 'INACTIVE'");
        }
        return switch (value.toUpperCase()) {
            case "ACTIVE"   -> ACTIVE;
            case "INACTIVE" -> INACTIVE;
            default -> throw new ClienteDominioInvalidoException("El estado debe ser 'ACTIVE' o 'INACTIVE'");
        };
    }
}
