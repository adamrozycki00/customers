package com.example.customers.infrastructure.db.exception;

/** A stored document cannot be mapped to the domain. A server-side problem, never the client's fault. */
public class CorruptDataException extends RuntimeException {

  public CorruptDataException(String message, Throwable cause) {
    super(message, cause);
  }
}
