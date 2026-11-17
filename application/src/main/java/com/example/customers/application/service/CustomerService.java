package com.example.customers.application.service;

import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.genericfield.TenantGenericFields;
import com.example.customers.domain.tenant.TenantId;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;

@RequiredArgsConstructor
public class CustomerService implements CreateCustomerUseCase {

  private final CustomerPort customerPort;
  private final GenericFieldPort genericFieldPort;

  @Override
  public Customer create(Command command) {
    var tenantId = new TenantId(command.tenantId());
    var publicId = new PublicId(command.publicId());
    var firstName = nonNull(command.firstName()) ? new FirstName(command.firstName()) : null;
    var lastName = nonNull(command.lastName()) ? new LastName(command.lastName()) : null;
    Map<String, ?> rawGenericFields = nonNull(command.genericFields()) ? command.genericFields() : emptyMap();

    var registeredFields = genericFieldPort.findAllByTenant(tenantId);
    var tenantGenericFields = TenantGenericFields.of(registeredFields);
    var genericFields = tenantGenericFields.toTypedValues(rawGenericFields);

    var newCustomer = Customer.builder()
        .tenantId(tenantId)
        .publicId(publicId)
        .firstName(firstName)
        .lastName(lastName)
        .genericFields(genericFields)
        .build();

    return customerPort.insert(newCustomer);
  }
}
