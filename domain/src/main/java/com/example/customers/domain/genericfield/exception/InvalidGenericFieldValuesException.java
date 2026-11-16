package com.example.customers.domain.genericfield.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class InvalidGenericFieldValuesException extends RuntimeException {

  private final List<String> invalidFieldNames;

  public InvalidGenericFieldValuesException(String invalidFieldName) {
    this.invalidFieldNames = List.of(invalidFieldName);
  }
}
