package com.example.customers.domain.tenant;

import com.example.customers.domain.tenant.exception.InvalidTenantIdException;

import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isEmpty;

public record TenantId(String value) {

  private static final int MAX_LENGTH = 64;
  private static final Pattern LETTERS_DIGITS_UNDERSCORES_OR_HYPHENS = Pattern.compile("[A-Za-z0-9_-]+");

  public TenantId {
    validate(value);
  }

  private static void validate(String value) {
    if (isEmpty(value)
        || value.length() > MAX_LENGTH
        || !LETTERS_DIGITS_UNDERSCORES_OR_HYPHENS.matcher(value).matches()) {
      throw new InvalidTenantIdException();
    }
  }
}
