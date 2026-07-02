package com.apchavez.customers.domain.exception;

public class ClienteNoEncontradoException extends ClienteDominioException {
    public ClienteNoEncontradoException(Integer id) {
        super("No se encontró un cliente con el ID: " + id);
    }
}
