package com.example.customers.domain.genericfield.value;

import static java.util.Objects.isNull;

import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValueException;
import java.time.Instant;

public record DatetimeValue(Instant value) implements GenericFieldValue {

  public DatetimeValue {
    validate(value);
  }

  private static void validate(Instant value) {
    if (isNull(value)) {
      throw new InvalidGenericFieldValueException();
    }
  }
}
