package com.example.customers.domain.genericfield;

import static java.util.Objects.isNull;

import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.tenant.TenantId;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Aggregate root: a tenant's typed generic field definition, identified by tenant id and name.
 */
@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class GenericField {

  @EqualsAndHashCode.Include
  private final TenantId tenantId;

  @EqualsAndHashCode.Include
  private final FieldName name;

  private final FieldType type;

  @Builder
  private GenericField(TenantId tenantId, FieldName name, FieldType type) {
    validate(tenantId, name, type);

    this.tenantId = tenantId;
    this.name = name;
    this.type = type;
  }

  private static void validate(TenantId tenantId, FieldName name, FieldType type) {
    if (isNull(tenantId) || isNull(name) || isNull(type)) {
      throw new ValidationException("Generic field requires a tenant id, a name and a type");
    }
  }
}
