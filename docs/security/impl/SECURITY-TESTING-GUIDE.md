# Security Testing & Validation Guide

**Testing Date:** January 22, 2026  
**Keycloak URL:** http://localhost:8181  
**Gateway URL:** http://localhost:8080  
**Services:** Order (8081), Inventory (8082)

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Test User Credentials](#test-user-credentials)
3. [Token Acquisition](#token-acquisition)
4. [Test Scenarios](#test-scenarios)
5. [Authorization Matrix Testing](#authorization-matrix-testing)
6. [Postman Collection Setup](#postman-collection-setup)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### 1. Start All Services

```powershell
# Terminal 1: Order Service
cd order-service
mvn spring-boot:run

# Terminal 2: Inventory Service
cd inventory-service
mvn spring-boot:run

# Terminal 3: Gateway Service
cd gateway-service
mvn spring-boot:run
```

### 2. Set Environment Variables

```powershell
# Set client secret (replace with actual secret from Keycloak)
$env:KEYCLOAK_CLIENT_SECRET="<your-gateway-client-secret>"
```

### 3. Verify Services Health

```powershell
# Gateway
curl http://localhost:8080/actuator/health

# Order Service
curl http://localhost:8081/actuator/health

# Inventory Service
curl http://localhost:8082/actuator/health
```

All should return: `{"status":"UP"}`

---

## Test User Credentials

| Username | Password | Role | Permissions |
|----------|----------|------|-------------|
| `customer1` | `password123` | Customer | order.create, order.read.own, order.cancel, order.history.view |
| `customer2` | `password123` | Customer | order.create, order.read.own, order.cancel, order.history.view |
| `admin` | `admin123` | Admin | ALL permissions (order + inventory) |
| `order_manager` | `password123` | OrderManager | All order permissions + reservation operations |
| `inventory_manager` | `password123` | InventoryManager | All inventory permissions + reservations |

---

## Token Acquisition

### Method 1: Direct Token Request (For Testing)

```powershell
# Get token for customer1
$response = curl -X POST "http://localhost:8181/realms/ecommerce/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password" `
  -d "client_id=order-service" `
  -d "client_secret=<ORDER_SERVICE_CLIENT_SECRET>" `
  -d "username=customer1" `
  -d "password=password123" `
  -d "scope=openid profile email"

# Parse response to extract token (PowerShell)
$responseJson = $response | ConvertFrom-Json
$TOKEN = $responseJson.access_token
echo $TOKEN
```

### Method 2: Authorization Code Flow (Via Browser)

1. Open browser: http://localhost:8080/api/v1/orders
2. Redirected to Keycloak login page
3. Login with: `customer1` / `password123`
4. Redirected back to Gateway with token
5. Gateway forwards token to backend services

---

## Test Scenarios

### Scenario 1: Unauthenticated Access (Expect 401)

```powershell
# Try to access orders without token
curl -X GET "http://localhost:8080/api/v1/orders/00000000-0000-0000-0000-000000000001"

# Expected Response: 401 Unauthorized
```

### Scenario 2: Customer Creates Order (Expect 201)

```powershell
# Get token for customer1
$TOKEN_CUSTOMER1 = "<get-token-as-shown-above>"

# Create order
curl -X POST "http://localhost:8080/api/v1/orders" `
  -H "Authorization: Bearer $TOKEN_CUSTOMER1" `
  -H "Content-Type: application/json" `
  -d '{
    "customerId": "c7e1d5e8-1a2b-4c5d-8e9f-1a2b3c4d5e6f",
    "address": {
      "street": "123 Main St",
      "city": "Springfield",
      "state": "IL",
      "postalCode": "62701",
      "country": "USA"
    },
    "items": [
      {
        "productId": "p1",
        "productName": "Widget",
        "quantity": 2,
        "unitPrice": 29.99
      }
    ]
  }'

# Expected Response: 201 Created
# Save the returned order ID for next tests
$ORDER_ID = "<copy-id-from-response>"
```

### Scenario 3: Customer Reads Own Order (Expect 200)

```powershell
# Customer1 reads their own order
curl -X GET "http://localhost:8080/api/v1/orders/$ORDER_ID" `
  -H "Authorization: Bearer $TOKEN_CUSTOMER1"

# Expected Response: 200 OK with order details
```

### Scenario 4: Customer Tries to Read Inventory (Expect 403)

```powershell
# Customer tries to access inventory (lacks inventory.read claim)
curl -X GET "http://localhost:8080/api/v1/inventory/items/00000000-0000-0000-0000-000000000001" `
  -H "Authorization: Bearer $TOKEN_CUSTOMER1"

# Expected Response: 403 Forbidden
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access Denied",
  "instance": "/api/v1/inventory/items/00000000-0000-0000-0000-000000000001"
}
```

### Scenario 5: Admin Reads All Orders (Expect 200)

```powershell
# Get admin token
$TOKEN_ADMIN = "<get-admin-token>"

# Admin reads ANY order (has order.read.all)
curl -X GET "http://localhost:8080/api/v1/orders/$ORDER_ID" `
  -H "Authorization: Bearer $TOKEN_ADMIN"

# Expected Response: 200 OK
```

### Scenario 6: Admin Manages Inventory (Expect 200)

```powershell
# Admin reads inventory (has inventory.read)
curl -X GET "http://localhost:8080/api/v1/inventory/items/00000000-0000-0000-0000-000000000001" `
  -H "Authorization: Bearer $TOKEN_ADMIN"

# Expected Response: 200 OK with inventory details
```

### Scenario 7: OrderManager Updates Order Status (Expect 200)

```powershell
# Get order_manager token
$TOKEN_ORDER_MGR = "<get-order-manager-token>"

# Update order status (has order.status.update)
curl -X PATCH "http://localhost:8080/api/v1/orders/$ORDER_ID/status" `
  -H "Authorization: Bearer $TOKEN_ORDER_MGR" `
  -H "Content-Type: application/json" `
  -d '{"status": "PROCESSING"}'

# Expected Response: 200 OK
```

### Scenario 8: InventoryManager Reads Inventory (Expect 200)

```powershell
# Get inventory_manager token
$TOKEN_INV_MGR = "<get-inventory-manager-token>"

# Read inventory items
curl -X GET "http://localhost:8080/api/v1/inventory/items/00000000-0000-0000-0000-000000000001" `
  -H "Authorization: Bearer $TOKEN_INV_MGR"

# Expected Response: 200 OK
```

### Scenario 9: InventoryManager Cannot Read Orders (Expect 403)

```powershell
# Inventory manager tries to read orders (lacks order.read claims)
curl -X GET "http://localhost:8080/api/v1/orders/$ORDER_ID" `
  -H "Authorization: Bearer $TOKEN_INV_MGR"

# Expected Response: 403 Forbidden
```

### Scenario 10: Customer Cancels Order (Expect 200)

```powershell
# Customer cancels their own order (has order.cancel)
curl -X DELETE "http://localhost:8080/api/v1/orders/$ORDER_ID" `
  -H "Authorization: Bearer $TOKEN_CUSTOMER1"

# Expected Response: 200 OK with cancellation message
```

### Scenario 11: Expired Token (Expect 401)

```powershell
# Wait 15 minutes (token expires)
# Try to use expired token
curl -X GET "http://localhost:8080/api/v1/orders/$ORDER_ID" `
  -H "Authorization: Bearer $EXPIRED_TOKEN"

# Expected Response: 401 Unauthorized
# Error: "invalid_token" or "token_expired"
```

### Scenario 12: Correlation ID Tracing

```powershell
# Send request with custom correlation ID
curl -X GET "http://localhost:8080/api/v1/orders/$ORDER_ID" `
  -H "Authorization: Bearer $TOKEN_CUSTOMER1" `
  -H "X-Correlation-Id: test-12345"

# Check Gateway logs for: "Correlation-Id: test-12345"
# Check Order Service logs for same correlation ID
```

---

## Authorization Matrix Testing

Test all endpoint-role combinations:

### Order Service Endpoints

| Endpoint | Method | Customer | Admin | OrderManager | InventoryManager |
|----------|--------|----------|-------|--------------|------------------|
| POST /api/v1/orders | POST | ✅ 201 | ✅ 201 | ✅ 201 | ❌ 403 |
| GET /api/v1/orders/{id} | GET | ✅ 200 (own) | ✅ 200 (all) | ✅ 200 (all) | ❌ 403 |
| DELETE /api/v1/orders/{id} | DELETE | ❌ 403 | ✅ 200 | ✅ 200 | ❌ 403 |
| PATCH /api/v1/orders/{id}/status | PATCH | ❌ 403 | ✅ 200 | ✅ 200 | ❌ 403 |

### Inventory Service Endpoints

| Endpoint | Method | Customer | Admin | OrderManager | InventoryManager |
|----------|--------|----------|-------|--------------|------------------|
| GET /api/v1/inventory/items/{id} | GET | ❌ 403 | ✅ 200 | ❌ 403 | ✅ 200 |
| POST /api/v1/inventory/reservations | POST | ❌ 403 | ✅ 201 | ✅ 201 | ✅ 201 |
| GET /api/v1/inventory/reservations/{id} | GET | ❌ 403 | ✅ 200 | ✅ 200 | ✅ 200 |
| PUT /api/v1/inventory/reservations/{id}/confirm | PUT | ❌ 403 | ✅ 204 | ✅ 204 | ✅ 204 |
| DELETE /api/v1/inventory/reservations/{id} | DELETE | ❌ 403 | ✅ 204 | ✅ 204 | ✅ 204 |

### Test Script (PowerShell)

```powershell
# Automated matrix testing script
$endpoints = @(
    @{Method="POST"; Url="/api/v1/orders"; Body='{"customerId":"test", "address":{...}, "items":[...]}'; Claim="order.create"},
    @{Method="GET"; Url="/api/v1/orders/$ORDER_ID"; Body=$null; Claim="order.read.own|order.read.all"},
    @{Method="GET"; Url="/api/v1/inventory/items/00000000-0000-0000-0000-000000000001"; Body=$null; Claim="inventory.read"}
)

$users = @("customer1", "admin", "order_manager", "inventory_manager")

foreach ($user in $users) {
    Write-Host "`n=== Testing as $user ===" -ForegroundColor Cyan
    
    # Get token for user
    $token = Get-Token -Username $user -Password "password123"
    
    foreach ($endpoint in $endpoints) {
        Write-Host "`nTesting: $($endpoint.Method) $($endpoint.Url)" -ForegroundColor Yellow
        
        $response = Invoke-WebRequest `
            -Uri "http://localhost:8080$($endpoint.Url)" `
            -Method $endpoint.Method `
            -Headers @{Authorization="Bearer $token"; "Content-Type"="application/json"} `
            -Body $endpoint.Body `
            -SkipHttpErrorCheck
        
        Write-Host "Status: $($response.StatusCode)" -ForegroundColor $(
            if ($response.StatusCode -ge 200 -and $response.StatusCode -lt 300) {"Green"} 
            elseif ($response.StatusCode -eq 403) {"Yellow"} 
            else {"Red"}
        )
    }
}
```

---

## Postman Collection Setup

### 1. Create Environment Variables

- `keycloak_url`: `http://localhost:8181`
- `gateway_url`: `http://localhost:8080`
- `client_id`: `order-service`
- `client_secret`: `<your-order-service-client-secret>`
- `username`: `customer1`
- `password`: `password123`
- `access_token`: (auto-populated)

### 2. Pre-Request Script (Collection Level)

```javascript
// Auto-refresh token before each request
const tokenUrl = pm.environment.get("keycloak_url") + "/realms/ecommerce/protocol/openid-connect/token";

pm.sendRequest({
    url: tokenUrl,
    method: 'POST',
    header: {
        'Content-Type': 'application/x-www-form-urlencoded'
    },
    body: {
        mode: 'urlencoded',
        urlencoded: [
            {key: 'grant_type', value: 'password'},
            {key: 'client_id', value: pm.environment.get("client_id")},
            {key: 'client_secret', value: pm.environment.get("client_secret")},
            {key: 'username', value: pm.environment.get("username")},
            {key: 'password', value: pm.environment.get("password")},
            {key: 'scope', value: 'openid profile email'}
        ]
    }
}, function (err, response) {
    if (!err) {
        const jsonData = response.json();
        pm.environment.set("access_token", jsonData.access_token);
        console.log("Token refreshed successfully");
    } else {
        console.error("Failed to get token:", err);
    }
});
```

### 3. Authorization Header (Request Level)

Add to all requests:
```
Authorization: Bearer {{access_token}}
```

### 4. Sample Request: Create Order

- **Method:** POST
- **URL:** `{{gateway_url}}/api/v1/orders`
- **Headers:** 
  - `Authorization: Bearer {{access_token}}`
  - `Content-Type: application/json`
- **Body:**
```json
{
  "customerId": "c7e1d5e8-1a2b-4c5d-8e9f-1a2b3c4d5e6f",
  "address": {
    "street": "123 Main St",
    "city": "Springfield",
    "state": "IL",
    "postalCode": "62701",
    "country": "USA"
  },
  "items": [
    {
      "productId": "p1",
      "productName": "Widget",
      "quantity": 2,
      "unitPrice": 29.99
    }
  ]
}
```

### 5. Tests Tab (Validate Response)

```javascript
pm.test("Status code is 201", function () {
    pm.response.to.have.status(201);
});

pm.test("Response has orderId", function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData.id).to.exist;
    pm.environment.set("order_id", jsonData.id);
});

pm.test("Correlation ID present", function () {
    pm.response.to.have.header("X-Correlation-Id");
});
```

---

## Troubleshooting

### Issue 1: 401 Unauthorized

**Symptoms:**
```json
{
  "error": "unauthorized",
  "error_description": "Full authentication is required to access this resource"
}
```

**Causes:**
1. Missing `Authorization` header
2. Token expired (15 min lifespan)
3. Invalid token format (missing "Bearer " prefix)

**Solutions:**
```powershell
# Verify token format
echo $TOKEN  # Should start with "eyJhbGci..."

# Get fresh token
$TOKEN = Get-NewToken

# Use correct header format
curl -H "Authorization: Bearer $TOKEN" ...
```

### Issue 2: 403 Forbidden

**Symptoms:**
```json
{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access Denied"
}
```

**Causes:**
1. User lacks required operation claim
2. Ownership check failed (customer accessing other user's order)

**Solutions:**
```powershell
# Decode token to verify claims
# Go to https://jwt.io and paste token

# Expected claims for customer1:
{
  "order_claims": ["order.create", "order.read.own", "order.cancel"]
}

# If claims missing, check Keycloak role assignment:
# Users → customer1 → Role mapping → Verify "Customer" role assigned
```

### Issue 3: CORS Error (Browser)

**Symptoms:**
```
Access to XMLHttpRequest blocked by CORS policy
```

**Cause:** Gateway CORS configuration missing

**Solution:**
Already configured in `gateway-service/src/main/resources/application-dev.yml`:
```yaml
spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins: "*"
```

### Issue 4: Connection Refused

**Symptoms:**
```
curl: (7) Failed to connect to localhost port 8180: Connection refused
```

**Causes:**
1. Keycloak not running on port 8181
2. Service not started
3. Wrong port number

**Solutions:**
```powershell
# Verify Keycloak
curl http://localhost:8181

# Verify Gateway
curl http://localhost:8080/actuator/health

# Check ports
netstat -ano | findstr "8181 8080 8081 8082"
```

### Issue 5: Correlation ID Not Forwarded

**Symptoms:** Gateway logs show correlation ID, but Order Service doesn't

**Cause:** Gateway filter not injecting header

**Solution:** Already implemented in `CorrelationIdFilter.java`

**Verification:**
```powershell
# Send request with custom correlation ID
curl -H "X-Correlation-Id: test-123" http://localhost:8080/api/v1/orders

# Check logs:
# Gateway: "Correlation-Id: test-123"
# Order Service: Should also show "test-123"
```

---

## Success Criteria Checklist

### Infrastructure
- [x] Keycloak running on port 8181
- [x] Realm `ecommerce` configured
- [x] All 22 operation claims created (11 per service)
- [x] 4 composite roles created
- [x] 5 test users created with correct roles
- [x] Token acquisition works for all users

### Gateway Service
- [x] OAuth2 dependencies added
- [x] Gateway authenticates via Keycloak
- [x] JWT tokens forwarded to backend (TokenRelay)
- [x] Correlation IDs injected
- [x] Health checks remain public

### Order Service
- [x] JWT validation works
- [x] `order_claims` extracted from JWT
- [x] All endpoints protected with @PreAuthorize
- [x] 401 returned for unauthenticated requests
- [x] 403 returned for insufficient permissions
- [x] User context extracted from JWT

### Inventory Service
- [x] JWT validation works
- [x] `inventory_claims` extracted from JWT
- [x] All endpoints protected with @PreAuthorize
- [x] 401 returned for unauthenticated requests
- [x] 403 returned for insufficient permissions

### Authorization Matrix
- [ ] Customer can create orders (201)
- [ ] Customer can read own orders (200)
- [ ] Customer CANNOT read other user's orders (403)
- [ ] Customer CANNOT access inventory (403)
- [ ] Admin can read all orders (200)
- [ ] Admin can manage inventory (200)
- [ ] OrderManager can manage orders (200)
- [ ] OrderManager CANNOT manage inventory (403)
- [ ] InventoryManager can manage inventory (200)
- [ ] InventoryManager CANNOT access orders (403)
- [ ] Correlation IDs present in all service logs
- [ ] Token expiration enforced (401 after 15 min)

---

## Next Steps After Testing

1. ✅ All tests passing
2. ➡️ **Performance Testing** (auth overhead measurement)
3. ➡️ **Load Testing** (concurrent users with token refresh)
4. ➡️ **Integration Tests** (automated test suite with Testcontainers)
5. ➡️ **Production Hardening** (rate limiting, DDoS protection)

---

**Testing Complete! ✅**

If all scenarios pass, the Keycloak integration is fully functional.
