package com.apchavez.customers.infrastructure.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class TransactionConfig {

    /**
     * Spring Boot does NOT auto-configure a ReactiveTransactionManager for R2DBC
     * (unlike JPA, which gets JpaTransactionManager automatically). Without this bean,
     * @Transactional on reactive Mono/Flux methods is silently ignored and writes
     * are NOT atomic.
     */
    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory);
    }
}
