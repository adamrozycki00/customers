package com.example.customers.application.port.in;

import com.example.customers.domain.genericfield.GenericField;
import lombok.Builder;

public interface RegisterGenericFieldUseCase {

  GenericField register(Command command);

  @Builder
  record Command(String tenantId, String name, String type) {
  }
}
