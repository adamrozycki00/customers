package com.example.customers.application.service;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GenericFieldService implements RegisterGenericFieldUseCase {

  private final GenericFieldPort genericFieldPort;

  @Override
  public GenericField register(Command command) {
    var tenantId = new TenantId(command.tenantId());
    var name = new FieldName(command.name());
    var type = FieldType.of(command.type());

    var field = GenericField.builder()
        .tenantId(tenantId)
        .name(name)
        .type(type)
        .build();

    return genericFieldPort.insert(field);
  }
}
