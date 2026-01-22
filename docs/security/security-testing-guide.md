# Security Testing Guide

**Last Updated:** 2026-01-22

---

## Overview

This guide provides comprehensive test scenarios for validating the Keycloak integration and operation-based claims authorization in our microservices platform.

---

## Table of Contents

1. [Setup](#setup)
2. [Token Acquisition Tests](#token-acquisition-tests)
3. [Authorization Tests](#authorization-tests)
4. [Integration Test Examples](#integration-test-examples)
5. [Test Matrix](#test-matrix)
6. [Automated Testing](#automated-testing)

---

## Setup

### Prerequisites

- Keycloak running on http://localhost:8180
- Gateway running on http://localhost:8080
- Order Service running on http://localhost:8081
- Inventory Service running on http://localhost:8082
- Test users created (customer1, admin, inventory_manager)

### Tools

- **cURL**: Command-line HTTP client
- **Postman**: GUI for API testing
- **jq**: JSON processor (optional, for parsing)

---

## Token Acquisition Tests

### Test 1: Customer Token (Password Grant)

**Objective**: Verify customer can obtain access token

```bash
curl -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'username=customer1' \
  -d 'password=customer123'
```

**Expected Response** (200 OK):
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6...",
  "expires_in": 900,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "uuid",
  "scope": "openid profile email"
}
```

**Validation**:
- ✓ Status code is 200
- ✓ `access_token` is present
- ✓ `token_type` is "Bearer"
- ✓ `expires_in` is 900 (15 minutes)

---

### Test 2: Admin Token

```bash
curl -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'username=admin' \
  -d 'password=admin123'
```

**Expected**: Same structure as Test 1

---

### Test 3: Invalid Credentials

```bash
curl -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=password' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'username=customer1' \
  -d 'password=WRONG_PASSWORD'
```

**Expected Response** (401 Unauthorized):
```json
{
  "error": "invalid_grant",
  "error_description": "Invalid user credentials"
}
```

---

### Test 4: Token Refresh

**Step 1**: Obtain initial tokens (use Test 1)

**Step 2**: Refresh using refresh token

```bash
curl -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=refresh_token' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_CLIENT_SECRET' \
  -d 'refresh_token=YOUR_REFRESH_TOKEN'
```

**Expected**: New access_token and refresh_token

---

## Authorization Tests

### Customer Tests

#### Test 5: Customer Creates Order (Allowed)

**Claim Required**: `order.create`  
**Expected Result**: ✅ Success (201 Created)

```bash
# Get token
TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -d 'grant_type=password' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_SECRET' \
  -d 'username=customer1' \
  -d 'password=customer123' | jq -r '.access_token')

# Create order
curl -X POST 'http://localhost:8080/api/v1/orders' \
  -H "Authorization: Bearer $TOKEN" \
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
        "productId": "550e8400-e29b-41d4-a716-446655440001",
        "quantity": 2,
        "unitPrice": 100.00
      }
    ]
  }'
```

**Expected**: 201 Created with order response

---

#### Test 6: Customer Updates Order Status (Denied)

**Claim Required**: `order.status.update`  
**Expected Result**: ❌ 403 Forbidden

```bash
curl -X PATCH "http://localhost:8080/api/v1/orders/{orderId}/status" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "SHIPPED"
  }'
```

**Expected Response** (403 Forbidden):
```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access Denied",
  "instance": "/api/v1/orders/{id}/status"
}
```

---

#### Test 7: Customer Reads Own Order (Allowed)

**Claim Required**: `order.read.own`  
**Expected Result**: ✅ Success (200 OK)

```bash
# Create order first (Test 5), capture order ID
ORDER_ID="captured-from-test-5"

# Read order
curl -X GET "http://localhost:8080/api/v1/orders/$ORDER_ID" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected**: 200 OK with order details

---

#### Test 8: Customer Reads Other User's Order (Denied)

**Expected Result**: ❌ 403 Forbidden

```bash
# Use order ID belonging to different user
OTHER_ORDER_ID="other-user-order-id"

curl -X GET "http://localhost:8080/api/v1/orders/$OTHER_ORDER_ID" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected**: 403 Forbidden (ownership check fails)

---

#### Test 9: Customer Checks Inventory (Allowed)

**Claim Required**: `inventory.read`  
**Expected Result**: ✅ Success (200 OK)

```bash
curl -X POST 'http://localhost:8080/api/v1/inventory/check-availability' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "items": [
      {
        "productId": "550e8400-e29b-41d4-a716-446655440001",
        "requestedQuantity": 5
      }
    ]
  }'
```

**Expected**: 200 OK with availability result

---

#### Test 10: Customer Deletes Inventory (Denied)

**Claim Required**: `inventory.delete`  
**Expected Result**: ❌ 403 Forbidden

```bash
curl -X DELETE "http://localhost:8080/api/v1/inventory/items/{productId}" \
  -H "Authorization: Bearer $TOKEN"
```

**Expected**: 403 Forbidden

---

### Admin Tests

#### Test 11: Admin Reads All Orders (Allowed)

**Claim Required**: `order.read.all`  
**Expected Result**: ✅ Success (200 OK)

```bash
# Get admin token
ADMIN_TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -d 'grant_type=password' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_SECRET' \
  -d 'username=admin' \
  -d 'password=admin123' | jq -r '.access_token')

# Read any order
curl -X GET "http://localhost:8080/api/v1/orders/$ANY_ORDER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected**: 200 OK (no ownership restriction)

---

#### Test 12: Admin Updates Order Status (Allowed)

**Claim Required**: `order.status.update`  
**Expected Result**: ✅ Success (200 OK)

```bash
curl -X PATCH "http://localhost:8080/api/v1/orders/{orderId}/status" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "status": "SHIPPED"
  }'
```

**Expected**: 200 OK with updated order

---

#### Test 13: Admin Deletes Order (Allowed)

**Claim Required**: `order.delete`  
**Expected Result**: ✅ Success (204 No Content)

```bash
curl -X DELETE "http://localhost:8080/api/v1/orders/{orderId}" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Expected**: 204 No Content

---

### Inventory Manager Tests

#### Test 14: Inventory Manager Updates Stock (Allowed)

**Claim Required**: `inventory.write`  
**Expected Result**: ✅ Success (200 OK)

```bash
# Get inventory manager token
INV_TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -d 'grant_type=password' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_SECRET' \
  -d 'username=inventory_manager' \
  -d 'password=inventory123' | jq -r '.access_token')

# Update inventory
curl -X PUT "http://localhost:8080/api/v1/inventory/items/{productId}" \
  -H "Authorization: Bearer $INV_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "availableQuantity": 100
  }'
```

**Expected**: 200 OK

---

#### Test 15: Inventory Manager Creates Order (Denied)

**Claim Required**: `order.create`  
**Expected Result**: ❌ 403 Forbidden

```bash
curl -X POST 'http://localhost:8080/api/v1/orders' \
  -H "Authorization: Bearer $INV_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{ "customerId": "cust-123", ... }'
```

**Expected**: 403 Forbidden

---

### Unauthenticated Tests

#### Test 16: No Token (Denied)

**Expected Result**: ❌ 401 Unauthorized

```bash
curl -X GET 'http://localhost:8080/api/v1/orders/{orderId}'
```

**Expected Response**:
```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Full authentication is required to access this resource"
}
```

---

#### Test 17: Invalid Token (Denied)

```bash
curl -X GET 'http://localhost:8080/api/v1/orders/{orderId}' \
  -H 'Authorization: Bearer INVALID_TOKEN_HERE'
```

**Expected**: 401 Unauthorized

---

#### Test 18: Expired Token (Denied)

**Steps**:
1. Obtain token
2. Wait 16 minutes (access token expires)
3. Use expired token

```bash
curl -X GET 'http://localhost:8080/api/v1/orders/{orderId}' \
  -H "Authorization: Bearer $EXPIRED_TOKEN"
```

**Expected**: 401 Unauthorized with "Token expired" message

---

## Integration Test Examples

### Spring Boot Test (Order Service)

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class OrderControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockJwt(claims = {"order.create"})
    void createOrder_withValidClaim_shouldSucceed() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "customerId": "cust-123",
                      "deliveryAddress": {...},
                      "items": [...]
                    }
                    """))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockJwt(claims = {}) // No order.create claim
    void createOrder_withoutClaim_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("..."))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_withoutAuthentication_shouldFail() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("..."))
                .andExpect(status().isUnauthorized());
    }
}
```

---

### Custom Annotation for Testing

```java
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockJwtSecurityContextFactory.class)
public @interface WithMockJwt {
    String subject() default "test-user";
    String username() default "testuser";
    String[] claims() default {};
}
```

---

## Test Matrix

### Order Endpoints

| Endpoint | Method | Customer | Admin | OrderManager | InventoryManager | Unauthenticated |
|----------|--------|----------|-------|--------------|------------------|-----------------|
| `/orders` | POST | ✅ (201) | ✅ (201) | ✅ (201) | ❌ (403) | ❌ (401) |
| `/orders/{id}` | GET | ✅ (own) | ✅ (all) | ✅ (all) | ❌ (403) | ❌ (401) |
| `/orders/{id}/cancel` | POST | ✅ (own) | ✅ (all) | ✅ (all) | ❌ (403) | ❌ (401) |
| `/orders/{id}/status` | PATCH | ❌ (403) | ✅ (200) | ✅ (200) | ❌ (403) | ❌ (401) |
| `/orders/{id}` | DELETE | ❌ (403) | ✅ (204) | ❌ (403) | ❌ (403) | ❌ (401) |

---

### Inventory Endpoints

| Endpoint | Method | Customer | Admin | OrderManager | InventoryManager | Unauthenticated |
|----------|--------|----------|-------|--------------|------------------|-----------------|
| `/inventory/items/{id}` | GET | ✅ (200) | ✅ (200) | ✅ (200) | ✅ (200) | ❌ (401) |
| `/inventory/check-availability` | POST | ✅ (200) | ✅ (200) | ✅ (200) | ✅ (200) | ❌ (401) |
| `/inventory/items/{id}` | PUT | ❌ (403) | ✅ (200) | ❌ (403) | ✅ (200) | ❌ (401) |
| `/inventory/items` | POST | ❌ (403) | ✅ (201) | ❌ (403) | ✅ (201) | ❌ (401) |
| `/inventory/items/{id}` | DELETE | ❌ (403) | ✅ (204) | ❌ (403) | ❌ (403) | ❌ (401) |
| `/reservations` | POST | ✅ (201) | ✅ (201) | ✅ (201) | ❌ (403) | ❌ (401) |
| `/reservations/{id}` | GET | ❌ (403) | ✅ (200) | ✅ (200) | ✅ (200) | ❌ (401) |
| `/reservations/{id}/confirm` | POST | ❌ (403) | ✅ (200) | ❌ (403) | ✅ (200) | ❌ (401) |
| `/reservations/{id}/cancel` | POST | ❌ (403) | ✅ (204) | ✅ (204) | ✅ (204) | ❌ (401) |

---

## Automated Testing

### Postman Collection Structure

```
E-Commerce Security Tests/
├── Setup/
│   ├── Get Customer Token
│   ├── Get Admin Token
│   ├── Get Inventory Manager Token
│   └── Get Order Manager Token
├── Customer Tests/
│   ├── [Positive] Create Order
│   ├── [Positive] Read Own Order
│   ├── [Negative] Update Order Status
│   └── [Negative] Delete Order
├── Admin Tests/
│   ├── [Positive] Read All Orders
│   ├── [Positive] Update Order Status
│   └── [Positive] Delete Order
├── Inventory Manager Tests/
│   ├── [Positive] Update Inventory
│   ├── [Positive] Confirm Reservation
│   └── [Negative] Create Order
└── Unauthenticated Tests/
    ├── [Negative] No Token
    └── [Negative] Invalid Token
```

---

### CI/CD Integration

Add to GitHub Actions / GitLab CI:

```yaml
test-security:
  stage: test
  script:
    - docker-compose up -d keycloak
    - sleep 30  # Wait for Keycloak
    - ./mvnw test -Dtest="*SecurityTest"
  dependencies:
    - build
```

---

## Troubleshooting Tests

### Issue: All tests return 401

**Cause**: Keycloak not running or wrong issuer-uri

**Solution**:
```bash
# Check Keycloak status
curl http://localhost:8180/realms/ecommerce/.well-known/openid-configuration

# Verify issuer-uri in application.yml
```

---

### Issue: Token obtained but API returns 403

**Cause**: Missing claim in token

**Solution**:
1. Decode token at jwt.io
2. Check for required claim (e.g., `order.create`)
3. Verify user has correct composite role in Keycloak
4. Check client scope mappers

---

### Issue: Tests pass locally but fail in CI

**Cause**: Timing issue (Keycloak not ready)

**Solution**: Add health check wait:
```bash
until curl -f http://localhost:8180/health; do sleep 5; done
```

---

## Test Coverage Goals

- ✅ All endpoints tested with valid claims
- ✅ All endpoints tested with missing claims (negative)
- ✅ All endpoints tested without authentication
- ✅ Token refresh flow tested
- ✅ Expired token handling tested
- ✅ Ownership checks tested (for *.own claims)

**Target**: 100% security test coverage for all protected endpoints

---

## Related Documentation

- [Operation Claims Guide](operation-claims-guide.md)
- [Keycloak Integration Guide](keycloak-integration.md)
- [JWT Token Structure](jwt-token-structure.md)

---

**Maintained by**: Platform Team  
**Review**: After each sprint or security change
