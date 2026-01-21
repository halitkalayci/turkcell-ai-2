# Order Service

Order Service for E-commerce Microservices Platform built with Spring Boot 3.X and Java 21.

## Overview

This is a standalone microservice that handles order management operations including:
- Creating new orders
- Retrieving order details
- Cancelling orders
- Updating order status

## Architecture

- **Approach**: Contract-First using OpenAPI 3.0
- **Framework**: Spring Boot 3.2.1
- **Java Version**: 21
- **Database**: H2 In-Memory
- **API Documentation**: Swagger/OpenAPI

## Project Structure

```
order-service/
├── src/
│   └── main/
│       ├── java/com/ecommerce/orderservice/
│       │   ├── OrderServiceApplication.java
│       │   ├── controller/
│       │   │   └── OrderController.java
│       │   ├── service/
│       │   │   ├── OrderService.java
│       │   │   └── OrderServiceImpl.java
│       │   ├── repository/
│       │   │   └── OrderRepository.java
│       │   ├── domain/
│       │   │   ├── Order.java
│       │   │   ├── OrderItem.java
│       │   │   ├── OrderStatus.java
│       │   │   └── Address.java
│       │   ├── mapper/
│       │   │   └── OrderMapper.java
│       │   └── exception/
│       │       ├── GlobalExceptionHandler.java
│       │       ├── OrderNotFoundException.java
│       │       └── BusinessRuleViolationException.java
│       └── resources/
│           └── application.yml
├── pom.xml
└── README.md
```

## Prerequisites

- JDK 21
- Maven 3.8+

## Building the Project

```bash
cd order-service
mvn clean install
```

This will:
1. Generate API models and interfaces from the OpenAPI specification
2. Compile the source code
3. Run tests
4. Package the application as a JAR file

## Running the Application

```bash
mvn spring-boot:run
```

Or run the JAR file directly:

```bash
java -jar target/order-service-1.0.0.jar
```

The service will start on `http://localhost:8080`

## API Documentation

Once the application is running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs
- **H2 Console**: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:orderdb`
  - Username: `sa`
  - Password: (leave empty)

## API Endpoints

### Create Order
```http
POST /orders
Content-Type: application/json

{
  "customerId": "123e4567-e89b-12d3-a456-426614174000",
  "address": {
    "street": "123 Main Street, Apt 4B",
    "city": "Istanbul",
    "state": "Marmara",
    "postalCode": "34000",
    "country": "Turkey"
  },
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440001",
      "quantity": 2
    }
  ]
}
```

### Get Order by ID
```http
GET /orders/{id}
```

### Cancel Order
```http
POST /orders/{id}/cancel
```

### Update Order Status
```http
PATCH /orders/{id}/status
Content-Type: application/json

{
  "status": "SHIPPED"
}
```

## Order Status Flow

- `PREPARING` → Initial status when order is created
- `SHIPPED` → Order has been shipped
- `DELIVERED` → Order has been delivered
- `CANCELLED` → Order was cancelled (only possible when status is PREPARING)

## Business Rules

1. Orders can only be cancelled when status is `PREPARING`
2. Cancelled orders cannot have their status updated
3. Product details (name, price) are currently mocked for standalone operation
4. All orders require at least one item
5. All address fields except `state` are mandatory

## Error Handling

The service follows RFC 7807 Problem Details for HTTP APIs:

- **400 Bad Request**: Validation errors
- **404 Not Found**: Order not found
- **422 Unprocessable Entity**: Business rule violations
- **500 Internal Server Error**: Unexpected errors

## Technologies Used

- Spring Boot 3.2.1
- Spring Data JPA
- Spring Web
- H2 Database
- OpenAPI Generator
- MapStruct
- Lombok
- Jakarta Validation
- Springdoc OpenAPI

## Future Enhancements

- Integration with inventory-service for stock validation
- Integration with notification-service for order updates
- Event publishing (Kafka/RabbitMQ)
- Authentication & Authorization (JWT)
- PostgreSQL for production database
- Docker containerization
