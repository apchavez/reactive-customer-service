package com.apchavez.customers.domain.port;

import com.apchavez.customers.domain.event.CustomerEvent;
import reactor.core.publisher.Mono;

public interface CustomerEventPublisherPort {
    Mono<Void> publish(CustomerEvent event);
}
