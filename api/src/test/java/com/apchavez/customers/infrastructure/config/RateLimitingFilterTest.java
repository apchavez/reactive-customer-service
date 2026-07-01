package com.apchavez.customers.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(redisTemplate);
    }

    private MockServerWebExchange buildExchange(HttpMethod method, String path, String ip) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(method, path)
                .remoteAddress(new InetSocketAddress(ip, 80))
                .build();
        return MockServerWebExchange.from(request);
    }

    private WebFilterChain passThroughChain() {
        return exchange -> Mono.empty();
    }

    // ── GET is never rate-limited ────────────────────────────────────────────

    @Test
    void should_allow_get_requests_without_calling_redis() {
        MockServerWebExchange exchange = buildExchange(HttpMethod.GET, "/api/v1/customers/active", "1.1.1.1");

        StepVerifier.create(filter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
        // Redis must not be called for GET
    }

    // ── POST within limit ────────────────────────────────────────────────────

    @Test
    void should_allow_post_when_count_is_within_limit() {
        doReturn(Flux.just(1L)).when(redisTemplate).execute(any(), anyList(), anyList());

        MockServerWebExchange exchange = buildExchange(HttpMethod.POST, "/api/v1/customers", "2.2.2.2");

        StepVerifier.create(filter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    // ── POST blocked when limit exceeded ────────────────────────────────────

    @Test
    void should_block_post_when_redis_count_exceeds_limit() {
        doReturn(Flux.just((long) RateLimitingFilter.MAX_REQUESTS + 1))
                .when(redisTemplate).execute(any(), anyList(), anyList());

        MockServerWebExchange blocked = buildExchange(HttpMethod.POST, "/api/v1/customers", "3.3.3.3");
        filter.filter(blocked, passThroughChain()).block();

        assertThat(blocked.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(blocked.getResponse().getHeaders().getFirst("Retry-After")).isNotNull();
    }

    // ── PUT blocked when limit exceeded ─────────────────────────────────────

    @Test
    void should_block_put_when_redis_count_exceeds_limit() {
        doReturn(Flux.just((long) RateLimitingFilter.MAX_REQUESTS + 1))
                .when(redisTemplate).execute(any(), anyList(), anyList());

        MockServerWebExchange blocked = buildExchange(HttpMethod.PUT, "/api/v1/customers/1", "4.4.4.4");
        filter.filter(blocked, passThroughChain()).block();

        assertThat(blocked.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ── DELETE blocked when limit exceeded ───────────────────────────────────

    @Test
    void should_block_delete_when_redis_count_exceeds_limit() {
        doReturn(Flux.just((long) RateLimitingFilter.MAX_REQUESTS + 1))
                .when(redisTemplate).execute(any(), anyList(), anyList());

        MockServerWebExchange blocked = buildExchange(HttpMethod.DELETE, "/api/v1/customers/1", "5.5.5.5");
        filter.filter(blocked, passThroughChain()).block();

        assertThat(blocked.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ── Per-IP isolation ─────────────────────────────────────────────────────

    @Test
    void should_track_limits_independently_per_ip() {
        // Counter per Redis key (key contains the IP, so per-IP isolation is tested)
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        doAnswer(inv -> {
            String key = ((java.util.List<?>) inv.getArgument(1)).get(0).toString();
            return Flux.just(counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet());
        }).when(redisTemplate).execute(any(), anyList(), anyList());

        String ip1 = "6.6.6.6";
        String ip2 = "7.7.7.7";

        // Exhaust ip1
        for (int i = 0; i <= RateLimitingFilter.MAX_REQUESTS; i++) {
            filter.filter(buildExchange(HttpMethod.POST, "/api/v1/customers", ip1), passThroughChain()).block();
        }

        MockServerWebExchange blockedIp1 = buildExchange(HttpMethod.POST, "/api/v1/customers", ip1);
        filter.filter(blockedIp1, passThroughChain()).block();
        assertThat(blockedIp1.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // ip2 should still be allowed (its key has count=1)
        MockServerWebExchange allowedIp2 = buildExchange(HttpMethod.POST, "/api/v1/customers", ip2);
        filter.filter(allowedIp2, passThroughChain()).block();
        assertThat(allowedIp2.getResponse().getStatusCode()).isNull();
    }

    // ── Fail-open when Redis is unavailable ──────────────────────────────────

    @Test
    void should_allow_request_when_redis_is_unavailable() {
        doReturn(Flux.error(new RuntimeException("Connection refused: localhost/6379")))
                .when(redisTemplate).execute(any(), anyList(), anyList());

        MockServerWebExchange exchange = buildExchange(HttpMethod.POST, "/api/v1/customers", "8.8.8.8");

        StepVerifier.create(filter.filter(exchange, passThroughChain()))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    // ── X-Forwarded-For: rightmost IP is used ────────────────────────────────

    @Test
    void should_use_rightmost_ip_from_x_forwarded_for_header() {
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        doAnswer(inv -> {
            String key = ((java.util.List<?>) inv.getArgument(1)).get(0).toString();
            return Flux.just(counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet());
        }).when(redisTemplate).execute(any(), anyList(), anyList());

        String spoofedIp = "1.2.3.4";
        String trustedIp = "10.10.10.10";

        // Send MAX_REQUESTS+1 requests with same X-Forwarded-For (rightmost = trustedIp)
        for (int i = 0; i <= RateLimitingFilter.MAX_REQUESTS; i++) {
            MockServerHttpRequest req = MockServerHttpRequest
                    .method(HttpMethod.POST, "/api/v1/customers")
                    .remoteAddress(new InetSocketAddress("127.0.0.1", 80))
                    .header("X-Forwarded-For", spoofedIp + ", " + trustedIp)
                    .build();
            filter.filter(MockServerWebExchange.from(req), passThroughChain()).block();
        }

        // Next request from the same trustedIp must be blocked
        MockServerWebExchange blocked = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.POST, "/api/v1/customers")
                        .remoteAddress(new InetSocketAddress("127.0.0.1", 80))
                        .header("X-Forwarded-For", spoofedIp + ", " + trustedIp)
                        .build());
        filter.filter(blocked, passThroughChain()).block();
        assertThat(blocked.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }
}
