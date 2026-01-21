# Task 04: Inventory Service - Infrastructure Layer Refactoring

**Task ID:** REFACTOR-04  
**Service:** inventory-service  
**Phase:** Implementation - Infrastructure Layer  
**Estimated Time:** 6 hours  
**Dependencies:** Task 02 (Domain), Task 03 (Application) ✅  

---

## Objective

Transform existing JPA entities and repositories into proper adapters that implement domain ports. Create clean separation between domain and persistence concerns.

---

## Infrastructure Layer Principles

Per AGENTS.md Section 3.4:
- JPA entities (persistence model ONLY)
- Repositories (implement domain ports)
- External clients, messaging
- NO business rules
- Mappers between domain ↔ persistence

---

## Package Structure

```
infrastructure/
├── persistence/
│   ├── entity/           [JPA entities]
│   ├── repository/       [Spring Data JPA repositories]
│   ├── adapter/          [Port implementations]
│   └── mapper/           [Domain ↔ Entity mappers]
└── config/
    └── OpenApiConfig.java
```

---

## Files to Transform/Create

### Package: `infrastructure.persistence.entity`

#### 1. `InventoryItemEntity.java` (Rename from `InventoryItem`)
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.entity.InventoryItemEntity`

**Changes from current `entity/InventoryItem.java`:**
- Rename class to `InventoryItemEntity`
- Remove ALL business methods (`isAvailable()`, `reserve()`, `release()`)
- Keep ONLY JPA annotations and getters/setters
- Change `LocalDateTime` → `Instant`
- Keep `@Version` for optimistic locking

```java
@Entity
@Table(name = "inventory_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemEntity {

    @Id
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @UpdateTimestamp
    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;  // Changed from LocalDateTime

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
```

**Key Changes:**
- ❌ Remove: `isAvailable()`, `reserve()`, `release()`, `restock()`
- ✅ Keep: JPA annotations, basic getters/setters
- ✅ Change: `LocalDateTime` → `Instant`

---

#### 2. `ReservationEntity.java` (Rename from `Reservation`)
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.entity.ReservationEntity`

```java
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, 
               orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<ReservationItemEntity> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;  // Changed from LocalDateTime

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;  // Changed from LocalDateTime
}
```

**Changes:**
- ❌ Remove: `isExpired()`, `confirm()`, `cancel()`, `canBeConfirmed()`, etc.
- ✅ Keep: JPA relationships, annotations
- ✅ Change: `LocalDateTime` → `Instant`

---

#### 3. `ReservationItemEntity.java`
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.entity.ReservationItemEntity`

```java
@Entity
@Table(name = "reservation_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationEntity reservation;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private Integer quantity;
}
```

---

### Package: `infrastructure.persistence.repository`

These are Spring Data JPA repositories (NOT domain ports).

#### 4. `InventoryItemJpaRepository.java` (Rename from `InventoryItemRepository`)
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.repository.InventoryItemJpaRepository`

```java
public interface InventoryItemJpaRepository 
        extends JpaRepository<InventoryItemEntity, UUID> {
    
    // Keep existing query methods
    List<InventoryItemEntity> findByProductIdIn(List<UUID> productIds);
}
```

**Changes:**
- Rename to `InventoryItemJpaRepository`
- Works with `InventoryItemEntity` (not domain model)

---

#### 5. `ReservationJpaRepository.java`
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.repository.ReservationJpaRepository`

```java
public interface ReservationJpaRepository 
        extends JpaRepository<ReservationEntity, UUID> {
    
    Optional<ReservationEntity> findByOrderId(UUID orderId);
    
    @Query("SELECT r FROM ReservationEntity r WHERE r.expiresAt < :currentTime " +
           "AND r.status = 'PENDING'")
    List<ReservationEntity> findExpiredReservations(
        @Param("currentTime") Instant currentTime
    );
}
```

---

### Package: `infrastructure.persistence.mapper`

Mappers convert between domain models and JPA entities.

#### 6. `InventoryItemMapper.java`
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.mapper.InventoryItemMapper`

```java
@Component
public class InventoryItemMapper {

    public InventoryItem toDomain(InventoryItemEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return new InventoryItem(
            entity.getProductId(),
            entity.getAvailableQuantity(),
            entity.getReservedQuantity(),
            entity.getVersion()
        );
    }

    public InventoryItemEntity toEntity(InventoryItem domain) {
        if (domain == null) {
            return null;
        }
        
        return InventoryItemEntity.builder()
            .productId(domain.getProductId())
            .availableQuantity(domain.getAvailableQuantity())
            .reservedQuantity(domain.getReservedQuantity())
            .totalQuantity(domain.getTotalQuantity())
            .lastUpdatedAt(domain.getLastUpdatedAt())
            .version(domain.getVersion())
            .build();
    }

    public List<InventoryItem> toDomainList(List<InventoryItemEntity> entities) {
        return entities.stream()
            .map(this::toDomain)
            .toList();
    }

    public List<InventoryItemEntity> toEntityList(List<InventoryItem> domains) {
        return domains.stream()
            .map(this::toEntity)
            .toList();
    }
}
```

**Note:** Mapper only transfers data, NO business logic.

---

#### 7. `ReservationMapper.java`
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.mapper.ReservationMapper`

```java
@Component
public class ReservationMapper {

    public Reservation toDomain(ReservationEntity entity) {
        if (entity == null) {
            return null;
        }
        
        List<ReservationItem> items = entity.getItems().stream()
            .map(itemEntity -> new ReservationItem(
                itemEntity.getProductId(),
                itemEntity.getQuantity()
            ))
            .toList();
        
        return new Reservation(
            entity.getId(),
            entity.getOrderId(),
            items,
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getExpiresAt()
        );
    }

    public ReservationEntity toEntity(Reservation domain) {
        if (domain == null) {
            return null;
        }
        
        ReservationEntity entity = ReservationEntity.builder()
            .id(domain.getId())
            .orderId(domain.getOrderId())
            .status(domain.getStatus())
            .createdAt(domain.getCreatedAt())
            .expiresAt(domain.getExpiresAt())
            .build();
        
        List<ReservationItemEntity> itemEntities = domain.getItems().stream()
            .map(item -> ReservationItemEntity.builder()
                .reservation(entity)
                .productId(item.getProductId())
                .quantity(item.getQuantity())
                .build())
            .toList();
        
        entity.setItems(itemEntities);
        
        return entity;
    }
}
```

---

### Package: `infrastructure.persistence.adapter`

Adapters implement domain ports using JPA repositories.

#### 8. `InventoryRepositoryAdapter.java`
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.adapter.InventoryRepositoryAdapter`

**Purpose:** Implements `domain.port.InventoryRepository`

```java
@Component
@RequiredArgsConstructor
public class InventoryRepositoryAdapter implements InventoryRepository {

    private final InventoryItemJpaRepository jpaRepository;
    private final InventoryItemMapper mapper;

    @Override
    public Optional<InventoryItem> findByProductId(UUID productId) {
        return jpaRepository.findById(productId)
            .map(mapper::toDomain);
    }

    @Override
    public List<InventoryItem> findByProductIds(List<UUID> productIds) {
        List<InventoryItemEntity> entities = 
            jpaRepository.findByProductIdIn(productIds);
        return mapper.toDomainList(entities);
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        InventoryItemEntity entity = mapper.toEntity(item);
        InventoryItemEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void saveAll(List<InventoryItem> items) {
        List<InventoryItemEntity> entities = mapper.toEntityList(items);
        jpaRepository.saveAll(entities);
    }
}
```

**Characteristics:**
- Implements domain port interface
- Uses JPA repository internally
- Converts between domain ↔ entity via mapper
- NO business logic

---

#### 9. `ReservationRepositoryAdapter.java`
**Path:** `com.ecommerce.inventoryservice.infrastructure.persistence.adapter.ReservationRepositoryAdapter`

```java
@Component
@RequiredArgsConstructor
public class ReservationRepositoryAdapter implements ReservationRepository {

    private final ReservationJpaRepository jpaRepository;
    private final ReservationMapper mapper;

    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity entity = mapper.toEntity(reservation);
        ReservationEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Reservation> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Reservation> findByOrderId(UUID orderId) {
        return jpaRepository.findByOrderId(orderId)
            .map(mapper::toDomain);
    }

    @Override
    public List<Reservation> findExpiredReservations(Instant currentTime) {
        List<ReservationEntity> entities = 
            jpaRepository.findExpiredReservations(currentTime);
        return entities.stream()
            .map(mapper::toDomain)
            .toList();
    }
}
```

---

## Database Migration (data.sql)

Current `data.sql` needs NO schema changes (columns stay same), but we need to ensure `Instant` compatibility.

H2 Configuration:
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:h2:mem:inventorydb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC  # Ensure UTC for Instant
```

**No SQL changes needed** - Instant maps to TIMESTAMP in H2.

---

## Testing Strategy

### Adapter Integration Tests

#### `InventoryRepositoryAdapterTest.java`
```java
@DataJpaTest  // Only loads persistence layer
class InventoryRepositoryAdapterTest {

    @Autowired
    private InventoryItemJpaRepository jpaRepository;
    
    private InventoryRepositoryAdapter adapter;
    private InventoryItemMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new InventoryItemMapper();
        adapter = new InventoryRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void shouldSaveAndRetrieveDomainModel() {
        // Given
        UUID productId = UUID.randomUUID();
        InventoryItem domain = new InventoryItem(productId, 100, 0, 0L);
        
        // When
        InventoryItem saved = adapter.save(domain);
        Optional<InventoryItem> retrieved = adapter.findByProductId(productId);
        
        // Then
        assertThat(saved.getProductId()).isEqualTo(productId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getAvailableQuantity()).isEqualTo(100);
    }

    @Test
    void shouldMapInstantCorrectly() {
        // Verify Instant persists correctly
        UUID productId = UUID.randomUUID();
        InventoryItem domain = new InventoryItem(productId, 50, 10, 0L);
        Instant before = Instant.now();
        
        adapter.save(domain);
        
        InventoryItem retrieved = adapter.findByProductId(productId).orElseThrow();
        assertThat(retrieved.getLastUpdatedAt()).isAfterOrEqualTo(before);
    }
}
```

---

## File Breakdown

| File | Type | Lines Est. | Complexity |
|------|------|------------|------------|
| InventoryItemEntity.java | JPA Entity | 50 | Low |
| ReservationEntity.java | JPA Entity | 60 | Low |
| ReservationItemEntity.java | JPA Entity | 40 | Low |
| InventoryItemJpaRepository.java | Spring Data | 10 | Low |
| ReservationJpaRepository.java | Spring Data | 15 | Low |
| InventoryItemMapper.java | Mapper | 60 | Low |
| ReservationMapper.java | Mapper | 80 | Medium |
| InventoryRepositoryAdapter.java | Adapter | 50 | Low |
| ReservationRepositoryAdapter.java | Adapter | 60 | Low |

**Total:** ~425 lines

---

## Migration Checklist

### Rename Operations
- [ ] `entity/InventoryItem.java` → `infrastructure/persistence/entity/InventoryItemEntity.java`
- [ ] `entity/Reservation.java` → `infrastructure/persistence/entity/ReservationEntity.java`
- [ ] `entity/ReservationItem.java` → `infrastructure/persistence/entity/ReservationItemEntity.java`
- [ ] `entity/ReservationStatus.java` → `domain/model/ReservationStatus.java`
- [ ] `repository/InventoryItemRepository.java` → `infrastructure/persistence/repository/InventoryItemJpaRepository.java`
- [ ] `repository/ReservationRepository.java` → `infrastructure/persistence/repository/ReservationJpaRepository.java`

### Delete Operations
- [ ] Remove business methods from entities
- [ ] Delete old `service/InventoryService.java`
- [ ] Delete old `service/InventoryServiceImpl.java`

### Create Operations
- [ ] Create all mappers
- [ ] Create all adapters
- [ ] Update application.yml for UTC timezone

---

## Validation Criteria

- [ ] All JPA entities renamed with "Entity" suffix
- [ ] NO business methods in entities
- [ ] All timestamps use `Instant`
- [ ] Adapters implement domain ports
- [ ] Mappers correctly convert domain ↔ entity
- [ ] Integration tests pass with adapters
- [ ] NO domain code depends on infrastructure

---

## Common Mistakes to Avoid

1. ❌ Keeping business logic in entities
2. ❌ Using `LocalDateTime` instead of `Instant`
3. ❌ Putting mapping logic in adapters (use mappers)
4. ❌ Making domain depend on JPA interfaces
5. ❌ Forgetting to update `@UpdateTimestamp` type
6. ❌ Not testing Instant persistence

---

## Next Task

**Task 05:** Inventory Service - Web Layer Updates

---

**Status:** 🔄 READY TO START (AI Agent: Execute after Task 03 completion and validation)
