package com.example.customers.application.port;

import com.example.customers.application.exception.InvalidPageRequestException;

public record PageRequest(int page, int size) {

  private static final int FIRST_PAGE = 1;
  private static final int MIN_PAGE_SIZE = 1;

  public PageRequest {
    validate(page, size);
  }

  private static void validate(int page, int size) {
    if (page < FIRST_PAGE || size < MIN_PAGE_SIZE) {
      throw new InvalidPageRequestException();
    }
  }
}
