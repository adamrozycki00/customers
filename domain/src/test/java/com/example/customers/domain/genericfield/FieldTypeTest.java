package com.example.customers.domain.genericfield;

import com.example.customers.domain.genericfield.exception.InvalidFieldTypeException;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValueException;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FieldTypeTest {

  @Nested
  class ValidTypes {

    @Test
    void shouldResolveUpperCaseType() {
      String inputUpperCaseType = "BOOLEAN";

      var resultFieldType = FieldType.of(inputUpperCaseType);

      assertThat(resultFieldType).isEqualTo(FieldType.BOOLEAN);
    }

    @Test
    void shouldResolveLowerCaseType() {
      String inputLowerCaseType = "boolean";

      var resultFieldType = FieldType.of(inputLowerCaseType);

      assertThat(resultFieldType).isEqualTo(FieldType.BOOLEAN);
    }

    @Test
    void shouldResolveStringType() {
      String inputStringType = "STRING";

      var resultFieldType = FieldType.of(inputStringType);

      assertThat(resultFieldType).isEqualTo(FieldType.STRING);
    }

    @Test
    void shouldResolveIntegerType() {
      String inputIntegerType = "INTEGER";

      var resultFieldType = FieldType.of(inputIntegerType);

      assertThat(resultFieldType).isEqualTo(FieldType.INTEGER);
    }

    @Test
    void shouldResolveDatetimeType() {
      String inputDatetimeType = "DATETIME";

      var resultFieldType = FieldType.of(inputDatetimeType);

      assertThat(resultFieldType).isEqualTo(FieldType.DATETIME);
    }
  }

  @Nested
  class InvalidTypes {

    @Test
    void shouldRejectInvalidType() {
      String inputInvalidType = "DATE";

      var thrown = catchThrowable(() -> FieldType.of(inputInvalidType));

      assertThat(thrown).isInstanceOf(InvalidFieldTypeException.class);
    }

    @Test
    void shouldRejectNullType() {
      String inputNullType = null;

      var thrown = catchThrowable(() -> FieldType.of(inputNullType));

      assertThat(thrown).isInstanceOf(InvalidFieldTypeException.class);
    }
  }

  @Nested
  class BooleanParsing {

    @Test
    void shouldParseTrue() {
      var inputTrue = true;
      var expectedValue = new BooleanValue(true);

      var resultValue = FieldType.BOOLEAN.parse(inputTrue);

      assertThat(resultValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldRejectNonBooleanInput() {
      String inputNonBoolean = "yes";

      var thrown = catchThrowable(() -> FieldType.BOOLEAN.parse(inputNonBoolean));

      assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
    }
  }

  @Nested
  class StringParsing {

    @Test
    void shouldParseString() {
      String inputString = "gold";
      var expectedValue = new StringValue("gold");

      var resultValue = FieldType.STRING.parse(inputString);

      assertThat(resultValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldRejectNonStringInput() {
      var inputNonString = 42;

      var thrown = catchThrowable(() -> FieldType.STRING.parse(inputNonString));

      assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
    }

    @Test
    void shouldAcceptMaxLengthString() {
      String inputMaxLengthString = "a".repeat(1024);
      var expectedValue = new StringValue(inputMaxLengthString);

      var resultValue = FieldType.STRING.parse(inputMaxLengthString);

      assertThat(resultValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldRejectTooLongString() {
      String inputTooLongString = "a".repeat(1025);

      var thrown = catchThrowable(() -> FieldType.STRING.parse(inputTooLongString));

      assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
    }
  }

  @Nested
  class IntegerParsing {

    @Test
    void shouldParseInteger() {
      var inputInteger = 42;
      var expectedValue = new IntegerValue(42);

      var resultValue = FieldType.INTEGER.parse(inputInteger);

      assertThat(resultValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldParseLong() {
      var inputLong = 5_000_000_000L;
      var expectedValue = new IntegerValue(5_000_000_000L);

      var resultValue = FieldType.INTEGER.parse(inputLong);

      assertThat(resultValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldRejectFractionalNumber() {
      var inputFractionalNumber = 4.5;

      var thrown = catchThrowable(() -> FieldType.INTEGER.parse(inputFractionalNumber));

      assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
    }

    @Test
    void shouldRejectOutOfRangeNumber() {
      var inputOutOfRangeNumber = new BigInteger("9223372036854775808");

      var thrown = catchThrowable(() -> FieldType.INTEGER.parse(inputOutOfRangeNumber));

      assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
    }

    @Test
    void shouldRejectNumericString() {
      String inputNumericString = "42";

      var thrown = catchThrowable(() -> FieldType.INTEGER.parse(inputNumericString));

      assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
    }
  }

  @Nested
  class DatetimeParsing {

    @Test
    void shouldParseUtcDatetime() {
      String inputUtcDatetime = "2026-11-16T10:00:00Z";
      var expectedValue = new DatetimeValue(Instant.parse("2026-11-16T10:00:00Z"));

      var resultValue = FieldType.DATETIME.parse(inputUtcDatetime);

      assertThat(resultValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldParseDatetimeWithoutOffsetAsUtc() {
      String inputDatetimeWithoutOffset = "2026-11-16T10:00:00";
      var expectedValue = new DatetimeValue(Instant.parse("2026-11-16T10:00:00Z"));

      var resultValue = FieldType.DATETIME.parse(inputDatetimeWithoutOffset);

      assertThat(resultValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldConvertOffsetToUtc() {
      String inputDatetimeWithOffset = "2026-11-16T12:00:00+02:00";
      var expectedValue = new DatetimeValue(Instant.parse("2026-11-16T10:00:00Z"));

      var resultValue = FieldType.DATETIME.parse(inputDatetimeWithOffset);

      assertThat(resultValue).isEqualTo(expectedValue);
    }

    @Test
    void shouldRejectUnparseableDatetime() {
      String inputUnparseableDatetime = "tomorrow";

      var thrown = catchThrowable(() -> FieldType.DATETIME.parse(inputUnparseableDatetime));

      assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
    }

    @Test
    void shouldRejectNonStringDatetime() {
      var inputNonStringDatetime = 1_700_000_000;

      var thrown = catchThrowable(() -> FieldType.DATETIME.parse(inputNonStringDatetime));

      assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
    }
  }
}
