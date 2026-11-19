package com.example.customers.domain.tenant;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.upperCase;

import com.example.customers.domain.exception.ValidationException;
import java.util.Locale;
import java.util.regex.Pattern;

public record TenantId(String value) {

  private static final int MAX_LENGTH = 64;
  private static final Pattern LETTERS_DIGITS_UNDERSCORES_OR_HYPHENS = Pattern.compile("[A-Za-z0-9_-]+");

  public TenantId {
    value = upperCase(value, Locale.ROOT);
    validate(value);
  }

  private static void validate(String value) {
    if (isEmpty(value)
        || value.length() > MAX_LENGTH
        || !LETTERS_DIGITS_UNDERSCORES_OR_HYPHENS.matcher(value).matches()) {
      throw new ValidationException("Tenant id must be 1 to " + MAX_LENGTH + " letters, digits, '_' or '-'");
    }
  }
}
