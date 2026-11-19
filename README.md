# Customers

A multi-tenant service that stores customer records. Each tenant can register its own typed generic fields.

## Build and run

Prerequisites: JDK 25 and a running Docker daemon. Maven is provided through the wrapper.

```shell
# Unit tests and MongoDB adapter tests (the adapter tests start MongoDB via Testcontainers)
./mvnw test

# Everything above plus the end-to-end tests (*IT, package com.example.customers.it)
./mvnw verify

# Run the application (MongoDB is started automatically from compose.yaml)
./mvnw install -DskipTests
./mvnw -pl infrastructure spring-boot:run
```

### Try it

The application listens on `http://localhost:8080`. Stopping it (Ctrl+C) also stops the MongoDB container it started; the container is kept, so its data survives a restart.

```shell
# Register a generic field for tenant "flowers"
curl -X POST localhost:8080/generic-fields -H 'X-Tenant-Id: flowers' -H 'Content-Type: application/json' \
  -d '{"name": "vip", "type": "BOOL"}'

# Create a customer with a value for it
curl -X POST localhost:8080/customers -H 'X-Tenant-Id: flowers' -H 'Content-Type: application/json' \
  -d '{"id": "CUST-1", "first_name": "Anna", "generic_fields": {"vip": true}}'

# Read it back
curl localhost:8080/customers/CUST-1 -H 'X-Tenant-Id: flowers'

# Health, including MongoDB
curl localhost:8080/actuator/health
```

## API

The API is specified contract-first in OpenAPI: [`infrastructure/src/main/resources/openapi.yaml`](infrastructure/src/main/resources/openapi.yaml). The controller interfaces and request/response classes are generated from it at build time. JSON properties use `snake_case`.

Every request must carry the `X-Tenant-Id` header.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/generic-fields` | Register a generic field `{name, type}` |
| `POST` | `/customers` | Create a customer |
| `GET` | `/customers/{id}` | Read a customer |
| `GET` | `/customers?page=&size=` | List the tenant's customers |
| `PATCH` | `/customers/{id}` | Partially update a customer |
| `DELETE` | `/customers/{id}` | Delete a customer |

Example customer payload:

```json
{
  "id": "C-1",
  "first_name": "Ann",
  "generic_fields": {
    "vip": true,
    "score": 42,
    "date_of_birth": "1990-05-20T00:00:00Z"
  }
}
```

Errors are returned as RFC 9457 Problem Details (`application/problem+json`).

## Assumptions

The premises the design rests on. If one of them stops being true, the part of the design named after its arrow needs revisiting.

### Domain

1. Tenants are created and managed outside this service, which knows a tenant only by its id. The tenant id the caller sends is trusted, because authentication and authorization happen upstream. Tenant ids are case-insensitive (`flowers` and `FLOWERS` are one tenant).
   → no `Tenant` aggregate; tenant id in a header.
2. The customer id is the customer's only identity. It never changes, is never reused (not even after deletion), is case-sensitive, and is a URL-safe code (letters, digits, `_`, `-`, at most 128 characters).
   → `{tenantId, id}` as the MongoDB `_id`; uniqueness enforced by the database, no surrogate key.
3. Deleting a customer must stay traceable. Erasing personal data (e.g. for GDPR) is a separate concern, not the regular delete.
   → soft delete.
4. A generic field, once registered, is never renamed, retyped or deleted.
   → no transaction between fields and customers; a cached copy of a tenant's fields can only be missing a field, never be wrong.

### Usage and load

5. There are few tenants (up to 100) with few fields each (10 on average). There are many customers and many writes in total, but few per customer.
   → all field definitions are cached in memory without a size limit; customers are not cached.
6. Concurrent updates of the same customer are rare.
   → no optimistic locking; per-field atomic PATCH, last write wins per field.
7. Customers are only read by id or listed page by page in id order. Nobody searches or sorts by generic field values, and soft-deleted customers stay a small share of a tenant's customers.
   → no indexes on generic field values or the soft-delete marker; listing is a range scan on `_id`.

### Operation

8. The service runs as several instances, each with its own generic field cache.
   → the field cache reloads a tenant when a request names a field it does not know.
9. MongoDB outages are short. During one, the service should stay up and answer 503, not be restarted or taken out of the load balancer.
   → no DB check at startup or in readiness; reads retried briefly.
10. Customer names and generic field values are personal data and must not be logged. Tenant ids and customer ids may be logged.
    → request logs hold only method, path, tenant, status and duration, never bodies.
