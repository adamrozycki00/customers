package com.example.customers.domain.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CustomerIdTest {

  @Nested
  class ValidIds {

    @Test
    void shouldKeepValidIdVerbatim() {
      String inputValidId = "Cust_01-A";

      var resultId = new CustomerId(inputValidId);

      assertThat(resultId.value()).isEqualTo("Cust_01-A");
    }

    @Test
    void shouldAcceptMaxLengthId() {
      String inputMaxLengthId = "a".repeat(128);

      var resultId = new CustomerId(inputMaxLengthId);

      assertThat(resultId.value()).hasSize(128);
    }

    @Test
    void shouldTreatLowerCaseAndUpperCaseIdsAsDifferent() {
      String inputLowerCaseId = "cust-1";
      String inputUpperCaseId = "CUST-1";

      var resultLowerCaseId = new CustomerId(inputLowerCaseId);
      var resultUpperCaseId = new CustomerId(inputUpperCaseId);

      assertThat(resultLowerCaseId).isNotEqualTo(resultUpperCaseId);
    }
  }

  @Nested
  class InvalidIds {

    @Test
    void shouldRejectNullId() {
      String inputNullId = null;

      var thrown = catchThrowable(() -> new CustomerId(inputNullId));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Id must be 1 to 128 letters, digits, '_' or '-'");
    }

    @Test
    void shouldRejectEmptyId() {
      String inputEmptyId = "";

      var thrown = catchThrowable(() -> new CustomerId(inputEmptyId));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Id must be 1 to 128 letters, digits, '_' or '-'");
    }

    @Test
    void shouldRejectIdWithInvalidCharacters() {
      String inputIdWithInvalidCharacters = "CUST 1";

      var thrown = catchThrowable(() -> new CustomerId(inputIdWithInvalidCharacters));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Id must be 1 to 128 letters, digits, '_' or '-'");
    }

    @Test
    void shouldRejectTooLongId() {
      String inputTooLongId = "a".repeat(129);

      var thrown = catchThrowable(() -> new CustomerId(inputTooLongId));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Id must be 1 to 128 letters, digits, '_' or '-'");
    }
  }
}
