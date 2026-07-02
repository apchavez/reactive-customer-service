package com.apchavez.customers.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;

@Component
public class RateLimitingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    static final int MAX_REQUESTS = 100;
    private static final int WINDOW_SECONDS = 60;
    private static final String KEY_PREFIX = "rl:";
    private static final String TARGET_PATH_PREFIX = "/api/v1/customers";
    private static final Set<HttpMethod> TARGET_METHODS =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE);

    // Atomic fixed-window: INCR then set TTL only on first call to avoid resetting the window.
    // Keys auto-expire in Redis so no manual cleanup thread is needed.
    private static final RedisScript<Long> RATE_LIMIT_SCRIPT = RedisScript.of("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """, Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitingFilter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (!TARGET_METHODS.contains(request.getMethod())
                || !request.getPath().value().startsWith(TARGET_PATH_PREFIX)) {
            return chain.filter(exchange);
        }

        String ip = extractClientIp(request);
        long bucket = System.currentTimeMillis() / (WINDOW_SECONDS * 1000L);
        String key = KEY_PREFIX + ip + ":" + bucket;

        return redisTemplate.execute(RATE_LIMIT_SCRIPT, List.of(key), List.of(String.valueOf(WINDOW_SECONDS)))
                .next()
                .flatMap(count -> {
                    if (count > MAX_REQUESTS) {
                        log.warn("[RATE-LIMIT] IP '{}' bloqueada — solicitud #{} ({} {})",
                                ip, count, request.getMethod(), request.getPath());
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(WINDOW_SECONDS));
                        return exchange.getResponse().setComplete();
                    }
                    return chain.filter(exchange);
                })
                .onErrorResume(ex -> {
                    // Redis unavailable: fail-open to avoid blocking legitimate traffic.
                    log.warn("[RATE-LIMIT] Redis no disponible (fail-open) — IP '{}': {}", ip, ex.getMessage());
                    return chain.filter(exchange);
                });
    }

    private String extractClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Rightmost IP is added by trusted infrastructure; leftmost can be spoofed.
            String[] parts = forwarded.split(",");
            for (int i = parts.length - 1; i >= 0; i--) {
                String ip = parts[i].trim();
                if (!ip.isBlank()) {
                    return ip;
                }
            }
        }
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        return remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
    }
}
