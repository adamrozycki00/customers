package com.example.customers.domain.customer;

import com.example.customers.domain.customer.exception.InvalidPublicIdException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PublicIdTest {

  @Nested
  class ValidPublicIds {

    @Test
    void shouldKeepValidPublicIdVerbatim() {
      String inputValidPublicId = "Cust_01-A";

      var resultPublicId = new PublicId(inputValidPublicId);

      assertThat(resultPublicId.value()).isEqualTo("Cust_01-A");
    }

    @Test
    void shouldAcceptMaxLengthPublicId() {
      String inputMaxLengthPublicId = "a".repeat(128);

      var resultPublicId = new PublicId(inputMaxLengthPublicId);

      assertThat(resultPublicId.value()).hasSize(128);
    }

    @Test
    void shouldTreatLowerCaseAndUpperCasePublicIdsAsDifferent() {
      String inputLowerCasePublicId = "cust-1";
      String inputUpperCasePublicId = "CUST-1";

      var resultLowerCasePublicId = new PublicId(inputLowerCasePublicId);
      var resultUpperCasePublicId = new PublicId(inputUpperCasePublicId);

      assertThat(resultLowerCasePublicId).isNotEqualTo(resultUpperCasePublicId);
    }
  }

  @Nested
  class InvalidPublicIds {

    @Test
    void shouldRejectNullPublicId() {
      String inputNullPublicId = null;

      var thrown = catchThrowable(() -> new PublicId(inputNullPublicId));

      assertThat(thrown).isInstanceOf(InvalidPublicIdException.class);
    }

    @Test
    void shouldRejectEmptyPublicId() {
      String inputEmptyPublicId = "";

      var thrown = catchThrowable(() -> new PublicId(inputEmptyPublicId));

      assertThat(thrown).isInstanceOf(InvalidPublicIdException.class);
    }

    @Test
    void shouldRejectPublicIdWithInvalidCharacters() {
      String inputPublicIdWithInvalidCharacters = "CUST 1";

      var thrown = catchThrowable(() -> new PublicId(inputPublicIdWithInvalidCharacters));

      assertThat(thrown).isInstanceOf(InvalidPublicIdException.class);
    }

    @Test
    void shouldRejectTooLongPublicId() {
      String inputTooLongPublicId = "a".repeat(129);

      var thrown = catchThrowable(() -> new PublicId(inputTooLongPublicId));

      assertThat(thrown).isInstanceOf(InvalidPublicIdException.class);
    }
  }
}
