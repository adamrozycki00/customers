package com.example.customers.application.adapter;

import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;

public class InMemoryGenericFieldDbAdapter implements GenericFieldPort {

  private final List<GenericField> fields = new ArrayList<>();

  @Getter
  private Set<String> lastExpectedNames;

  @Override
  public GenericField insert(GenericField field) {
    fields.add(field);
    return field;
  }

  @Override
  public List<GenericField> findAllByTenantIncluding(TenantId tenantId, @NonNull Set<String> expectedNames) {
    lastExpectedNames = expectedNames;
    return fields.stream()
        .filter(field -> field.getTenantId().equals(tenantId))
        .toList();
  }
}
