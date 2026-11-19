package com.example.customers.domain.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LastNameTest {

  @Nested
  class ValidLastNames {

    @Test
    void shouldTrimSurroundingWhitespace() {
      String inputLastNameWithSurroundingWhitespace = " Smith-Jones ";

      var resultLastName = new LastName(inputLastNameWithSurroundingWhitespace);

      assertThat(resultLastName.value()).isEqualTo("Smith-Jones");
    }

    @Test
    void shouldKeepInnerWhitespace() {
      String inputLastNameWithInnerWhitespace = "van der Berg";

      var resultLastName = new LastName(inputLastNameWithInnerWhitespace);

      assertThat(resultLastName.value()).isEqualTo("van der Berg");
    }

    @Test
    void shouldAcceptMaxLengthLastName() {
      String inputMaxLengthLastName = "a".repeat(256);

      var resultLastName = new LastName(inputMaxLengthLastName);

      assertThat(resultLastName.value()).hasSize(256);
    }

    @Test
    void shouldApplyLengthLimitAfterTrimming() {
      String maxLengthName = "a".repeat(256);
      String inputMaxLengthLastNameWithSurroundingWhitespace = " " + maxLengthName + " ";

      var resultLastName = new LastName(inputMaxLengthLastNameWithSurroundingWhitespace);

      assertThat(resultLastName.value()).hasSize(256);
    }
  }

  @Nested
  class InvalidLastNames {

    @Test
    void shouldRejectNullLastName() {
      String inputNullLastName = null;

      var thrown = catchThrowable(() -> new LastName(inputNullLastName));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectBlankLastName() {
      String inputBlankLastName = "   ";

      var thrown = catchThrowable(() -> new LastName(inputBlankLastName));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectTooLongLastName() {
      String inputTooLongLastName = "a".repeat(257);

      var thrown = catchThrowable(() -> new LastName(inputTooLongLastName));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }
  }
}
