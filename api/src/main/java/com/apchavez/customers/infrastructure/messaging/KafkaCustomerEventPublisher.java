package com.apchavez.customers.infrastructure.messaging;

import com.apchavez.customers.domain.event.CustomerEvent;
import com.apchavez.customers.domain.port.CustomerEventPublisherPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

@Component
@ConditionalOnProperty(name = "spring.kafka.producer.bootstrap-servers")
public class KafkaCustomerEventPublisher implements CustomerEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaCustomerEventPublisher.class);
    private static final String TOPIC = "customer-events";

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;

    public KafkaCustomerEventPublisher(KafkaSender<String, String> kafkaSender,
                                        ObjectMapper objectMapper) {
        this.kafkaSender = kafkaSender;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> publish(CustomerEvent event) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(json -> kafkaSender.send(Mono.just(SenderRecord.create(
                        new ProducerRecord<>(TOPIC, event.customer().id().toString(), json), null))).next())
                .doOnSuccess(r -> log.info("Event published: type={}, customerId={}",
                        event.eventType(), event.customer().id()))
                .doOnError(e -> log.error("Failed to publish event: type={}", event.eventType(), e))
                .onErrorResume(e -> Mono.empty())
                .then();
    }
}
