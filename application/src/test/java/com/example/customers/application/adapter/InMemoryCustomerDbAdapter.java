package com.example.customers.application.adapter;

import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.tenant.TenantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryCustomerDbAdapter implements CustomerPort {

  private final List<Customer> customers = new ArrayList<>();

  @Override
  public Customer insert(Customer customer) {
    customers.add(customer);
    return customer;
  }

  @Override
  public Optional<Customer> find(TenantId tenantId, PublicId publicId) {
    return customers.stream()
        .filter(customer -> customer.getTenantId().equals(tenantId))
        .filter(customer -> customer.getPublicId().equals(publicId))
        .findFirst();
  }
}
