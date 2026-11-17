package com.example.customers.application.port.out;

import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;

import java.util.List;

public interface GenericFieldPort {

  GenericField insert(GenericField field);

  List<GenericField> findAllByTenant(TenantId tenantId);
}
