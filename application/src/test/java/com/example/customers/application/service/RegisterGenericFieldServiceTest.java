package com.example.customers.application.service;

import com.example.customers.application.adapter.InMemoryGenericFieldRepository;
import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.application.port.in.RegisterGenericFieldUseCase.Command;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterGenericFieldServiceTest {

  private final InMemoryGenericFieldRepository repo = new InMemoryGenericFieldRepository();
  private final RegisterGenericFieldUseCase registerGenericField = new RegisterGenericFieldService(repo);

  @Test
  void shouldReturnRegisteredField() {
    var inputCommand = Command.builder()
        .tenantId("flowers")
        .name("vip")
        .type("BOOLEAN")
        .build();
    var expectedTenantId = new TenantId("flowers");
    var expectedName = new FieldName("vip");
    var expectedField = GenericField.builder()
        .tenantId(expectedTenantId)
        .name(expectedName)
        .type(FieldType.BOOLEAN)
        .build();

    var resultField = registerGenericField.register(inputCommand);

    assertThat(resultField).usingRecursiveComparison().isEqualTo(expectedField);
  }
}
