package com.example.customers.domain.genericfield.exception;

import com.example.customers.domain.exception.DomainValidationException;

public class InvalidFieldTypeException extends DomainValidationException {

  public InvalidFieldTypeException() {
    super("Unknown field type; use one of BOOLEAN, STRING, INTEGER, DATETIME");
  }
}
