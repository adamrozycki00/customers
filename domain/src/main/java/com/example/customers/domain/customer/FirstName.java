package com.example.customers.domain.customer;

import com.example.customers.domain.customer.exception.InvalidFirstNameException;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.strip;

public record FirstName(String value) {

  private static final int MAX_LENGTH = 256;

  public FirstName {
    value = strip(value);
    validate(value);
  }

  private static void validate(String value) {
    if (isBlank(value) || value.length() > MAX_LENGTH) {
      throw new InvalidFirstNameException();
    }
  }
}
