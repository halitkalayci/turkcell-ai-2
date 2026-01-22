# Keycloak Realm Setup Instructions

**Keycloak Version:** 26.5.1  
**Keycloak URL:** http://localhost:8181  
**Realm Name:** `ecommerce`  
**Setup Date:** January 22, 2026

---

## Table of Contents

1. [Access Keycloak Admin Console](#step-1-access-keycloak-admin-console)
2. [Create Realm](#step-2-create-realm)
3. [Create Clients](#step-3-create-clients)
4. [Create Operation Claims (Client Roles)](#step-4-create-operation-claims-client-roles)
5. [Create Composite Roles](#step-5-create-composite-roles)
6. [Configure Client Scopes & Mappers](#step-6-configure-client-scopes--mappers)
7. [Create Test Users](#step-7-create-test-users)
8. [Extract Client Secrets](#step-8-extract-client-secrets)
9. [Test Token Acquisition](#step-9-test-token-acquisition)
10. [Set Environment Variables](#step-10-set-environment-variables)

---

## Step 1: Access Keycloak Admin Console

1. Open browser: **http://localhost:8181**
2. Click **"Administration Console"**
3. Login with Keycloak admin credentials
   - Username: `admin`
   - Password: `admin` (or your configured password)
4. You should see the Keycloak Admin Console

---

## Step 2: Create Realm

1. In the top-left corner, hover over **"Master"** dropdown
2. Click **"Create Realm"**
3. Fill in the form:
   - **Realm name**: `ecommerce`
   - **Enabled**: ON
4. Click **"Create"**
5. Verify: Top-left dropdown now shows **"ecommerce"**

### Configure Realm Settings

1. Click **"Realm settings"** in left sidebar
2. Go to **"Login"** tab:
   - **User registration**: OFF
   - **Forgot password**: OFF (or ON if needed)
   - **Remember me**: ON
   - **Login with email**: ON
3. Go to **"Tokens"** tab:
   - **Access Token Lifespan**: `15` minutes
   - **Refresh Token Max Reuse**: `0`
   - **Client login timeout**: `15` minutes
   - **Access Token Lifespan For Implicit Flow**: `15` minutes
   - **SSO Session Idle**: `30` minutes
   - **SSO Session Max**: `10` hours
4. Click **"Save"**

---

## Step 3: Create Clients

We need to create 3 clients: `gateway-client`, `order-service`, `inventory-service`

### 3.1 Create Gateway Client

1. Click **"Clients"** in left sidebar
2. Click **"Create client"** button
3. **General Settings** (Step 1):
   - **Client type**: `OpenID Connect`
   - **Client ID**: `gateway-client`
   - Click **"Next"**
4. **Capability config** (Step 2):
   - **Client authentication**: **ON** (confidential client)
   - **Authorization**: OFF
   - **Authentication flow**:
     - ✅ **Standard flow** (Authorization Code Flow)
     - ✅ **Direct access grants** (Resource Owner Password Credentials)
     - ❌ Implicit flow
     - ❌ OAuth 2.0 Device Authorization Grant
   - Click **"Next"**
5. **Login settings** (Step 3):
   - **Root URL**: `http://localhost:8080`
   - **Home URL**: `http://localhost:8080`
   - **Valid redirect URIs**: 
     ```
     http://localhost:8080/*
     http://localhost:8080/login/oauth2/code/*
     ```
   - **Valid post logout redirect URIs**: `http://localhost:8080/*`
   - **Web origins**: `http://localhost:8080`
   - Click **"Save"**

6. **Configure Additional Settings**:
   - Click on the newly created **"gateway-client"**
   - Go to **"Settings"** tab:
     - Scroll down to **"Access settings"**
     - **Admin URL**: (leave empty)
     - **Backchannel logout URL**: (leave empty)
   - Go to **"Credentials"** tab:
     - **Client Authenticator**: `Client Id and Secret`
     - Copy the **Client Secret** (you'll need this later)
     - **Save this secret**: `<GATEWAY_CLIENT_SECRET>` (example: `fK8dX2nP9mQ3wR5tY7uV1bN4cZ6hJ8lM`)

### 3.2 Create Order Service Client

1. Click **"Clients"** in left sidebar
2. Click **"Create client"** button
3. **General Settings** (Step 1):
   - **Client type**: `OpenID Connect`
   - **Client ID**: `order-service`
   - Click **"Next"**
4. **Capability config** (Step 2):
   - **Client authentication**: **ON** (confidential client)
   - **Authorization**: OFF
   - **Authentication flow**:
     - ✅ **Direct access grants** (for testing with password grant)
     - ❌ Standard flow
     - ❌ Implicit flow
     - ❌ OAuth 2.0 Device Authorization Grant
   - Click **"Next"**
5. **Login settings** (Step 3):
   - **Root URL**: `http://localhost:8081`
   - **Home URL**: (leave empty)
   - **Valid redirect URIs**: (leave empty - not needed for resource server)
   - **Valid post logout redirect URIs**: (leave empty)
   - **Web origins**: `+` (this means inherit from valid redirect URIs)
   - Click **"Save"**

6. **Configure Additional Settings**:
   - Click on **"order-service"**
   - Go to **"Credentials"** tab:
     - Copy the **Client Secret**
     - **Save this secret**: `<ORDER_SERVICE_CLIENT_SECRET>`

### 3.3 Create Inventory Service Client

1. Click **"Clients"** in left sidebar
2. Click **"Create client"** button
3. **General Settings** (Step 1):
   - **Client type**: `OpenID Connect`
   - **Client ID**: `inventory-service`
   - Click **"Next"**
4. **Capability config** (Step 2):
   - **Client authentication**: **ON**
   - **Authorization**: OFF
   - **Authentication flow**:
     - ✅ **Direct access grants**
     - ❌ Standard flow
     - ❌ Implicit flow
   - Click **"Next"**
5. **Login settings** (Step 3):
   - **Root URL**: `http://localhost:8082`
   - **Home URL**: (leave empty)
   - **Valid redirect URIs**: (leave empty)
   - **Valid post logout redirect URIs**: (leave empty)
   - **Web origins**: `+`
   - Click **"Save"**

6. **Configure Additional Settings**:
   - Click on **"inventory-service"**
   - Go to **"Credentials"** tab:
     - Copy the **Client Secret**
     - **Save this secret**: `<INVENTORY_SERVICE_CLIENT_SECRET>`

---

## Step 4: Create Operation Claims (Client Roles)

We create operation claims as **Client Roles** for each service.

### 4.1 Order Service Claims (11 roles)

1. Click **"Clients"** in left sidebar
2. Click on **"order-service"**
3. Click **"Roles"** tab
4. Click **"Create role"** button
5. Create each of these roles (11 total):

| Role Name | Description |
|-----------|-------------|
| `order.create` | Create new orders |
| `order.read.own` | Read user's own orders |
| `order.read.all` | Read all orders (admin) |
| `order.update` | Update order details |
| `order.delete` | Delete orders |
| `order.status.update` | Update order status |
| `order.cancel` | Cancel orders |
| `order.payment.process` | Process payments |
| `order.refund.process` | Process refunds |
| `order.history.view` | View order history |
| `order.export` | Export order data |

**For each role:**
- **Role name**: (from table above)
- **Description**: (from table above)
- Click **"Save"**
- Repeat 11 times

### 4.2 Inventory Service Claims (11 roles)

1. Click **"Clients"** in left sidebar
2. Click on **"inventory-service"**
3. Click **"Roles"** tab
4. Click **"Create role"** button
5. Create each of these roles (11 total):

| Role Name | Description |
|-----------|-------------|
| `inventory.read` | Read inventory items |
| `inventory.create` | Create inventory items |
| `inventory.write` | Update inventory items |
| `inventory.delete` | Delete inventory items |
| `inventory.audit.view` | View inventory audit logs |
| `reservation.create` | Create reservations |
| `reservation.confirm` | Confirm reservations |
| `reservation.cancel` | Cancel reservations |
| `reservation.release` | Release reservations |
| `reservation.extend` | Extend reservation time |
| `reservation.view.all` | View all reservations |

**For each role:** (repeat 11 times)
- **Role name**: (from table above)
- **Description**: (from table above)
- Click **"Save"**

---

## Step 5: Create Composite Roles

Composite roles group multiple operation claims together.

### 5.1 Create "Customer" Role

1. Click **"Realm roles"** in left sidebar
2. Click **"Create role"** button
3. Fill in:
   - **Role name**: `Customer`
   - **Description**: `Standard customer with order management permissions`
4. Click **"Save"**
5. Click **"Action"** dropdown → **"Add associated roles"**
6. **Filter by clients**: Select **"order-service"**
7. Select these roles:
   - ✅ `order.create`
   - ✅ `order.read.own`
   - ✅ `order.cancel`
   - ✅ `order.history.view`
8. Click **"Assign"** (4 roles assigned)

### 5.2 Create "OrderManager" Role

1. Click **"Realm roles"** in left sidebar
2. Click **"Create role"** button
3. Fill in:
   - **Role name**: `OrderManager`
   - **Description**: `Order management staff with full order permissions`
4. Click **"Save"**
5. Click **"Action"** dropdown → **"Add associated roles"**
6. **Filter by clients**: Select **"order-service"**
7. Select ALL order service roles (11 roles):
   - ✅ `order.create`
   - ✅ `order.read.own`
   - ✅ `order.read.all`
   - ✅ `order.update`
   - ✅ `order.delete`
   - ✅ `order.status.update`
   - ✅ `order.cancel`
   - ✅ `order.payment.process`
   - ✅ `order.refund.process`
   - ✅ `order.history.view`
   - ✅ `order.export`
8. Click **"Assign"** (11 roles assigned)
9. **Also add reservation permissions**:
   - Click **"Action"** → **"Add associated roles"**
   - **Filter by clients**: Select **"inventory-service"**
   - Select:
     - ✅ `reservation.create`
     - ✅ `reservation.confirm`
     - ✅ `reservation.cancel`
     - ✅ `reservation.release`
     - ✅ `reservation.view.all`
   - Click **"Assign"** (5 more roles)

### 5.3 Create "InventoryManager" Role

1. Click **"Realm roles"** in left sidebar
2. Click **"Create role"** button
3. Fill in:
   - **Role name**: `InventoryManager`
   - **Description**: `Inventory management staff with full inventory permissions`
4. Click **"Save"**
5. Click **"Action"** dropdown → **"Add associated roles"**
6. **Filter by clients**: Select **"inventory-service"**
7. Select ALL inventory service roles (11 roles):
   - ✅ `inventory.read`
   - ✅ `inventory.create`
   - ✅ `inventory.write`
   - ✅ `inventory.delete`
   - ✅ `inventory.audit.view`
   - ✅ `reservation.create`
   - ✅ `reservation.confirm`
   - ✅ `reservation.cancel`
   - ✅ `reservation.release`
   - ✅ `reservation.extend`
   - ✅ `reservation.view.all`
8. Click **"Assign"** (11 roles assigned)

### 5.4 Create "Admin" Role

1. Click **"Realm roles"** in left sidebar
2. Click **"Create role"** button
3. Fill in:
   - **Role name**: `Admin`
   - **Description**: `System administrator with all permissions`
4. Click **"Save"**
5. Click **"Action"** dropdown → **"Add associated roles"**
6. **Filter by clients**: Select **"order-service"**
7. Select ALL order service roles (11 roles)
8. Click **"Assign"**
9. Click **"Action"** → **"Add associated roles"** again
10. **Filter by clients**: Select **"inventory-service"**
11. Select ALL inventory service roles (11 roles)
12. Click **"Assign"** (Total: 22 roles assigned)

---

## Step 6: Configure Client Scopes & Mappers

We need to add custom claims to JWT tokens.

### 6.1 Create Order Claims Mapper (for order-service)

1. Click **"Clients"** in left sidebar
2. Click on **"order-service"**
3. Go to **"Client scopes"** tab
4. Click on **"order-service-dedicated"** (the dedicated scope)
5. Go to **"Mappers"** tab
6. Click **"Add mapper"** → **"By configuration"**
7. Select **"User Client Role"**
8. Fill in:
   - **Name**: `order-claims-mapper`
   - **Client ID**: `order-service`
   - **Token Claim Name**: `order_claims`
   - **Claim JSON Type**: `String`
   - **Add to ID token**: ON
   - **Add to access token**: ON
   - **Add to userinfo**: OFF
   - **Multivalued**: ON
9. Click **"Save"**

### 6.2 Create Inventory Claims Mapper (for inventory-service)

1. Click **"Clients"** in left sidebar
2. Click on **"inventory-service"**
3. Go to **"Client scopes"** tab
4. Click on **"inventory-service-dedicated"** (the dedicated scope)
5. Go to **"Mappers"** tab
6. Click **"Add mapper"** → **"By configuration"**
7. Select **"User Client Role"**
8. Fill in:
   - **Name**: `inventory-claims-mapper`
   - **Client ID**: `inventory-service`
   - **Token Claim Name**: `inventory_claims`
   - **Claim JSON Type**: `String`
   - **Add to ID token**: ON
   - **Add to access token**: ON
   - **Add to userinfo**: OFF
   - **Multivalued**: ON
9. Click **"Save"**

### 6.3 Create Gateway Claims Mapper (for gateway-client)

Gateway needs to see both order and inventory claims.

1. Click **"Clients"** in left sidebar
2. Click on **"gateway-client"**
3. Go to **"Client scopes"** tab
4. Click **"Add client scope"** dropdown → **"Add optional"**
5. Select both:
   - ✅ `order-service-dedicated`
   - ✅ `inventory-service-dedicated`
6. Click **"Add"**

This allows Gateway to include both claim sets in tokens.

---

## Step 7: Create Test Users

Create 4 test users with different roles.

### 7.1 Create Customer1 User

1. Click **"Users"** in left sidebar
2. Click **"Add user"** button
3. Fill in:
   - **Username**: `customer1`
   - **Email**: `customer1@example.com`
   - **Email verified**: ON
   - **First name**: `John`
   - **Last name**: `Customer`
   - **Enabled**: ON
4. Click **"Create"**
5. Go to **"Credentials"** tab:
   - Click **"Set password"**
   - **Password**: `password123`
   - **Password confirmation**: `password123`
   - **Temporary**: OFF
   - Click **"Save"**
   - Confirm: Click **"Save password"**
6. Go to **"Role mapping"** tab:
   - Click **"Assign role"**
   - **Filter by realm roles**: ON
   - Select: ✅ `Customer`
   - Click **"Assign"**

### 7.2 Create Customer2 User

1. Click **"Users"** in left sidebar
2. Click **"Add user"** button
3. Fill in:
   - **Username**: `customer2`
   - **Email**: `customer2@example.com`
   - **Email verified**: ON
   - **First name**: `Jane`
   - **Last name**: `Customer`
   - **Enabled**: ON
4. Click **"Create"**
5. Go to **"Credentials"** tab:
   - Click **"Set password"**
   - **Password**: `password123`
   - **Temporary**: OFF
   - Click **"Save"** → **"Save password"**
6. Go to **"Role mapping"** tab:
   - Assign role: ✅ `Customer`

### 7.3 Create Admin User

1. Click **"Users"** in left sidebar
2. Click **"Add user"** button
3. Fill in:
   - **Username**: `admin`
   - **Email**: `admin@example.com`
   - **Email verified**: ON
   - **First name**: `System`
   - **Last name**: `Administrator`
   - **Enabled**: ON
4. Click **"Create"**
5. Go to **"Credentials"** tab:
   - Click **"Set password"**
   - **Password**: `admin123`
   - **Temporary**: OFF
   - Click **"Save"** → **"Save password"**
6. Go to **"Role mapping"** tab:
   - Assign role: ✅ `Admin`

### 7.4 Create Order Manager User

1. Click **"Users"** in left sidebar
2. Click **"Add user"** button
3. Fill in:
   - **Username**: `order_manager`
   - **Email**: `ordermanager@example.com`
   - **Email verified**: ON
   - **First name**: `Order`
   - **Last name**: `Manager`
   - **Enabled**: ON
4. Click **"Create"**
5. Go to **"Credentials"** tab:
   - Click **"Set password"**
   - **Password**: `password123`
   - **Temporary**: OFF
   - Click **"Save"** → **"Save password"**
6. Go to **"Role mapping"** tab:
   - Assign role: ✅ `OrderManager`

### 7.5 Create Inventory Manager User

1. Click **"Users"** in left sidebar
2. Click **"Add user"** button
3. Fill in:
   - **Username**: `inventory_manager`
   - **Email**: `inventorymanager@example.com`
   - **Email verified**: ON
   - **First name**: `Inventory`
   - **Last name**: `Manager`
   - **Enabled**: ON
4. Click **"Create"**
5. Go to **"Credentials"** tab:
   - Click **"Set password"**
   - **Password**: `password123`
   - **Temporary**: OFF
   - Click **"Save"** → **"Save password"**
6. Go to **"Role mapping"** tab:
   - Assign role: ✅ `InventoryManager`

---

## Step 8: Extract Client Secrets

Retrieve the client secrets for environment variables.

1. Click **"Clients"** in left sidebar
2. Click on **"gateway-client"**
3. Go to **"Credentials"** tab
4. Copy the **"Client secret"** value
5. Save it: `GATEWAY_CLIENT_SECRET=<paste-secret-here>`
6. Repeat for **"order-service"**:
   - Save it: `ORDER_SERVICE_CLIENT_SECRET=<paste-secret-here>`
7. Repeat for **"inventory-service"**:
   - Save it: `INVENTORY_SERVICE_CLIENT_SECRET=<paste-secret-here>`

**Example output:**
```
GATEWAY_CLIENT_SECRET=fK8dX2nP9mQ3wR5tY7uV1bN4cZ6hJ8lM
ORDER_SERVICE_CLIENT_SECRET=aB1cD2eF3gH4iJ5kL6mN7oP8qR9sT0u
INVENTORY_SERVICE_CLIENT_SECRET=zY9xW8vU7tS6rQ5pO4nM3lK2jI1hG0f
```

---

## Step 9: Test Token Acquisition

Use curl to verify token acquisition works.

### 9.1 Test Customer1 Token

```powershell
curl -X POST "http://localhost:8181/realms/ecommerce/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password" `
  -d "client_id=order-service" `
  -d "client_secret=<ORDER_SERVICE_CLIENT_SECRET>" `
  -d "username=customer1" `
  -d "password=password123" `
  -d "scope=openid profile email"
```

**Expected Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 900,
  "refresh_expires_in": 1800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "not-before-policy": 0,
  "session_state": "...",
  "scope": "openid profile email"
}
```

### 9.2 Decode Token to Verify Claims

Copy the `access_token` and decode it at https://jwt.io

**Expected Claims:**
```json
{
  "sub": "...",
  "preferred_username": "customer1",
  "email": "customer1@example.com",
  "order_claims": [
    "order.create",
    "order.read.own",
    "order.cancel",
    "order.history.view"
  ]
}
```

### 9.3 Test Admin Token

```powershell
curl -X POST "http://localhost:8181/realms/ecommerce/protocol/openid-connect/token" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "grant_type=password" `
  -d "client_id=order-service" `
  -d "client_secret=<ORDER_SERVICE_CLIENT_SECRET>" `
  -d "username=admin" `
  -d "password=admin123" `
  -d "scope=openid profile email"
```

**Verify `order_claims` contains all 11 order permissions.**

---

## Step 10: Set Environment Variables

### Windows PowerShell

```powershell
# Set environment variables (replace with your actual secrets)
$env:KEYCLOAK_CLIENT_SECRET="<GATEWAY_CLIENT_SECRET>"
$env:ORDER_SERVICE_CLIENT_SECRET="<ORDER_SERVICE_CLIENT_SECRET>"
$env:INVENTORY_SERVICE_CLIENT_SECRET="<INVENTORY_SERVICE_CLIENT_SECRET>"

# Verify
echo $env:KEYCLOAK_CLIENT_SECRET
```

### Linux/macOS

```bash
# Set environment variables (replace with your actual secrets)
export KEYCLOAK_CLIENT_SECRET="<GATEWAY_CLIENT_SECRET>"
export ORDER_SERVICE_CLIENT_SECRET="<ORDER_SERVICE_CLIENT_SECRET>"
export INVENTORY_SERVICE_CLIENT_SECRET="<INVENTORY_SERVICE_CLIENT_SECRET>"

# Verify
echo $KEYCLOAK_CLIENT_SECRET
```

### Permanent Setup (Optional)

**Windows (User Environment Variables):**
1. Open **System Properties** → **Environment Variables**
2. Under **User variables**, click **"New"**
3. Add each variable with its value

**Linux/macOS (~/.bashrc or ~/.zshrc):**
```bash
export KEYCLOAK_CLIENT_SECRET="<secret>"
export ORDER_SERVICE_CLIENT_SECRET="<secret>"
export INVENTORY_SERVICE_CLIENT_SECRET="<secret>"
```

Then: `source ~/.bashrc`

---

## Verification Checklist

After completing all steps, verify:

- [ ] Realm `ecommerce` created and active
- [ ] 3 clients created: gateway-client, order-service, inventory-service
- [ ] 22 operation claims created (11 per service)
- [ ] 4 composite roles created: Customer, Admin, OrderManager, InventoryManager
- [ ] 5 test users created with correct role assignments
- [ ] Client scopes configured with claim mappers
- [ ] Token acquisition works for all users
- [ ] JWT tokens contain correct `order_claims` or `inventory_claims`
- [ ] Environment variables exported with client secrets

---

## Next Steps

1. ✅ Keycloak realm configured
2. ➡️ **Implement Gateway Security** (Batch 2)
3. ➡️ **Implement Order Service Security** (Batch 3)
4. ➡️ **Implement Inventory Service Security** (Batch 4)
5. ➡️ **End-to-End Testing** (Batch 5)

---

## Troubleshooting

### Token Acquisition Fails (401 Unauthorized)
- **Cause**: Wrong client_secret
- **Fix**: Re-check client secret in Keycloak Console

### No Claims in Token
- **Cause**: Mapper not configured or scope not added
- **Fix**: Re-check Step 6 (Client Scopes & Mappers)

### User Cannot Login
- **Cause**: Password not set or temporary password enabled
- **Fix**: Go to Users → Credentials → Set password (Temporary: OFF)

### Wrong Port (8180 vs 8181)
- **Note**: Documentation uses 8180, your setup uses 8181
- **Fix**: All configuration files in code will use `localhost:8181`

---

**Configuration Complete! ✅**
Proceed to Batch 2 implementation.
