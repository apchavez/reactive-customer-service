package com.apchavez.customers.infrastructure.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.producer.bootstrap-servers")
public class KafkaConfig {

    @Bean
    public KafkaSender<String, String> kafkaSender(Environment environment) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getRequiredProperty("spring.kafka.producer.bootstrap-servers"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        putIfPresent(props, "security.protocol",
                environment.getProperty("spring.kafka.properties.security.protocol"));
        putIfPresent(props, "sasl.mechanism",
                environment.getProperty("spring.kafka.properties.sasl.mechanism"));
        putIfPresent(props, "sasl.jaas.config",
                environment.getProperty("spring.kafka.properties.sasl.jaas.config"));
        return KafkaSender.create(SenderOptions.create(props));
    }

    private static void putIfPresent(Map<String, Object> props, String key, String value) {
        if (value != null && !value.isBlank()) {
            props.put(key, value);
        }
    }
}
