# Optimistic Locking Guidelines

## Overview

This document defines mandatory patterns for using JPA Optimistic Locking (`@Version`) to prevent concurrent update conflicts while maintaining data integrity.

---

## Rule: NEVER manually set ID and version together

When using `@Version` for optimistic locking, you MUST follow these patterns:

### ✅ Pattern 1: New Entities (INSERT)

- **Do NOT set `id`** in builder/constructor
- **Do NOT set `version`** in builder/constructor  
- Let JPA/Hibernate generate both fields automatically

```java
// ✅ CORRECT: New entity
ReservationEntity entity = ReservationEntity.builder()
    .orderId(orderId)
    .status(status)
    .expiresAt(expiresAt)
    // NO .id() - let Hibernate generate
    // NO .version() - let Hibernate generate
    .build();
```

### ✅ Pattern 2: Existing Entities (UPDATE)

- **Load entity from DB first** using `findById()`
- **Update only the changed fields** via setters
- **NEVER create new entity** with old ID and null version

```java
// ✅ CORRECT: Update existing entity
Optional<ReservationEntity> existing = repository.findById(id);
if (existing.isPresent()) {
    ReservationEntity entity = existing.get();
    entity.setStatus(newStatus);          // Update fields
    entity.setExpiresAt(newExpiresAt);    // Update fields
    repository.save(entity);               // Version auto-incremented
}
```

---

## Mapper Pattern (Domain ↔ Entity)

Mappers MUST provide separate methods for new vs existing entities:

### 1. `toEntity()` - For NEW entities

```java
/**
 * Converts domain model to NEW JPA entity (for inserts).
 * Does NOT set ID or version - lets JPA generate them.
 */
public ReservationEntity toEntity(Reservation domain) {
    return ReservationEntity.builder()
        .orderId(domain.getOrderId())
        .status(domain.getStatus())
        // NO .id() - new entity
        // NO .version() - new entity
        .build();
}
```

### 2. `updateEntity()` - For UPDATING existing entities

```java
/**
 * Updates an existing JPA entity with domain model data.
 * Preserves ID and version for optimistic locking.
 */
public void updateEntity(Reservation domain, ReservationEntity entity) {
    // Update fields (preserve ID and version)
    entity.setOrderId(domain.getOrderId());
    entity.setStatus(domain.getStatus());
    entity.setExpiresAt(domain.getExpiresAt());
    // ID and version are NOT touched
}
```

---

## Repository Adapter Pattern

Repository adapters MUST check entity existence before deciding INSERT vs UPDATE:

```java
@Override
public Reservation save(Reservation reservation) {
    // Check if entity exists in DB
    Optional<ReservationEntity> existingEntity = jpaRepository.findById(reservation.getId());
    
    ReservationEntity entityToSave;
    if (existingEntity.isPresent()) {
        // UPDATE: Use existing entity (preserves version)
        entityToSave = existingEntity.get();
        mapper.updateEntity(reservation, entityToSave);
    } else {
        // INSERT: Create new entity (Hibernate generates ID & version)
        entityToSave = mapper.toEntity(reservation);
    }
    
    ReservationEntity savedEntity = jpaRepository.save(entityToSave);
    return mapper.toDomain(savedEntity);
}
```

---

## ❌ Anti-Patterns (WRONG)

### Anti-pattern 1: Manual ID + Null Version

```java
// ❌ WRONG: Setting ID manually with null version
Entity entity = Entity.builder()
    .id(domainId)      // ❌ Manual ID
    .version(null)     // ❌ Null version
    .build();

repository.save(entity); 
// 💥 Hibernate error: "Detached entity with generated id has uninitialized version"
```

### Anti-pattern 2: Creating New Entity Instead of Updating

```java
// ❌ WRONG: Creating new entity for update
public Entity save(DomainModel domain) {
    // Missing existence check!
    Entity entity = mapper.toEntity(domain); // Always creates new
    return repository.save(entity);          // ❌ Fails if exists
}
```

### Anti-pattern 3: Single Mapper Method for Both Cases

```java
// ❌ WRONG: One method for both new and existing
public Entity toEntity(DomainModel domain) {
    return Entity.builder()
        .id(domain.getId())  // ❌ Problem for new entities
        .version(null)       // ❌ Problem if entity exists
        .build();
}
```

---

## Error Symptoms

If you see these errors, you have an Optimistic Lock integration problem:

```
org.hibernate.PropertyValueException: Detached entity with generated id 
'...' has an uninitialized version value 'null'
```

**Solution:** Apply the patterns above - separate new vs existing entity handling.

---

## Summary Checklist

✅ Use `@Version` on all entities with concurrent access  
✅ Never manually set both `id` and `version=null`  
✅ Mappers have separate `toEntity()` and `updateEntity()` methods  
✅ Repository adapters check existence before save  
✅ New entities: No ID, no version (Hibernate generates)  
✅ Existing entities: Load from DB, update fields, preserve version  

---

## Related Documentation

- Hexagonal Architecture: `docs/architecture/hexagonal-architecture.md`
- JPA Best Practices: `docs/architecture/jpa-patterns.md`
- Domain Model Guidelines: `docs/architecture/domain-driven-design.md`
