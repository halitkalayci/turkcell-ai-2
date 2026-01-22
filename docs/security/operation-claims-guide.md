# Operation-Based Claims Guide

**Last Updated:** 2026-01-22

---

## Overview

This document defines the **operation-based claims** authorization model used in our microservices platform. Unlike traditional role-based access control (RBAC), we use **fine-grained permissions** that represent specific operations on resources.

### Why Operation-Based Claims?

✅ **Fine-grained control**: Each endpoint can require specific permissions  
✅ **Flexibility**: Combine claims to create custom access patterns  
✅ **Scalability**: Easy to add new claims without refactoring  
✅ **Principle of Least Privilege**: Users get only necessary permissions  
✅ **Clear semantics**: `order.create` is more explicit than `ROLE_USER`

---

## Claim Naming Convention

**Format**: `<resource>.<operation>`

- **resource**: The domain entity (order, inventory, product, etc.)
- **operation**: The action (create, read, update, delete, etc.)

**Examples**:
- `order.create` - Create new orders
- `inventory.read` - Read inventory data
- `reservation.confirm` - Confirm reservations

**Special Cases**:
- `order.read.own` - Read only own resources (user-specific)
- `order.read.all` - Read all resources (admin-level)
- `order.status.update` - Sub-resource operation

---

## Complete Claims Reference

### Order Service Claims

| Claim | Description | Usage | Typical Roles |
|-------|-------------|-------|---------------|
| `order.create` | Create new orders | POST /orders | Customer, Admin, OrderManager |
| `order.read` | Read order details (generic) | GET /orders/{id} | All authenticated users |
| `order.read.own` | Read only own orders | GET /orders/{id} (filtered) | Customer |
| `order.read.all` | Read all orders (bypass ownership) | GET /orders/{id} | Admin, OrderManager |
| `order.update` | Update order information | PUT /orders/{id} | Admin, OrderManager |
| `order.cancel` | Cancel orders | POST /orders/{id}/cancel | Customer, Admin, OrderManager |
| `order.delete` | Delete orders (hard delete) | DELETE /orders/{id} | Admin only |
| `order.status.update` | Update order status (workflow) | PATCH /orders/{id}/status | Admin, OrderManager |

---

### Inventory Service Claims

| Claim | Description | Usage | Typical Roles |
|-------|-------------|-------|---------------|
| `inventory.read` | Read inventory data | GET /inventory/items/{id} | All authenticated users |
| `inventory.write` | Update inventory quantities | PUT /inventory/items/{id} | Admin, InventoryManager |
| `inventory.create` | Create new inventory items | POST /inventory/items | Admin, InventoryManager |
| `inventory.delete` | Delete inventory items | DELETE /inventory/items/{id} | Admin only |
| `reservation.create` | Create stock reservations | POST /reservations | Customer, Admin, OrderManager |
| `reservation.read` | Read reservation details | GET /reservations/{id} | Admin, InventoryManager, OrderManager |
| `reservation.confirm` | Confirm/finalize reservations | POST /reservations/{id}/confirm | Admin, InventoryManager |
| `reservation.cancel` | Cancel reservations | POST /reservations/{id}/cancel | Admin, InventoryManager, OrderManager |

---

### Future: Product Service Claims (Placeholder)

| Claim | Description | Usage | Typical Roles |
|-------|-------------|-------|---------------|
| `product.create` | Create new products | POST /products | Admin, ProductManager |
| `product.read` | Read product information | GET /products/{id} | All (public) |
| `product.update` | Update product details | PUT /products/{id} | Admin, ProductManager |
| `product.delete` | Delete products | DELETE /products/{id} | Admin only |
| `product.price.update` | Update product pricing | PATCH /products/{id}/price | Admin, PricingManager |
| `product.publish` | Publish product to catalog | POST /products/{id}/publish | Admin, ProductManager |
| `product.unpublish` | Remove product from catalog | POST /products/{id}/unpublish | Admin, ProductManager |

---

## Composite Roles

Composite roles are **groups of claims** that represent common user personas. They are defined in Keycloak as realm-level roles.

### Customer

**Description**: Standard customer who can browse, order, and manage own orders

**Claims**:
- `order.create`
- `order.read.own`
- `order.cancel`
- `inventory.read`
- `reservation.create`

**Use Case**: Regular e-commerce customer

---

### Admin

**Description**: Full system administrator with all permissions

**Claims**: ALL claims from all services

**Use Case**: System administrators, DevOps

---

### OrderManager

**Description**: Staff member who manages customer orders

**Claims**:
- `order.read.all`
- `order.update`
- `order.status.update`
- `order.cancel`
- `reservation.read`
- `reservation.cancel`

**Use Case**: Customer service representatives, order fulfillment staff

---

### InventoryManager

**Description**: Staff member who manages product stock

**Claims**:
- `inventory.read`
- `inventory.write`
- `inventory.create`
- `reservation.read`
- `reservation.confirm`
- `reservation.cancel`

**Use Case**: Warehouse managers, stock controllers

---

### ProductManager (Future)

**Description**: Staff member who manages product catalog

**Claims**:
- `product.create`
- `product.read`
- `product.update`
- `product.price.update`
- `product.publish`
- `product.unpublish`

**Use Case**: Merchandisers, category managers

---

## Permission Matrix

### Order Endpoints

| Endpoint | Method | Required Claims | Customer | Admin | OrderManager | InventoryManager |
|----------|--------|----------------|----------|-------|--------------|------------------|
| `/orders` | POST | `order.create` | ✓ | ✓ | ✓ | ✗ |
| `/orders/{id}` | GET | `order.read.own` OR `order.read.all` | ✓ (own) | ✓ | ✓ | ✗ |
| `/orders/{id}/cancel` | POST | `order.cancel` | ✓ (own) | ✓ | ✓ | ✗ |
| `/orders/{id}/status` | PATCH | `order.status.update` | ✗ | ✓ | ✓ | ✗ |
| `/orders/{id}` | PUT | `order.update` | ✗ | ✓ | ✓ | ✗ |
| `/orders/{id}` | DELETE | `order.delete` | ✗ | ✓ | ✗ | ✗ |

---

### Inventory Endpoints

| Endpoint | Method | Required Claims | Customer | Admin | OrderManager | InventoryManager |
|----------|--------|----------------|----------|-------|--------------|------------------|
| `/inventory/items/{id}` | GET | `inventory.read` | ✓ | ✓ | ✓ | ✓ |
| `/inventory/check-availability` | POST | `inventory.read` | ✓ | ✓ | ✓ | ✓ |
| `/inventory/items/{id}` | PUT | `inventory.write` | ✗ | ✓ | ✗ | ✓ |
| `/inventory/items` | POST | `inventory.create` | ✗ | ✓ | ✗ | ✓ |
| `/inventory/items/{id}` | DELETE | `inventory.delete` | ✗ | ✓ | ✗ | ✗ |
| `/reservations` | POST | `reservation.create` | ✓ | ✓ | ✓ | ✗ |
| `/reservations/{id}` | GET | `reservation.read` | ✗ | ✓ | ✓ | ✓ |
| `/reservations/{id}/confirm` | POST | `reservation.confirm` | ✗ | ✓ | ✗ | ✓ |
| `/reservations/{id}/cancel` | POST | `reservation.cancel` | ✗ | ✓ | ✓ | ✓ |

---

## Implementation Guidelines

### 1. Controller Level (Spring Security)

Use `@PreAuthorize` with `hasAuthority()` on controller methods:

```java
@PostMapping
@PreAuthorize("hasAuthority('order.create')")
public ResponseEntity<OrderResponseDto> createOrder(
    @Valid @RequestBody OrderRequestDto request,
    @AuthenticationPrincipal Jwt jwt) {
    // Implementation
}
```

**Multiple Claims (OR logic)**:
```java
@GetMapping("/{id}")
@PreAuthorize("hasAnyAuthority('order.read.own', 'order.read.all')")
public ResponseEntity<OrderResponseDto> getOrder(@PathVariable UUID id) {
    // Implementation
}
```

---

### 2. Service Level (Ownership Checks)

For `*.own` claims, implement ownership validation in service layer:

```java
public OrderResponseDto getOrder(UUID orderId, String userId, boolean canReadAll) {
    Order order = orderRepository.findById(orderId)
        .orElseThrow(() -> new OrderNotFoundException(orderId));
    
    // If user doesn't have "read all" claim, verify ownership
    if (!canReadAll && !order.getCustomerId().equals(userId)) {
        throw new ForbiddenException("Cannot access other user's orders");
    }
    
    return orderMapper.toDto(order);
}
```

---

### 3. JWT Token Extraction

Extract claims from JWT token in controllers:

```java
@AuthenticationPrincipal Jwt jwt

// Get user ID
String userId = jwt.getSubject();

// Get username
String username = jwt.getClaimAsString("preferred_username");

// Get order claims
List<String> orderClaims = jwt.getClaim("order_claims");

// Check specific claim
boolean canReadAll = orderClaims != null && 
    orderClaims.contains("order.read.all");
```

---

### 4. Gateway Level (Optional Pre-filtering)

For performance, gateway can reject requests early:

```java
.pathMatchers("/api/v1/orders").hasAuthority("order.create")
.pathMatchers("/api/v1/orders/{id}/status").hasAuthority("order.status.update")
```

**Note**: Backend services still MUST validate claims (defense in depth).

---

## Adding New Claims

### Step 1: Define the Claim

1. Determine resource and operation
2. Follow naming convention: `resource.operation`
3. Document in this file (add to appropriate table)

### Step 2: Create in Keycloak

1. Navigate to appropriate client (e.g., `order-service`)
2. Go to **Roles** tab
3. **Create role**:
   - Name: `order.export` (example)
   - Description: Export orders to CSV
4. Click **Save**

### Step 3: Assign to Composite Roles

1. Navigate to **Realm roles**
2. Select composite role (e.g., `Admin`, `OrderManager`)
3. **Associated roles** → **Assign role**
4. Select new claim
5. Click **Assign**

### Step 4: Update Client Scope Mapper (if needed)

If using custom claim name in JWT:
1. **Clients** → **order-service** → **Client scopes**
2. Click `order-service-dedicated`
3. Verify mapper includes new role

### Step 5: Implement in Code

1. Add `@PreAuthorize("hasAuthority('order.export')")` to controller
2. Update OpenAPI spec with security requirement
3. Add integration tests
4. Update this documentation

---

## Best Practices

### ✅ DO

- Use descriptive claim names (`order.status.update` not `order.us`)
- Follow naming convention strictly
- Document new claims in this file
- Use composite roles for common personas
- Implement ownership checks for `*.own` claims
- Test both positive and negative authorization cases

### ❌ DON'T

- Don't use generic claims like `read`, `write` (too broad)
- Don't mix claim formats (`order:create` vs `order.create`)
- Don't hardcode claim checks in business logic
- Don't skip backend validation (relying only on gateway)
- Don't create overlapping claims (`order.read` + `order.read.all`)

---

## Testing Claims

See [security-testing-guide.md](security-testing-guide.md) for detailed test scenarios.

**Quick Test**:
```bash
# 1. Get token for customer1
TOKEN=$(curl -s -X POST 'http://localhost:8180/realms/ecommerce/protocol/openid-connect/token' \
  -d 'grant_type=password' \
  -d 'client_id=gateway-client' \
  -d 'client_secret=YOUR_SECRET' \
  -d 'username=customer1' \
  -d 'password=customer123' | jq -r '.access_token')

# 2. Test allowed endpoint (should succeed)
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/orders

# 3. Test forbidden endpoint (should fail with 403)
curl -H "Authorization: Bearer $TOKEN" \
  -X PATCH http://localhost:8080/api/v1/orders/{id}/status
```

---

## Migration Guide

### From ROLE-based to CLAIM-based

**Old (Role-based)**:
```java
@PreAuthorize("hasRole('ADMIN')")
```

**New (Claim-based)**:
```java
@PreAuthorize("hasAuthority('order.delete')")
```

**Migration Steps**:
1. Identify all `hasRole()` calls in codebase
2. Map roles to appropriate claims (see Permission Matrix)
3. Update `@PreAuthorize` annotations
4. Update Keycloak configuration
5. Test all endpoints with new claims

---

## Related Documentation

- [Keycloak Integration Guide](keycloak-integration.md)
- [JWT Token Structure](jwt-token-structure.md)
- [Security Architecture ADR](../architecture/security-architecture.md)
- [AGENTS.md Section 7](../../AGENTS.md#7-security--authentication)

---

**Maintained by**: Platform Team  
**Review Frequency**: Every sprint or when adding new services
