package com.example.customers.domain.genericfield.value;

import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValueException;

import static java.util.Objects.isNull;

public record StringValue(String value) implements GenericFieldValue {

  private static final int MAX_LENGTH = 1024;

  public StringValue {
    validate(value);
  }

  private static void validate(String value) {
    if (isNull(value) || value.length() > MAX_LENGTH) {
      throw new InvalidGenericFieldValueException();
    }
  }
}
