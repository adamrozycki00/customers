package com.example.customers.application.port.out;

import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.tenant.TenantId;

import java.util.Optional;

public interface CustomerPort {

  Customer insert(Customer customer);

  Optional<Customer> find(TenantId tenantId, PublicId publicId);
}
