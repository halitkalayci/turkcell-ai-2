package com.ecommerce.inventoryservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for Inventory Service API documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventoryServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Inventory Service API")
                        .description("""
                                Inventory Service manages product stock levels and reservations for the e-commerce platform.
                                
                                **Key Features:**
                                - Real-time stock availability checks
                                - Synchronous stock reservations with TTL
                                - Automatic reservation expiration handling
                                
                                **Business Rules:**
                                - Reservations expire after 15 minutes by default
                                - Insufficient stock returns 422 Unprocessable Entity
                                - Concurrent reservation conflicts return 409 Conflict
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("E-Commerce Platform Team")
                                .email("platform@ecommerce.com")));
    }
}
