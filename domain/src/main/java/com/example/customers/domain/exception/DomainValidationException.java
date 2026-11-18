package com.example.customers.domain.exception;

public abstract class DomainValidationException extends RuntimeException {

  protected DomainValidationException() {
  }

  protected DomainValidationException(String message) {
    super(message);
  }
}
