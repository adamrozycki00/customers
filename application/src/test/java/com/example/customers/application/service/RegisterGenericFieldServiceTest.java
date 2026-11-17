package com.example.customers.application.service;

import com.example.customers.application.adapter.InMemoryGenericFieldDbAdapter;
import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.application.port.in.RegisterGenericFieldUseCase.Command;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterGenericFieldServiceTest {

  private final InMemoryGenericFieldDbAdapter repo = new InMemoryGenericFieldDbAdapter();
  private final RegisterGenericFieldUseCase underTest = new RegisterGenericFieldService(repo);

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

    var resultField = underTest.register(inputCommand);

    assertThat(resultField).usingRecursiveComparison().isEqualTo(expectedField);
  }

  @Test
  void shouldStoreRegisteredField() {
    var inputCommand = Command.builder()
        .tenantId("flowers")
        .name("vip")
        .type("BOOLEAN")
        .build();
    var tenantId = new TenantId("flowers");
    var expectedName = new FieldName("vip");

    underTest.register(inputCommand);

    var resultStoredFields = repo.findAllByTenant(tenantId);
    assertThat(resultStoredFields)
        .extracting(GenericField::getName)
        .containsExactly(expectedName);
  }
}
