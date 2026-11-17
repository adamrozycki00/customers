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

    private static final String PUBLIC_ID_PATH = "publicId";

    public Command {
      fieldMask = nonNull(fieldMask) ? fieldMask : emptySet();
      genericFields = nonNull(genericFields) ? genericFields : emptyMap();
      validate(fieldMask);
    }

    private static void validate(Set<String> fieldMask) {
      if (fieldMask.contains(PUBLIC_ID_PATH)) {
        throw new InvalidFieldMaskException();
      }
    }
  }
}
