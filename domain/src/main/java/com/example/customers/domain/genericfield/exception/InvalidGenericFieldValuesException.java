package com.example.customers.domain.genericfield.exception;

import com.example.customers.domain.exception.DomainValidationException;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InvalidGenericFieldValuesException extends DomainValidationException {

  private final List<String> invalidFieldNames;
}
