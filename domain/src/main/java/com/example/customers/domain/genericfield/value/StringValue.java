package com.example.customers.domain.genericfield.value;

import static java.util.Objects.isNull;

import com.example.customers.domain.exception.ValidationException;

public record StringValue(String value) implements GenericFieldValue {

  private static final int MAX_LENGTH = 1024;

  public StringValue {
    validate(value);
  }

  private static void validate(String value) {
    if (isNull(value) || value.length() > MAX_LENGTH) {
      throw new ValidationException("String value is required and must be at most " + MAX_LENGTH + " characters");
    }
  }
}
