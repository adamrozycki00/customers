package com.example.customers.application.port.out;

import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;

import java.util.List;
import java.util.Set;
import lombok.NonNull;

public interface GenericFieldPort {

  GenericField insert(GenericField field);

  /**
   * Returns all generic fields of the tenant. If any of the expected names is missing from a cached result,
   * the result is refreshed from the source before it is returned. The expected names never filter the result.
   */
  List<GenericField> findAllByTenantIncluding(TenantId tenantId, @NonNull Set<String> expectedNames);
}
