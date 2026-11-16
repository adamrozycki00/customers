package com.example.customers.domain.genericfield;

import com.example.customers.domain.genericfield.exception.InvalidGenericFieldException;

import static java.util.Objects.isNull;

public record GenericField(FieldName name, FieldType type) {

  public GenericField {
    validate(name, type);
  }

  private static void validate(FieldName name, FieldType type) {
    if (isNull(name) || isNull(type)) {
      throw new InvalidGenericFieldException();
    }
  }
}
