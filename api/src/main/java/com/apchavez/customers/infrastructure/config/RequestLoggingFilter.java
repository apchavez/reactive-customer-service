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
                    log.atInfo()
                            .addKeyValue("requestId", requestId)
                            .addKeyValue("http.method", request.getMethod().name())
                            .addKeyValue("url.path", request.getPath().value())
                            .addKeyValue("http.response.status_code", status != null ? status.value() : 0)
                            .addKeyValue("event.duration_ms", System.currentTimeMillis() - startMs)
                            .log("HTTP request completed");
                })
                .doOnError(e -> log.atError()
                        .addKeyValue("requestId", requestId)
                        .addKeyValue("http.method", request.getMethod().name())
                        .addKeyValue("url.path", request.getPath().value())
                        .addKeyValue("event.duration_ms", System.currentTimeMillis() - startMs)
                        .addKeyValue("error.type", e.getClass().getSimpleName())
                        .log("HTTP request failed", e))
                .contextWrite(ctx -> ctx.put(REQUEST_ID_CONTEXT_KEY, requestId));
    }
}
