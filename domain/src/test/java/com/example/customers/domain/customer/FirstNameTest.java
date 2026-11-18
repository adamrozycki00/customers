package com.example.customers.domain.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FirstNameTest {

  @Nested
  class ValidFirstNames {

    @Test
    void shouldTrimSurroundingWhitespace() {
      String inputFirstNameWithSurroundingWhitespace = " Anna-Maria ";

      var resultFirstName = new FirstName(inputFirstNameWithSurroundingWhitespace);

      assertThat(resultFirstName.value()).isEqualTo("Anna-Maria");
    }

    @Test
    void shouldKeepInnerWhitespace() {
      String inputFirstNameWithInnerWhitespace = "Anna Maria";

      var resultFirstName = new FirstName(inputFirstNameWithInnerWhitespace);

      assertThat(resultFirstName.value()).isEqualTo("Anna Maria");
    }

    @Test
    void shouldAcceptMaxLengthFirstName() {
      String inputMaxLengthFirstName = "a".repeat(256);

      var resultFirstName = new FirstName(inputMaxLengthFirstName);

      assertThat(resultFirstName.value()).hasSize(256);
    }

    @Test
    void shouldApplyLengthLimitAfterTrimming() {
      String maxLengthName = "a".repeat(256);
      String inputMaxLengthFirstNameWithSurroundingWhitespace = " " + maxLengthName + " ";

      var resultFirstName = new FirstName(inputMaxLengthFirstNameWithSurroundingWhitespace);

      assertThat(resultFirstName.value()).hasSize(256);
    }
  }

  @Nested
  class InvalidFirstNames {

    @Test
    void shouldRejectNullFirstName() {
      String inputNullFirstName = null;

      var thrown = catchThrowable(() -> new FirstName(inputNullFirstName));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("First name must not be blank and must be at most 256 characters");
    }

    @Test
    void shouldRejectBlankFirstName() {
      String inputBlankFirstName = "   ";

      var thrown = catchThrowable(() -> new FirstName(inputBlankFirstName));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("First name must not be blank and must be at most 256 characters");
    }

    @Test
    void shouldRejectTooLongFirstName() {
      String inputTooLongFirstName = "a".repeat(257);

      var thrown = catchThrowable(() -> new FirstName(inputTooLongFirstName));

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("First name must not be blank and must be at most 256 characters");
    }
  }
}
