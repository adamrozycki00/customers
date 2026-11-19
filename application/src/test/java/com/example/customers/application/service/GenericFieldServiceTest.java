package com.example.customers.application.service;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.customers.application.adapter.InMemoryGenericFieldDbAdapter;
import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.application.port.in.RegisterGenericFieldUseCase.Command;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GenericFieldServiceTest {

  private static final Set<String> NO_EXPECTED_NAMES = emptySet();

  private final InMemoryGenericFieldDbAdapter repo = new InMemoryGenericFieldDbAdapter();

  private final RegisterGenericFieldUseCase underTest = new GenericFieldService(repo);

  @Test
  void shouldReturnRegisteredField() {
    var inputCommand = Command.builder()
        .tenantId("flowers")
        .name("vip")
        .type("BOOL")
        .build();
    var expectedTenantId = new TenantId("flowers");
    var expectedName = new FieldName("vip");
    var expectedField = GenericField.builder()
        .tenantId(expectedTenantId)
        .name(expectedName)
        .type(FieldType.BOOL)
        .build();

    var resultField = underTest.register(inputCommand);

    assertThat(resultField).usingRecursiveComparison().isEqualTo(expectedField);
  }

  @Test
  void shouldStoreRegisteredField() {
    var inputCommand = Command.builder()
        .tenantId("flowers")
        .name("vip")
        .type("BOOL")
        .build();
    var tenantId = new TenantId("flowers");
    var expectedName = new FieldName("vip");

    underTest.register(inputCommand);

    var resultStoredFields = repo.findAllByTenantIncluding(tenantId, NO_EXPECTED_NAMES);
    assertThat(resultStoredFields)
        .extracting(GenericField::getName)
        .containsExactly(expectedName);
  }
}
