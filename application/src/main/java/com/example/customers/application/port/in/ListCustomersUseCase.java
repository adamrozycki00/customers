package com.example.customers.application.port.in;

import com.example.customers.application.port.CustomerPage;
import lombok.Builder;

public interface ListCustomersUseCase {

  CustomerPage list(Query query);

  @Builder
  record Query(String tenantId, Integer page, Integer size) {
  }
}
