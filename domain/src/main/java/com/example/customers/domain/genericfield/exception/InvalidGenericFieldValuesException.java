package com.example.customers.domain.genericfield.exception;

import com.example.customers.domain.exception.DomainValidationException;
import java.util.List;
import lombok.Getter;

@Getter
public class InvalidGenericFieldValuesException extends DomainValidationException {

  private final List<String> invalidFieldNames;

  public InvalidGenericFieldValuesException(List<String> invalidFieldNames) {
    super("Unregistered generic fields or values of the wrong type");
    this.invalidFieldNames = invalidFieldNames;
  }
}
