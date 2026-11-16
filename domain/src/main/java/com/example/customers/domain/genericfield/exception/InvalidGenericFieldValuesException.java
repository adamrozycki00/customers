package com.example.customers.domain.genericfield.exception;

import com.example.customers.domain.exception.DomainValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class InvalidGenericFieldValuesException extends DomainValidationException {

  private final List<String> invalidFieldNames;
}
