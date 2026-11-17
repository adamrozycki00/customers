package com.example.customers.application.port.in;

import com.example.customers.domain.customer.Customer;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

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

    public Command {
      fieldMask = nonNull(fieldMask) ? fieldMask : emptySet();
    }
  }
}
