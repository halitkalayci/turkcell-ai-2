package com.ecommerce.gateway.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Component
public class OrderServiceHealthIndicator implements HealthIndicator {

    private final WebClient webClient;
    private static final String ORDER_SERVICE_HEALTH_URL = "http://localhost:8081/actuator/health";

    public OrderServiceHealthIndicator(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public Health health() {
        try {
            String response = webClient.get()
                .uri(ORDER_SERVICE_HEALTH_URL)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            
            log.debug("Order Service health check: UP");
            return Health.up()
                .withDetail("uri", ORDER_SERVICE_HEALTH_URL)
                .withDetail("status", "Service is reachable")
                .build();
                
        } catch (Exception e) {
            log.warn("Order Service health check: DOWN - {}", e.getMessage());
            return Health.down()
                .withDetail("uri", ORDER_SERVICE_HEALTH_URL)
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
