package com.apchavez.customers.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(-100)
public class RequestLoggingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String REQUEST_ID_CONTEXT_KEY = "requestId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        long startMs = System.currentTimeMillis();

        exchange.getResponse().getHeaders().add("X-Request-Id", requestId);

        return chain.filter(exchange)
                .doOnSuccess(v -> {
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    log.info("[{}] {} {} → {} ({}ms)",
                            requestId, request.getMethod(), request.getPath(),
                            status != null ? status : "---",
                            System.currentTimeMillis() - startMs);
                })
                .doOnError(e -> log.error("[{}] {} {} → ERROR ({}ms) — {}",
                        requestId, request.getMethod(), request.getPath(),
                        System.currentTimeMillis() - startMs, e.getMessage()))
                .contextWrite(ctx -> ctx.put(REQUEST_ID_CONTEXT_KEY, requestId));
    }
}
