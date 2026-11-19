package com.example.customers.domain.genericfield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ValidationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldNameTest {

  @Nested
  class AcceptsValidName {

    @Test
    void shouldKeepValidNameVerbatim() {
      String inputValidName = "vip";

      var resultFieldName = new FieldName(inputValidName);

      assertThat(resultFieldName.value()).isEqualTo("vip");
    }
  }

  @Nested
  class RejectsMissingValue {

    @Test
    void shouldRejectNullName() {
      String inputNullName = null;

      var thrown = catchThrowable(() -> new FieldName(inputNullName));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectEmptyName() {
      String inputEmptyName = "";

      var thrown = catchThrowable(() -> new FieldName(inputEmptyName));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }
  }

  @Nested
  class RejectsInvalidCharacters {

    @Test
    void shouldRejectNameStartingWithDigit() {
      String inputNameStartingWithDigit = "1_score";

      var thrown = catchThrowable(() -> new FieldName(inputNameStartingWithDigit));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectNameWithDots() {
      String inputNameWithDots = "date.of.birth";

      var thrown = catchThrowable(() -> new FieldName(inputNameWithDots));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectNameWithNonAsciiLetter() {
      String inputNameWithNonAsciiLetter = "naïve";

      var thrown = catchThrowable(() -> new FieldName(inputNameWithNonAsciiLetter));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }
  }

  @Nested
  class EnforcesLength {

    @Test
    void shouldAcceptMaxLengthName() {
      String inputMaxLengthName = "a".repeat(64);

      var resultFieldName = new FieldName(inputMaxLengthName);

      assertThat(resultFieldName.value()).hasSize(64);
    }

    @Test
    void shouldRejectTooLongName() {
      String inputTooLongName = "a".repeat(65);

      var thrown = catchThrowable(() -> new FieldName(inputTooLongName));

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }
  }

  @Nested
  class Equality {

    @Test
    void shouldTreatLowerCaseAndUpperCaseNamesAsDifferent() {
      String inputLowerCaseName = "vip";
      String inputUpperCaseName = "VIP";

      var resultLowerCaseFieldName = new FieldName(inputLowerCaseName);
      var resultUpperCaseFieldName = new FieldName(inputUpperCaseName);

      assertThat(resultLowerCaseFieldName).isNotEqualTo(resultUpperCaseFieldName);
    }
  }
}
