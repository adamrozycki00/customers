package com.example.customers.domain.exception;

public abstract class ConflictException extends RuntimeException {

  protected ConflictException() {
  }

  protected ConflictException(String message) {
    super(message);
  }
}
