package com.example.customers.application.port.in;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.nonNull;

import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.exception.ValidationException;
import java.util.Map;
import java.util.Set;
import lombok.Builder;

public interface UpdateCustomerUseCase {

  Customer update(Command command);

  @Builder
  record Command(
      String tenantId,
      String id,
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
        throw new ValidationException("Only first_name, last_name and generic_fields can be updated");
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
