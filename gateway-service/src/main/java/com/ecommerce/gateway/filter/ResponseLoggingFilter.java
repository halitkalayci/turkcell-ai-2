package com.ecommerce.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class ResponseLoggingFilter implements GlobalFilter, Ordered {

    private static final String CORRELATION_ID_ATTRIBUTE = "correlationId";
    private static final String REQUEST_START_TIME = "requestStartTime";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            String correlationId = exchange.getAttribute(CORRELATION_ID_ATTRIBUTE);
            Long startTime = exchange.getAttribute(REQUEST_START_TIME);
            
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                int statusCode = exchange.getResponse().getStatusCode() != null 
                    ? exchange.getResponse().getStatusCode().value() 
                    : 0;
                
                log.debug("[GATEWAY-RESPONSE] [{}] {} {}ms", 
                    correlationId, 
                    statusCode, 
                    duration);
            }
        }));
    }

    @Override
    public int getOrder() {
        return 1; // Execute after routing
    }
}
