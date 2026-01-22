package com.ecommerce.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@Order(-2)
public class GlobalErrorHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        String correlationId = exchange.getAttribute("correlationId");
        
        log.error("[GATEWAY-ERROR] [{}] {} - {}", correlationId, path, ex.getMessage());
        
        HttpStatus status;
        String title;
        String detail;
        
        if (ex instanceof ConnectException || ex.getCause() instanceof ConnectException) {
            status = HttpStatus.BAD_GATEWAY;
            title = "Bad Gateway";
            detail = "Service is currently unavailable";
        } else if (ex instanceof TimeoutException || ex.getCause() instanceof TimeoutException) {
            status = HttpStatus.GATEWAY_TIMEOUT;
            title = "Gateway Timeout";
            detail = "Request timeout while connecting to service";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            title = "Internal Server Error";
            detail = "An unexpected error occurred";
        }
        
        String problemDetails = String.format("""
            {
              "type": "about:blank",
              "title": "%s",
              "status": %d,
              "detail": "%s",
              "instance": "%s"
            }
            """, title, status.value(), detail, path);
        
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        
        DataBuffer buffer = exchange.getResponse()
            .bufferFactory()
            .wrap(problemDetails.getBytes(StandardCharsets.UTF_8));
        
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
