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

## API

The API is specified contract-first in OpenAPI: [`infrastructure/src/main/resources/openapi.yaml`](infrastructure/src/main/resources/openapi.yaml). The controller interfaces and request/response classes are generated from it at build time. JSON properties use `snake_case`.

Every request must carry the `X-Tenant-Id` header.

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/generic-fields` | Register a generic field `{name, type}` |
| `POST` | `/customers` | Create a customer |
| `GET` | `/customers/{publicId}` | Read a customer |
| `GET` | `/customers?page=&size=` | List the tenant's customers |
| `PATCH` | `/customers/{publicId}` | Partially update a customer |
| `DELETE` | `/customers/{publicId}` | Delete a customer |

Example customer payload:

```json
{
  "public_id": "C-1",
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

### Data and storage

1. MongoDB is used as a DB technology. Two reasons: flexible customer schema, straightforward atomic partial update.
2. Field names are case-sensitive, because tenants author these names and expect them preserved verbatim. They are limited to `[A-Za-z][A-Za-z0-9_]*` characters and at most 64 characters, so the example "date of birth" is registered as e.g. `date_of_birth`.
3. Customer index `_id` in the DB is composed of the `tenantId` and `publicId` fields, always in that key order. The public id is unique per tenant, case-sensitive (`CUST-1` and `cust-1` are different customers), limited to `[A-Za-z0-9_-]` characters and at most 128 characters, and permanent (not reusable after deletion).
4. Generic field values are stored in a nested `genericFields` sub-document (left out when a customer is created without any; after the last value is cleared by PATCH, an empty sub-document may remain, which reads the same as a missing one), so tenant-defined names can never collide with the customer's own fields. All generic field values are optional.
5. For concurrency and backward-compatibility reasons, once a field definition is registered, it cannot be modified (renamed, retyped, or deleted). Each field definition is its own document with `_id` composed of `tenantId` and `name` (in that key order), so a duplicate name is rejected by the DB with 409.
6. Supported field types are boolean, string, integer and ISO-8601 datetime. Integers are 64-bit. String values are at most 1024 characters, and first and last names at most 256. A name, when present, must not be empty or blank; to have no name, the field is left out. Leading and trailing whitespace is removed from names; whitespace inside a name is kept.
7. For consistent date storage, API fields of type ISO-8601 datetime are stored in Mongo as Date type, i.e. as a UTC instant. Type conversion is handled by the application logic. A value without an offset is interpreted as UTC (`Z`). The original offset is not preserved, precision is truncated to milliseconds, and a date-only value given with a non-UTC offset may land on a different calendar day in UTC (e.g. `1990-05-20T00:00:00+02:00` is stored as `1990-05-19T22:00:00Z`).
8. The number of generic fields per tenant is not capped, but is assumed to stay small, because people register them by hand.
9. Expected a small number of tenants and a small number of register_field requests; expected a large number of customers and therefore possibly large number of overall customer modification requests, but a small number of modification requests targetting a given customer. No cache is used: every customer write reads the tenant's generic field definitions from the DB in one index-backed query over a few small documents, so a newly registered field can be used immediately.


### API behaviour

9. Tenant ID is passed in the `X-Tenant-Id` header rather than as part of the path, as there is no requirement that tenants are RESTful resources. A missing or blank header is rejected with 400. The header is trusted, because authorization is out of scope.
10. Different HTTP methods are used for customer creation (POST) and updating (PATCH), in order to take advantage of unique inserts in Mongo. A duplicate public id gets 409.
11. PATCH (`application/json`) changes only what the request mentions. `first_name` and `last_name` are replaced when present and kept when absent or `null`; names cannot be cleared. Inside `generic_fields`, a value sets the field, `null` clears it, and an absent key keeps it; `generic_fields` itself absent or `null` keeps all generic fields. The public id cannot be changed. Request bodies are strict: an unknown property (including `public_id` in a PATCH) is rejected with 400.
12. A generic field value whose name is not registered for the tenant, or whose type does not match, is rejected with 400. Nothing is dropped silently.
13. The regular delete semantics for customers is soft deletion (a `deletedAt` timestamp, set on the first deletion only), for better traceability. GDPR-related hard deletion should be handled independently of the regular delete use case. POST requests with a soft-deleted public id get 409. GET and PATCH on a soft-deleted customer get 404, and soft-deleted customers are excluded from listings. DELETE is idempotent and always returns 204, including for a customer that never existed.
14. Listing uses offset pagination with 1-based page numbers (`page` default 1, `size` default 20, max 100), ordered by public id. The default and maximum page sizes are configurable in `application.yaml` (`customers.paging.default-page-size`, `customers.paging.max-page-size`).

### Concurrency

15. Each generic field is stored as its own document with a unique `_id` of `{tenantId, name}`, and is only ever inserted, never updated. A duplicate field name is therefore rejected by the database (409), even under concurrent registrations.
16. Customer updates are not optimistically locked. Each PATCH updates fields atomically, and concurrent writes to the same field resolve as last-write-wins.
17. Expected a small number of tenants and a small number of register_field requests; expected a large number of customers and therefore possibly large number of overall customer modification requests, but a small number of modification requests targetting a given customer.

### Structure

17. The code follows Domain-Driven Design and is split into three Maven modules: `domain` (no framework dependencies), `application` (use cases) and `infrastructure` (MongoDB and REST adapters).
