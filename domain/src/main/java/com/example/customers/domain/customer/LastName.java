package com.example.customers.domain.customer;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.strip;

import com.example.customers.domain.customer.exception.InvalidLastNameException;

public record LastName(String value) {

  private static final int MAX_LENGTH = 256;

  public LastName {
    value = strip(value);
    validate(value);
  }

  private static void validate(String value) {
    if (isBlank(value) || value.length() > MAX_LENGTH) {
      throw new InvalidLastNameException();
    }
  }
}
