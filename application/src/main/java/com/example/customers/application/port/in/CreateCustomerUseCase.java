package com.example.customers.application.port.in;

import com.example.customers.domain.customer.Customer;
import lombok.Builder;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;

public interface CreateCustomerUseCase {

  Customer create(Command command);

  @Builder
  record Command(String tenantId, String publicId, String firstName, String lastName, Map<String, Object> genericFields) {

    public Command {
      genericFields = nonNull(genericFields) ? genericFields : emptyMap();
    }
  }
}
