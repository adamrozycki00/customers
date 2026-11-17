package com.example.customers.application.adapter;

import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;

import java.util.ArrayList;
import java.util.List;

public class InMemoryGenericFieldDbAdapter implements GenericFieldPort {

  private final List<GenericField> fields = new ArrayList<>();

  @Override
  public GenericField insert(GenericField field) {
    fields.add(field);
    return field;
  }

  @Override
  public List<GenericField> findAllByTenant(TenantId tenantId) {
    return fields.stream()
        .filter(field -> field.getTenantId().equals(tenantId))
        .toList();
  }
}
