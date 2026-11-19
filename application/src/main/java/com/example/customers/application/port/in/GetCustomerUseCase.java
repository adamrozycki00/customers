package com.example.customers.application.port.in;

import com.example.customers.domain.customer.Customer;

public interface GetCustomerUseCase {

  Customer get(Query query);

  record Query(String tenantId, String id) {
  }
}
