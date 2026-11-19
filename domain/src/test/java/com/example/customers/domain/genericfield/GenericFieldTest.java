package com.example.customers.domain.genericfield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.tenant.TenantId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GenericFieldTest {

  private final TenantId validTenantId = new TenantId("flowers");
  private final FieldName validName = new FieldName("vip");

  @Nested
  class Equality {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
      var booleanField = GenericField.builder()
          .tenantId(validTenantId)
          .name(validName)
          .type(FieldType.BOOL)
          .build();
      var sameFieldWithOtherType = GenericField.builder()
          .tenantId(validTenantId)
          .name(validName)
          .type(FieldType.STRING)
          .build();

      var resultEquality = booleanField.equals(sameFieldWithOtherType);

      assertThat(resultEquality).isTrue();
    }

    @Test
    void shouldNotBeEqualWhenNamesDiffer() {
      var otherName = new FieldName("score");
      var field = GenericField.builder()
          .tenantId(validTenantId)
          .name(validName)
          .type(FieldType.BOOL)
          .build();
      var otherField = GenericField.builder()
          .tenantId(validTenantId)
          .name(otherName)
          .type(FieldType.BOOL)
          .build();

      var resultEquality = field.equals(otherField);

      assertThat(resultEquality).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenTenantIdsDiffer() {
      var otherTenantId = new TenantId("roses");
      var field = GenericField.builder()
          .tenantId(validTenantId)
          .name(validName)
          .type(FieldType.BOOL)
          .build();
      var otherTenantsField = GenericField.builder()
          .tenantId(otherTenantId)
          .name(validName)
          .type(FieldType.BOOL)
          .build();

      var resultEquality = field.equals(otherTenantsField);

      assertThat(resultEquality).isFalse();
    }
  }

  @Nested
  class InvalidGenericFields {

    @Test
    void shouldRejectMissingTenantId() {
      TenantId inputMissingTenantId = null;

      var thrown = catchThrowable(() -> GenericField.builder()
          .tenantId(inputMissingTenantId)
          .name(validName)
          .type(FieldType.BOOL)
          .build());

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectNullName() {
      FieldName inputNullName = null;

      var thrown = catchThrowable(() -> GenericField.builder()
          .tenantId(validTenantId)
          .name(inputNullName)
          .type(FieldType.BOOL)
          .build());

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectNullType() {
      FieldType inputNullType = null;

      var thrown = catchThrowable(() -> GenericField.builder()
          .tenantId(validTenantId)
          .name(validName)
          .type(inputNullType)
          .build());

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }
  }
}
