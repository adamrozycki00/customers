package com.example.customers.domain.customer;

import com.example.customers.domain.customer.exception.InvalidCustomerException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.isNull;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class Customer {

  @EqualsAndHashCode.Include
  private final TenantId tenantId;
  @EqualsAndHashCode.Include
  private final PublicId publicId;
  private final FirstName firstName;
  private final LastName lastName;
  private final Map<FieldName, GenericFieldValue> genericFields;

  @Builder
  private Customer(TenantId tenantId, PublicId publicId, FirstName firstName, LastName lastName,
      Map<FieldName, GenericFieldValue> genericFields) {
    validate(tenantId, publicId);

    this.tenantId = tenantId;
    this.publicId = publicId;
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

  private static void validate(TenantId tenantId, PublicId publicId) {
    if (isNull(tenantId) || isNull(publicId)) {
      throw new InvalidCustomerException();
    }
  }
}
