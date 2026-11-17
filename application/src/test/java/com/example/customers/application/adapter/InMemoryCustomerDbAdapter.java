package com.example.customers.application.adapter;

import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.domain.customer.Customer;

import java.util.ArrayList;
import java.util.List;

public class InMemoryCustomerDbAdapter implements CustomerPort {

  private final List<Customer> customers = new ArrayList<>();

  @Override
  public Customer insert(Customer customer) {
    customers.add(customer);
    return customer;
  }
}
