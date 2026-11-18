package com.example.customers.domain.genericfield;

import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;
import static java.util.Arrays.stream;
import static java.util.Objects.isNull;

import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public enum FieldType {
  BOOLEAN,
  STRING,
  INTEGER,
  DATETIME;

  private static final String UNKNOWN_FIELD_TYPE = "Unknown field type; use one of BOOLEAN, STRING, INTEGER, DATETIME";
  private static final DateTimeFormatter ISO_DATETIME_DEFAULTING_TO_UTC = ISO_DATE_TIME.withZone(UTC);

  public static FieldType of(String rawType) {
    validate(rawType);

    return findByName(rawType);
  }

  public GenericFieldValue parse(Object rawValue) {
    return switch (this) {
      case BOOLEAN -> parseBoolean(rawValue);
      case STRING -> parseString(rawValue);
      case INTEGER -> parseInteger(rawValue);
      case DATETIME -> parseDatetime(rawValue);
    };
  }

  private static BooleanValue parseBoolean(Object rawValue) {
    if (rawValue instanceof Boolean booleanValue) {
      return new BooleanValue(booleanValue);
    }

    throw new ValidationException("Value must be a boolean");
  }

  private static StringValue parseString(Object rawValue) {
    if (rawValue instanceof String stringValue) {
      return new StringValue(stringValue);
    }

    throw new ValidationException("Value must be a string");
  }

  private static IntegerValue parseInteger(Object rawValue) {
    if (rawValue instanceof Integer integerValue) {
      return new IntegerValue(integerValue);
    }

    if (rawValue instanceof Long longValue) {
      return new IntegerValue(longValue);
    }

    throw new ValidationException("Value must be a whole number within 64 bits");
  }

  private static DatetimeValue parseDatetime(Object rawValue) {
    if (rawValue instanceof String datetimeText) {
      var instant = toInstant(datetimeText);
      return new DatetimeValue(instant);
    }

    throw new ValidationException("Value must be an ISO-8601 datetime");
  }

  private static Instant toInstant(String datetimeText) {
    try {
      var parsedDatetime = ISO_DATETIME_DEFAULTING_TO_UTC.parse(datetimeText);
      return Instant.from(parsedDatetime);
    } catch (DateTimeException e) {
      throw new ValidationException("Value must be an ISO-8601 datetime");
    }
  }

  private static void validate(String rawType) {
    if (isNull(rawType)) {
      throw new ValidationException(UNKNOWN_FIELD_TYPE);
    }
  }

  private static FieldType findByName(String rawType) {
    String upperCaseType = rawType.toUpperCase(Locale.ROOT);
    var allTypes = values();

    return stream(allTypes)
        .filter(type -> type.name().equals(upperCaseType))
        .findFirst()
        .orElseThrow(() -> new ValidationException(UNKNOWN_FIELD_TYPE));
  }
}
