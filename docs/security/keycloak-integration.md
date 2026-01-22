# Keycloak Integration Guide

**Version:** Keycloak 26.5.1  
**Realm:** `ecommerce`  
**Last Updated:** 2026-01-22

---

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Keycloak Installation](#keycloak-installation)
4. [Realm Configuration](#realm-configuration)
5. [Client Setup](#client-setup)
6. [Operation Claims Configuration](#operation-claims-configuration)
7. [User Management](#user-management)
8. [Token Configuration](#token-configuration)
9. [Testing](#testing)
10. [Troubleshooting](#troubleshooting)

---

## Overview

This guide provides step-by-step instructions for integrating Keycloak 26.5.1 with the e-commerce microservices platform. We use **OAuth2/OpenID Connect** for authentication and **operation-based claims** for fine-grained authorization.

### Architecture

```
Client → Gateway (8080) → Keycloak (8180) → JWT Token
                ↓
         Backend Services (8081, 8082)
         (Validate JWT + Extract Claims)
```

### Key Concepts

- **Realm**: Isolated namespace for users, clients, and roles (`ecommerce`)
- **Client**: Application that uses Keycloak (gateway, services)
- **Operation Claims**: Fine-grained permissions (e.g., `order.create`)
- **Composite Roles**: Groups of claims (e.g., `Customer`, `Admin`)
- **JWT Token**: Bearer token containing user identity and claims

---

## Prerequisites

- Docker & Docker Compose installed
- Java 21 installed
- Maven installed
- Access to ports: 8080, 8081, 8082, 8180, 29023

---

## Keycloak Installation

### 1. Docker Compose Setup

Create `docker-compose.yml` in project root:

```yaml
version: '3.8'

services:
  keycloak:
    image: quay.io/keycloak/keycloak:26.5.1
    container_name: ecommerce-keycloak
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://keycloak-postgres:5432/keycloak
      KC_DB_USERNAME: keycloak
      KC_DB_PASSWORD: keycloak
      KC_HOSTNAME: localhost
      KC_HTTP_PORT: 8180
    ports:
      - "8180:8180"
    command:
      - start-dev
      - --http-port=8180
    depends_on:
      - keycloak-postgres
    networks:
      - ecommerce-network

  keycloak-postgres:
    image: postgres:16-alpine
    container_name: keycloak-postgres
    environment:
      POSTGRES_DB: keycloak
      POSTGRES_USER: keycloak
      POSTGRES_PASSWORD: keycloak
    volumes:
      - keycloak-postgres-data:/var/lib/postgresql/data
    networks:
      - ecommerce-network

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    networks:
      - ecommerce-network

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: kafka
    depends_on:
      - zookeeper
    ports:
      - "29023:29023"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:29023
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    networks:
      - ecommerce-network

volumes:
  keycloak-postgres-data:

networks:
  ecommerce-network:
    driver: bridge
```

### 2. Start Services

```bash
docker-compose up -d
```

### 3. Access Admin Console

- **URL**: http://localhost:8180
- **Username**: `admin`
- **Password**: `admin`

Wait ~30 seconds for Keycloak to fully start.

---

## Realm Configuration

### Step 1: Create Realm

1. Navigate to http://localhost:8180
2. Login with admin credentials
3. Click dropdown in top-left corner (currently shows "master")
4. Click **"Create Realm"**
5. Fill in:
   - **Realm name**: `ecommerce`
   - **Enabled**: `ON`
6. Click **"Create"**

**Result**: `ecommerce` realm is now active.

---

## Client Setup

### Client 1: Gateway Service

**Purpose**: Frontend/API Gateway that handles user authentication

1. Navigate to **Clients** → **Create client**
2. **General Settings**:
   - Client type: `OpenID Connect`
   - Client ID: `gateway-client`
   - Name: `Gateway Service Client`
   - Click **Next**
3. **Capability config**:
   - Client authentication: `ON`
   - Authorization: `OFF`
   - Standard flow: `ON` ✓
   - Direct access grants: `ON` ✓
   - Service accounts roles: `OFF`
   - Click **Next**
4. **Login settings**:
   - Root URL: `http://localhost:8080`
   - Valid redirect URIs: 
     - `http://localhost:8080/*`
     - `http://localhost:8080/login/oauth2/code/*`
   - Valid post logout redirect URIs: `http://localhost:8080/*`
   - Web origins: `http://localhost:8080`
   - Click **Save**
5. Go to **Credentials** tab
6. **Copy** the Client Secret (e.g., `abc123xyz...`)
   - ⚠️ Save this value - needed for Spring configuration

---

### Client 2: Order Service

**Purpose**: Resource server for order management

1. **Clients** → **Create client**
2. Client ID: `order-service`
3. Client authentication: `ON`
4. Service accounts roles: `ON` ✓ (for service-to-service)
5. Valid redirect URIs: `http://localhost:8081/*`
6. Click **Save**
7. **Credentials** tab → Copy client secret

---

### Client 3: Inventory Service

**Purpose**: Resource server for inventory management

1. **Clients** → **Create client**
2. Client ID: `inventory-service`
3. Client authentication: `ON`
4. Service accounts roles: `ON` ✓
5. Valid redirect URIs: `http://localhost:8082/*`
6. Click **Save**
7. **Credentials** tab → Copy client secret

---

## Operation Claims Configuration

See [operation-claims-guide.md](operation-claims-guide.md) for detailed claim definitions.

### Step 1: Create Client Roles (Order Service)

1. Navigate to **Clients** → **order-service**
2. Click **Roles** tab
3. Click **Create role**
4. Create the following roles one by one:

| Role Name | Description |
|-----------|-------------|
| `order.create` | Create new orders |
| `order.read` | Read order details (any) |
| `order.read.own` | Read own orders only |
| `order.read.all` | Read all orders (admin) |
| `order.update` | Update order information |
| `order.cancel` | Cancel orders |
| `order.delete` | Delete orders (admin only) |
| `order.status.update` | Update order status |

For each role:
- Enter role name
- Enter description
- Click **Save**

---

### Step 2: Create Client Roles (Inventory Service)

1. Navigate to **Clients** → **inventory-service**
2. Click **Roles** tab
3. Create the following roles:

| Role Name | Description |
|-----------|-------------|
| `inventory.read` | Read inventory data |
| `inventory.write` | Update inventory data |
| `inventory.create` | Create inventory items |
| `inventory.delete` | Delete inventory items |
| `reservation.create` | Create stock reservations |
| `reservation.read` | Read reservations |
| `reservation.confirm` | Confirm reservations |
| `reservation.cancel` | Cancel reservations |

---

### Step 3: Create Composite Roles (Realm Level)

#### Customer Role

1. Navigate to **Realm roles** → **Create role**
2. Role name: `Customer`
3. Description: `Standard customer permissions`
4. Click **Save**
5. Click **Composite** toggle: `ON`
6. Click **Associated roles** tab
7. Click **Assign role**
8. Filter by clients → Select `order-service`:
   - ✓ `order.create`
   - ✓ `order.read.own`
   - ✓ `order.cancel`
9. Filter by clients → Select `inventory-service`:
   - ✓ `inventory.read`
   - ✓ `reservation.create`
10. Click **Assign**

#### Admin Role

1. **Realm roles** → **Create role**
2. Role name: `Admin`
3. Description: `Full system administrator`
4. Composite: `ON`
5. **Associated roles** → Assign ALL roles from:
   - `order-service` (all order.* claims)
   - `inventory-service` (all inventory.* and reservation.* claims)

#### InventoryManager Role

1. **Realm roles** → **Create role**
2. Role name: `InventoryManager`
3. Composite: `ON`
4. Associated roles:
   - `inventory.read`
   - `inventory.write`
   - `inventory.create`
   - `reservation.read`
   - `reservation.confirm`
   - `reservation.cancel`

#### OrderManager Role

1. **Realm roles** → **Create role**
2. Role name: `OrderManager`
3. Composite: `ON`
4. Associated roles:
   - `order.read.all`
   - `order.update`
   - `order.status.update`
   - `order.cancel`

---

### Step 4: Configure Client Scope Mappers

These mappers add client roles to JWT tokens as custom claims.

#### Order Service Mapper

1. **Clients** → **order-service** → **Client scopes** tab
2. Click on `order-service-dedicated` (default scope)
3. Click **Add mapper** → **By configuration**
4. Select **User Client Role**
5. Configure:
   - Name: `order-service-roles`
   - Client ID: `order-service`
   - Token Claim Name: `order_claims`
   - Claim JSON Type: `String`
   - Add to ID token: `ON`
   - Add to access token: `ON`
   - Add to userinfo: `ON`
6. Click **Save**

#### Inventory Service Mapper

1. **Clients** → **inventory-service** → **Client scopes** tab
2. Click on `inventory-service-dedicated`
3. **Add mapper** → **User Client Role**
4. Configure:
   - Name: `inventory-service-roles`
   - Client ID: `inventory-service`
   - Token Claim Name: `inventory_claims`
   - Claim JSON Type: `String`
   - Add to ID token: `ON`
   - Add to access token: `ON`
   - Add to userinfo: `ON`
5. Click **Save**

---

## User Management

### Admin User

1. **Users** → **Create new user**
2. Fill in:
   - Username: `admin`
   - Email: `admin@ecommerce.com`
   - Email verified: `ON`
   - First name: `Admin`
   - Last name: `User`
3. Click **Create**
4. Go to **Credentials** tab:
   - Click **Set password**
   - Password: `admin123`
   - Temporary: `OFF`
   - Click **Save**
5. Go to **Role mapping** tab:
   - Click **Assign role**
   - Select `Admin` (composite role)
   - Click **Assign**

---

### Customer User

1. **Users** → **Create new user**
2. Fill in:
   - Username: `customer1`
   - Email: `customer1@test.com`
   - Email verified: `ON`
   - First name: `John`
   - Last name: `Doe`
3. Click **Create**
4. **Credentials** tab:
   - Password: `customer123`
   - Temporary: `OFF`
5. **Role mapping** tab:
   - Assign role: `Customer`

---

### Inventory Manager User

1. **Users** → **Create new user**
2. Username: `inventory_manager`
3. Email: `inventory@ecommerce.com`
4. Email verified: `ON`
5. **Credentials**: `inventory123`
6. **Role mapping**: `InventoryManager`

---

### Order Manager User

1. **Users** → **Create new user**
2. Username: `order_manager`
3. Email: `orders@ecommerce.com`
4. Email verified: `ON`
5. **Credentials**: `orders123`
6. **Role mapping**: `OrderManager`

---

## Token Configuration

### Token Lifespan Settings

1. Navigate to **Realm settings** → **Tokens** tab
2. Configure:
   - **Access Token Lifespan**: `15 Minutes`
   - **Access Token Lifespan For Implicit Flow**: `15 Minutes`
   - **Client login timeout**: `5 Minutes`
   - **Login timeout**: `30 Minutes`
   - **Login action timeout**: `5 Minutes`
   - **Refresh Token Max Reuse**: `0`
   - **SSO Session Idle**: `30 Minutes`
   - **SSO Session Max**: `10 Hours`
3. Click **Save**

---

## Testing

### Test 1: Get Access Token (Password Grant)

```bash
curl -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'username=customer1' \
  -d 'password=customer123'
```

**Expected Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "session_state": "uuid",
  "scope": "openid profile email"
}
```

---

### Test 2: Decode JWT Token

Use https://jwt.io to decode the access token.

**Expected Claims:**
```json
{
  "sub": "user-uuid",
  "preferred_username": "customer1",
  "email": "customer1@test.com",
  "realm_access": {
    "roles": ["Customer"]
  },
  "resource_access": {
    "order-service": {
      "roles": ["order.create", "order.read.own", "order.cancel"]
    },
    "inventory-service": {
      "roles": ["inventory.read", "reservation.create"]
    }
  },
  "order_claims": ["order.create", "order.read.own", "order.cancel"],
  "inventory_claims": ["inventory.read", "reservation.create"]
}
```

---

### Test 3: API Call with Token

```bash
curl -X POST 'http://localhost:8080/api/v1/orders' \
  -H 'Authorization: Bearer YOUR_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "cust-123",
    "deliveryAddress": {
      "street": "123 Main St",
      "city": "Istanbul",
      "zipCode": "34000",
      "country": "Turkey"
    },
    "items": [
      {
        "productId": "prod-1",
        "quantity": 2,
        "unitPrice": 100.00
      }
    ]
  }'
```

**Expected**: 201 Created (if user has `order.create` claim)

---

## Troubleshooting

### Issue: "Client not found" error

**Solution**: Verify client ID exactly matches (`gateway-client`, not `gateway_client`)

---

### Issue: Token doesn't contain custom claims

**Solution**: 
1. Check client scope mappers are configured correctly
2. Verify mapper is added to dedicated scope (not default)
3. Test token at http://localhost:8180/realms/ecommerce/protocol/openid-connect/token

---

### Issue: 403 Forbidden on API calls

**Solution**:
1. Decode JWT token at jwt.io
2. Verify required claim exists (e.g., `order.create`)
3. Check Spring Security logs for authorization errors
4. Verify `JwtClaimsConverter` extracts claims correctly

---

### Issue: Keycloak won't start

**Solution**:
```bash
# Check logs
docker logs ecommerce-keycloak

# Restart services
docker-compose down
docker-compose up -d
```

---

### Issue: Invalid signature error

**Solution**: Verify `issuer-uri` matches exactly in Spring config:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/ecommerce
```

---

## Additional Resources

- [Operation Claims Guide](operation-claims-guide.md)
- [JWT Token Structure](jwt-token-structure.md)
- [Security Testing Guide](security-testing-guide.md)
- [Keycloak Documentation](https://www.keycloak.org/docs/26.5.1/)
- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/)

---

**Next Steps**: After Keycloak setup, proceed to Spring Boot integration:
1. Update service dependencies (see AGENTS.md Section 7)
2. Configure `application.yml` with issuer-uri and client secrets
3. Implement `SecurityConfig.java` and `JwtClaimsConverter.java`
4. Add `@PreAuthorize` annotations to controllers
