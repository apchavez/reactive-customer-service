package com.apchavez.customers.domain.event;

import com.apchavez.customers.domain.model.Customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerEvent(
        String eventId,
        CustomerEventType eventType,
        String occurredAt,
        Customer customer) {

    public static CustomerEvent of(CustomerEventType type, Customer customer) {
        return new CustomerEvent(UUID.randomUUID().toString(), type, Instant.now().toString(), customer);
    }
}
