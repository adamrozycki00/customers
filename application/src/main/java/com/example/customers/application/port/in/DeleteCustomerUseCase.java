package com.example.customers.application.port.in;

public interface DeleteCustomerUseCase {

  void delete(Command command);

  record Command(String tenantId, String id) {
  }
}
