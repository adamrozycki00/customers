package com.example.customers.application.port.in;

import com.example.customers.application.exception.InvalidFieldMaskException;
import com.example.customers.domain.customer.Customer;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;

public interface UpdateCustomerUseCase {

  Customer update(Command command);

  @Builder
  record Command(
      String tenantId,
      String publicId,
      Set<String> fieldMask,
      String firstName,
      String lastName,
      Map<String, Object> genericFields) {

    public static final String FIRST_NAME_PATH = "firstName";
    public static final String LAST_NAME_PATH = "lastName";
    public static final String GENERIC_FIELDS_PATH_PREFIX = "genericFields.";

    public Command {
      fieldMask = nonNull(fieldMask) ? fieldMask : emptySet();
      genericFields = nonNull(genericFields) ? genericFields : emptyMap();
      validate(fieldMask);
    }

    private static void validate(Set<String> fieldMask) {
      if (fieldMask.stream().anyMatch(Command::isUnknownPath)) {
        throw new InvalidFieldMaskException();
      }
    }

    private static boolean isUnknownPath(String path) {
      var isKnownPath = path.equals(FIRST_NAME_PATH)
          || path.equals(LAST_NAME_PATH)
          || path.startsWith(GENERIC_FIELDS_PATH_PREFIX);
      return !isKnownPath;
    }
  }
}
