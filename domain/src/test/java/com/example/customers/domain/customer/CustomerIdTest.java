package com.example.customers.domain.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CustomerIdTest {

  @Nested
  class ValidCustomerIds {

    @Test
    void shouldKeepValidCustomerIdVerbatim() {
      String inputValidCustomerId = "Cust_01-A";

      var resultCustomerId = new CustomerId(inputValidCustomerId);

      assertThat(resultCustomerId.value()).isEqualTo("Cust_01-A");
    }

    @Test
    void shouldAcceptMaxLengthCustomerId() {
      String inputMaxLengthCustomerId = "a".repeat(128);

      var resultCustomerId = new CustomerId(inputMaxLengthCustomerId);

      assertThat(resultCustomerId.value()).hasSize(128);
    }

    @Test
    void shouldTreatLowerCaseAndUpperCaseCustomerIdsAsDifferent() {
      String inputLowerCaseCustomerId = "cust-1";
      String inputUpperCaseCustomerId = "CUST-1";

      var resultLowerCaseCustomerId = new CustomerId(inputLowerCaseCustomerId);
      var resultUpperCaseCustomerId = new CustomerId(inputUpperCaseCustomerId);

      assertThat(resultLowerCaseCustomerId).isNotEqualTo(resultUpperCaseCustomerId);
    }
  }

  @Nested
  class InvalidCustomerIds {

    @Test
    void shouldRejectNullCustomerId() {
      String inputNullCustomerId = null;

      var thrown = catchThrowable(() -> new CustomerId(inputNullCustomerId));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Id must be 1 to 128 letters, digits, '_' or '-'");
    }

    @Test
    void shouldRejectEmptyCustomerId() {
      String inputEmptyCustomerId = "";

      var thrown = catchThrowable(() -> new CustomerId(inputEmptyCustomerId));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Id must be 1 to 128 letters, digits, '_' or '-'");
    }

    @Test
    void shouldRejectCustomerIdWithInvalidCharacters() {
      String inputCustomerIdWithInvalidCharacters = "CUST 1";

      var thrown = catchThrowable(() -> new CustomerId(inputCustomerIdWithInvalidCharacters));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Id must be 1 to 128 letters, digits, '_' or '-'");
    }

    @Test
    void shouldRejectTooLongCustomerId() {
      String inputTooLongCustomerId = "a".repeat(129);

      var thrown = catchThrowable(() -> new CustomerId(inputTooLongCustomerId));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Id must be 1 to 128 letters, digits, '_' or '-'");
    }
  }
}
