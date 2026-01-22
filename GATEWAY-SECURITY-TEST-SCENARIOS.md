# Gateway Security Test Scenarios - 401 vs 403 vs 200

**Test Date:** January 22, 2026  
**Gateway URL:** http://localhost:8080  
**Keycloak URL:** http://localhost:8181  

---

## 🎯 Test Objective

Verify that Gateway enforces authentication and authorization:
- **401 Unauthorized** → No token provided
- **403 Forbidden** → Valid token but insufficient permissions (backend service rejects)
- **200 OK** → Valid token with correct permissions

---

## ✅ Prerequisites

### 1. Ensure Keycloak is Running
```powershell
curl http://localhost:8181/realms/ecommerce
```
Expected: JSON response with realm info

### 2. Ensure Gateway is Running
```powershell
curl http://localhost:8080/actuator/health
```
Expected: `{"status":"UP"}`

### 3. Ensure Order Service is Running (for backend tests)
```powershell
curl http://localhost:8081/actuator/health
```
Expected: `{"status":"UP"}`

---

## 🧪 Test Scenario 1: 401 Unauthorized (No Token)

**Objective:** Gateway rejects requests without authentication

### Test 1A: Access Order Service Without Token

```powershell
curl -X GET http://localhost:8080/api/v1/orders/00000000-0000-0000-0000-000000000001 -v
```

**Expected Response:**
```
< HTTP/1.1 401 Unauthorized
< Content-Type: application/json
< WWW-Authenticate: Bearer

{"error":"unauthorized","error_description":"Full authentication is required to access this resource"}
```

**Why 401?**
- No `Authorization` header provided
- Gateway's SecurityConfig requires `.anyExchange().authenticated()`
- Spring Security OAuth2 Resource Server returns 401

### Test 1B: Access Inventory Service Without Token

```powershell
curl -X GET http://localhost:8080/api/v1/inventory/items/00000000-0000-0000-0000-000000000001 -v
```

**Expected Response:**
```
< HTTP/1.1 401 Unauthorized
```

**Result:** ✅ Gateway blocks unauthenticated requests

---

## 🧪 Test Scenario 2: 200 OK (Valid Token)

**Objective:** Gateway allows authenticated requests with valid JWT token

### Step 1: Get Access Token

```powershell
# Get token for customer1 (has order.create, order.read.own, order.cancel)
$response = Invoke-RestMethod -Uri "http://localhost:8181/realms/ecommerce/protocol/openid-connect/token" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = "order-service"
    client_secret = "<YOUR_ORDER_SERVICE_CLIENT_SECRET>"
    username = "customer1"
    password = "password123"
    scope = "openid profile email"
  }

$TOKEN = $response.access_token
Write-Host "Token acquired: $($TOKEN.Substring(0,50))..."
```

### Test 2A: Access Order Service With Valid Token

```powershell
# Create an order (customer1 has order.create permission)
curl -X POST http://localhost:8080/api/v1/orders `
  -H "Authorization: Bearer $TOKEN" `
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
  }' -v
```

**Expected Response:**
```
< HTTP/1.1 201 Created
< Content-Type: application/json

{
  "id": "<order-uuid>",
  "customerId": "c7e1d5e8-1a2b-4c5d-8e9f-1a2b3c4d5e6f",
  "status": "PENDING",
  "totalAmount": 59.98,
  ...
}
```

**Why 200 OK (201 Created)?**
- Valid JWT token provided in `Authorization: Bearer` header
- Gateway validates JWT signature against Keycloak
- Gateway forwards token to Order Service via TokenRelay filter
- Order Service validates JWT and checks `order.create` claim
- customer1 has `order.create` permission → Request allowed

**Result:** ✅ Gateway forwards authenticated requests to backend

---

## 🧪 Test Scenario 3: 403 Forbidden (Valid Token, Insufficient Permissions)

**Objective:** Backend service rejects requests when user lacks required permission

### Test 3A: Customer Tries to Access Inventory (No inventory.read claim)

```powershell
# Use customer1 token (which has NO inventory permissions)
curl -X GET http://localhost:8080/api/v1/inventory/items/00000000-0000-0000-0000-000000000001 `
  -H "Authorization: Bearer $TOKEN" -v
```

**Expected Response:**
```
< HTTP/1.1 403 Forbidden
< Content-Type: application/json

{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access Denied",
  "instance": "/api/v1/inventory/items/00000000-0000-0000-0000-000000000001"
}
```

**Why 403 Forbidden?**
- JWT token is VALID (passes Gateway authentication)
- Gateway forwards token to Inventory Service
- Inventory Service checks for `inventory.read` claim
- customer1 does NOT have `inventory.read` claim
- Inventory Service's `@PreAuthorize("hasAuthority('inventory.read')")` rejects request
- Spring Security returns 403 Forbidden

**Result:** ✅ Backend service enforces authorization (operation-based claims)

### Test 3B: Customer Tries to Delete Order (No order.delete claim)

```powershell
# Try to delete an order (customer1 does NOT have order.delete permission)
curl -X DELETE http://localhost:8080/api/v1/orders/00000000-0000-0000-0000-000000000001 `
  -H "Authorization: Bearer $TOKEN" -v
```

**Expected Response:**
```
< HTTP/1.1 403 Forbidden
< Content-Type: application/json

{
  "type": "about:blank",
  "title": "Forbidden",
  "status": 403,
  "detail": "Access Denied"
}
```

**Why 403?**
- Token is valid (Gateway allows)
- Order Service checks for `order.delete` claim
- customer1 only has: `order.create`, `order.read.own`, `order.cancel`
- customer1 does NOT have `order.delete` → 403 Forbidden

**Result:** ✅ Fine-grained authorization works (customer can create but not delete)

---

## 🧪 Test Scenario 4: Admin Access (Has All Permissions)

**Objective:** Admin user can access all endpoints

### Step 1: Get Admin Token

```powershell
$adminResponse = Invoke-RestMethod -Uri "http://localhost:8181/realms/ecommerce/protocol/openid-connect/token" `
  -Method POST `
  -ContentType "application/x-www-form-urlencoded" `
  -Body @{
    grant_type = "password"
    client_id = "order-service"
    client_secret = "<YOUR_ORDER_SERVICE_CLIENT_SECRET>"
    username = "admin"
    password = "admin123"
    scope = "openid profile email"
  }

$ADMIN_TOKEN = $adminResponse.access_token
Write-Host "Admin token acquired"
```

### Test 4A: Admin Accesses Orders (Has order.read.all)

```powershell
curl -X GET http://localhost:8080/api/v1/orders/00000000-0000-0000-0000-000000000001 `
  -H "Authorization: Bearer $ADMIN_TOKEN" -v
```

**Expected Response:**
```
< HTTP/1.1 200 OK  (or 404 if order doesn't exist)
```

### Test 4B: Admin Accesses Inventory (Has inventory.read)

```powershell
curl -X GET http://localhost:8080/api/v1/inventory/items/00000000-0000-0000-0000-000000000001 `
  -H "Authorization: Bearer $ADMIN_TOKEN" -v
```

**Expected Response:**
```
< HTTP/1.1 200 OK  (or 404 if item doesn't exist)
```

**Result:** ✅ Admin has all permissions, can access all endpoints

---

## 📊 Test Results Matrix

| Test Case | Token | User | Endpoint | Expected Status | Reason |
|-----------|-------|------|----------|----------------|--------|
| 1A | ❌ None | - | GET /orders/{id} | **401 Unauthorized** | No authentication |
| 1B | ❌ None | - | GET /inventory/items/{id} | **401 Unauthorized** | No authentication |
| 2A | ✅ Valid | customer1 | POST /orders | **201 Created** | Has order.create |
| 3A | ✅ Valid | customer1 | GET /inventory/items/{id} | **403 Forbidden** | Lacks inventory.read |
| 3B | ✅ Valid | customer1 | DELETE /orders/{id} | **403 Forbidden** | Lacks order.delete |
| 4A | ✅ Valid | admin | GET /orders/{id} | **200 OK** | Has order.read.all |
| 4B | ✅ Valid | admin | GET /inventory/items/{id} | **200 OK** | Has inventory.read |

---

## 🔍 Verification Commands

### Check Gateway Logs (Correlation IDs)

Look for log entries showing:
```
Request: GET /api/v1/orders/123 | Correlation-Id: <uuid>
```

### Decode JWT Token (Verify Claims)

Copy the access token and decode at https://jwt.io

**Expected Claims for customer1:**
```json
{
  "sub": "<user-id>",
  "preferred_username": "customer1",
  "order_claims": [
    "order.create",
    "order.read.own",
    "order.cancel",
    "order.history.view"
  ],
  "iss": "http://localhost:8181/realms/ecommerce",
  "exp": 1737560000
}
```

**Expected Claims for admin:**
```json
{
  "order_claims": [
    "order.create", "order.read.own", "order.read.all", 
    "order.update", "order.delete", "order.status.update", 
    "order.cancel", "order.payment.process", "order.refund.process",
    "order.history.view", "order.export"
  ],
  "inventory_claims": [
    "inventory.read", "inventory.create", "inventory.write",
    "inventory.delete", "inventory.audit.view", "reservation.create",
    "reservation.confirm", "reservation.cancel", "reservation.release",
    "reservation.extend", "reservation.view.all"
  ]
}
```

---

## 🎯 Key Takeaways

### 401 Unauthorized = Authentication Failed
- Occurs at **Gateway level**
- Means: "You didn't provide a valid token"
- Solution: Provide `Authorization: Bearer <token>` header

### 403 Forbidden = Authorization Failed
- Occurs at **Backend Service level**
- Means: "Your token is valid, but you don't have permission"
- Solution: User needs the required operation claim (e.g., `order.create`)

### 200 OK = Success
- **Gateway:** Token is valid (JWT signature verified)
- **Backend Service:** User has required permission (claim present)
- Request processed successfully

---

## 🚀 Quick Test Script (PowerShell)

```powershell
# Save this as test-gateway-security.ps1

Write-Host "`n=== Gateway Security Test ===" -ForegroundColor Cyan

# Test 1: No Token (Expect 401)
Write-Host "`n[Test 1] Request WITHOUT token..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/orders/123" `
        -Method GET -SkipHttpErrorCheck
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor $(if ($response.StatusCode -eq 401) {"Green"} else {"Red"})
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 2: With Token (Expect 200 or 404)
Write-Host "`n[Test 2] Getting token for customer1..." -ForegroundColor Yellow
$tokenResponse = Invoke-RestMethod -Uri "http://localhost:8181/realms/ecommerce/protocol/openid-connect/token" `
    -Method POST -ContentType "application/x-www-form-urlencoded" -Body @{
        grant_type = "password"
        client_id = "order-service"
        client_secret = "<YOUR_SECRET>"
        username = "customer1"
        password = "password123"
    }

$token = $tokenResponse.access_token
Write-Host "Token acquired: $($token.Substring(0,20))..." -ForegroundColor Green

Write-Host "`n[Test 2] Request WITH token..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/orders/123" `
        -Method GET -Headers @{Authorization="Bearer $token"} -SkipHttpErrorCheck
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor $(if ($response.StatusCode -lt 400) {"Green"} else {"Red"})
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

# Test 3: Valid Token, Wrong Permission (Expect 403)
Write-Host "`n[Test 3] Accessing inventory (customer has NO inventory.read)..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/inventory/items/123" `
        -Method GET -Headers @{Authorization="Bearer $token"} -SkipHttpErrorCheck
    Write-Host "Status: $($response.StatusCode)" -ForegroundColor $(if ($response.StatusCode -eq 403) {"Green"} else {"Red"})
} catch {
    Write-Host "Error: $_" -ForegroundColor Red
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Cyan
```

---

## ✅ Success Criteria

- [ ] Test 1: No token → **401 Unauthorized** ✅
- [ ] Test 2: Valid token → **200 OK** or **201 Created** ✅
- [ ] Test 3: Valid token, wrong permission → **403 Forbidden** ✅
- [ ] Gateway logs show correlation IDs
- [ ] TokenRelay filter forwards JWT to backend services
- [ ] Backend services validate JWT and enforce operation claims

---

**Testing Status:** Ready to test!  
**Next Steps:** Run tests above to verify Gateway security is working correctly.
