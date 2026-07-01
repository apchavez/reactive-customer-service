package com.apchavez.customers.infrastructure.config;

import com.apchavez.customers.domain.port.CustomerRepositoryPort;
import com.apchavez.customers.domain.service.CustomerDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public CustomerDomainService customerDomainService(CustomerRepositoryPort repositoryPort) {
        return new CustomerDomainService(repositoryPort);
    }
}
