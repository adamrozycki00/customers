package com.example.customers.application.port.in;

import com.example.customers.domain.customer.Customer;
import lombok.Builder;

import java.util.Map;

public interface CreateCustomerUseCase {

  Customer create(Command command);

  @Builder
  record Command(String tenantId, String publicId, String firstName, String lastName, Map<String, Object> genericFields) {
  }
}
