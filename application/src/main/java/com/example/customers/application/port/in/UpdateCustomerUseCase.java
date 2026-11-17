package com.example.customers.application.port.in;

import com.example.customers.domain.customer.Customer;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

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
  }
}
