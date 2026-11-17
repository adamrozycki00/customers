package com.example.customers.application.port.out;

import com.example.customers.domain.customer.Customer;

public interface CustomerPort {

  Customer insert(Customer customer);
}
