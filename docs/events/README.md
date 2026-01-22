# Event-Driven Architecture

## Overview
This project uses Spring Cloud Stream for asynchronous inter-service communication with message broker abstraction.
All event contracts are defined in this directory.

## Principles
- **Contract First:** Define events before implementation
- **Transactional Outbox:** All producers use outbox pattern
- **Idempotency:** All consumers are idempotent
- **Reliability:** Retry + DLQ for failure handling
- **Broker Agnostic:** Spring Cloud Stream abstraction allows switching brokers

## Technology Stack
- **Framework:** Spring Cloud Stream (functional programming model)
- **Current Binder:** Kafka
- **Serialization:** JSON
- **Broker Connection:** localhost:29023

## Services
- **Order Service:** Port 8081, produces order events
- **Inventory Service:** Port 8082, produces inventory events
- **Notification Service:** Port 8083, consumes all events (pure consumer)

## Message Broker Configuration
- Current: Kafka at `localhost:29023`
- Can be switched to RabbitMQ or others via configuration only
- See [messaging-configuration.md](messaging-configuration.md) for details

## Event Files
- [Order Events](order-events.md) - Order Service event contracts
- [Inventory Events](inventory-events.md) - Inventory Service event contracts
- [Notification Events](notification-events.md) - Notification Service event subscriptions

## Implementation Guides
- [Event Envelope Standard](event-envelope-standard.md) - Standard event structure
- [Idempotency Pattern](idempotency-pattern.md) - Idempotency implementation guide
- [Messaging Configuration](messaging-configuration.md) - Spring Cloud Stream setup

## Future Enhancements
- SAGA Pattern for distributed transactions (when needed)
- Event versioning strategy
- Schema evolution patterns
