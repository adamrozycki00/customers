package com.example.customers.domain.tenant;

import com.example.customers.domain.tenant.exception.InvalidTenantIdException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TenantIdTest {

  @Nested
  class ValidTenantIds {

    @Test
    void shouldKeepValidTenantIdVerbatim() {
      String inputValidTenantId = "flowers";

      var resultTenantId = new TenantId(inputValidTenantId);

      assertThat(resultTenantId.value()).isEqualTo("flowers");
    }

    @Test
    void shouldAcceptMaxLengthTenantId() {
      String inputMaxLengthTenantId = "a".repeat(64);

      var resultTenantId = new TenantId(inputMaxLengthTenantId);

      assertThat(resultTenantId.value()).hasSize(64);
    }

    @Test
    void shouldTreatLowerCaseAndUpperCaseTenantIdsAsDifferent() {
      String inputLowerCaseTenantId = "flowers";
      String inputUpperCaseTenantId = "Flowers";

      var resultLowerCaseTenantId = new TenantId(inputLowerCaseTenantId);
      var resultUpperCaseTenantId = new TenantId(inputUpperCaseTenantId);

      assertThat(resultLowerCaseTenantId).isNotEqualTo(resultUpperCaseTenantId);
    }
  }

  @Nested
  class InvalidTenantIds {

    @Test
    void shouldRejectNullTenantId() {
      String inputNullTenantId = null;

      var thrown = catchThrowable(() -> new TenantId(inputNullTenantId));

      assertThat(thrown).isInstanceOf(InvalidTenantIdException.class);
    }

    @Test
    void shouldRejectEmptyTenantId() {
      String inputEmptyTenantId = "";

      var thrown = catchThrowable(() -> new TenantId(inputEmptyTenantId));

      assertThat(thrown).isInstanceOf(InvalidTenantIdException.class);
    }

    @Test
    void shouldRejectTenantIdWithInvalidCharacters() {
      String inputTenantIdWithInvalidCharacters = "flowers.inc";

      var thrown = catchThrowable(() -> new TenantId(inputTenantIdWithInvalidCharacters));

      assertThat(thrown).isInstanceOf(InvalidTenantIdException.class);
    }

    @Test
    void shouldRejectTooLongTenantId() {
      String inputTooLongTenantId = "a".repeat(65);

      var thrown = catchThrowable(() -> new TenantId(inputTooLongTenantId));

      assertThat(thrown).isInstanceOf(InvalidTenantIdException.class);
    }
  }
}
