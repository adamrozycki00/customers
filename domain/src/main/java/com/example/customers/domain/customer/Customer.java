package com.example.customers.domain.customer;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;

import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Aggregate root: a tenant's customer, identified by tenant id and the client-assigned customer id.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Customer {

  @EqualsAndHashCode.Include
  private final TenantId tenantId;
  @EqualsAndHashCode.Include
  private final CustomerId id;
  private final FirstName firstName;
  private final LastName lastName;
  private final Map<FieldName, GenericFieldValue> genericFields;

  @Builder
  private Customer(TenantId tenantId, CustomerId id, FirstName firstName, LastName lastName,
                   Map<FieldName, GenericFieldValue> genericFields) {
    validate(tenantId, id);

    this.tenantId = tenantId;
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.genericFields = makeImmutable(genericFields);
  }

  private static Map<FieldName, GenericFieldValue> makeImmutable(Map<FieldName, GenericFieldValue> genericFields) {
    if (isNull(genericFields)) {
      return emptyMap();
    }

    var orderedCopy = new LinkedHashMap<>(genericFields);
    return unmodifiableMap(orderedCopy);
  }

  private static void validate(TenantId tenantId, CustomerId id) {
    if (isNull(tenantId) || isNull(id)) {
      throw new ValidationException("Customer requires a tenant id and an id");
    }
  }
}
