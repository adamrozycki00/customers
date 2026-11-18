package com.example.customers.domain.genericfield;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.example.customers.domain.exception.ValidationException;
import java.util.regex.Pattern;

public record FieldName(String value) {

  private static final int MAX_LENGTH = 64;
  private static final Pattern LETTER_THEN_LETTERS_DIGITS_OR_UNDERSCORES = Pattern.compile("[A-Za-z][A-Za-z0-9_]*");

  public FieldName {
    validate(value);
  }

  private static void validate(String name) {
    if (isEmpty(name)
        || name.length() > MAX_LENGTH
        || !LETTER_THEN_LETTERS_DIGITS_OR_UNDERSCORES.matcher(name).matches()) {
      throw new ValidationException("Field name must start with a letter, contain only letters, digits or '_', and be at most "
          + MAX_LENGTH + " characters");
    }
  }
}
