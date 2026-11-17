package com.example.customers.application.service;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.application.port.in.RegisterGenericFieldUseCase.Command;
import com.example.customers.application.port.out.GenericFieldRepository;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RegisterGenericFieldService implements RegisterGenericFieldUseCase {

  private final GenericFieldRepository repo;

  @Override
  public GenericField register(Command command) {
    var tenantId = new TenantId(command.tenantId());
    var name = new FieldName(command.name());
    var type = FieldType.of(command.type());

    return GenericField.builder()
        .tenantId(tenantId)
        .name(name)
        .type(type)
        .build();
  }
}
