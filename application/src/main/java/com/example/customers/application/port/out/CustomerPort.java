package com.example.customers.application.port.out;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.out.dto.CustomerUpdate;
import com.example.customers.application.port.out.dto.PageRequest;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.CustomerId;
import com.example.customers.domain.tenant.TenantId;

import java.util.Optional;

public interface CustomerPort {

  Customer insert(Customer customer);

  Optional<Customer> find(TenantId tenantId, CustomerId id);

  CustomerPage getPage(TenantId tenantId, PageRequest pageRequest);

  Optional<Customer> update(TenantId tenantId, CustomerId id, CustomerUpdate update);

  void delete(TenantId tenantId, CustomerId id);
}
