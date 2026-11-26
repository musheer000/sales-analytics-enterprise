# 🏛️ Architecture Documentation

## 1. Layered Architecture Flow
```
Client (Browser / React / Angular / Postman)
      │
      ▼
Controller Layer (@RestController)
  - Maps HTTP endpoints (/api/*)
  - Validates request payloads (@Valid)
  - Formats ResponseEntity<ApiResponse<T>>
      │
      ▼
Service Layer (@Service)
  - Business logic & validations
  - Spring Cache (@Cacheable, @CachePut, @CacheEvict)
  - Transaction boundaries (@Transactional)
      │
      ▼
Mapper Layer (@Component)
  - Bidirectional transformation between Entities and DTOs
      │
      ▼
Repository Layer (Spring Data JPA)
  - Extends JpaRepository<T, ID>
  - Custom JPQL queries for analytical aggregations
      │
      ▼
Hibernate ORM & MySQL 8.0 Database (sales_analytics)
```

## 2. Spring Cache Lifecycle
- **Cache Hit:** Request -> Controller -> Service -> Cache Manager -> Returns cached result immediately.
- **Cache Miss:** Request -> Controller -> Service -> Cache Manager -> Repository -> MySQL -> Cache stored -> Return response.
- **Cache Invalidation:** Mutations (`POST`, `PUT`, `DELETE`) trigger `@CacheEvict(allEntries = true)` to keep reporting stats synchronized.
