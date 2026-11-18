package com.example.customers.application.port.in;

import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;

import com.example.customers.domain.customer.Customer;
import java.util.Map;
import lombok.Builder;

public interface CreateCustomerUseCase {

  Customer create(Command command);

  @Builder
  record Command(String tenantId, String publicId, String firstName, String lastName,
                 Map<String, Object> genericFields) {

    public Command {
      genericFields = nonNull(genericFields) ? genericFields : emptyMap();
    }
  }
}
