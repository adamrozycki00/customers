package com.example.customers.application.service;

import com.example.customers.application.exception.InvalidPageRequestException;
import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.PageRequest;
import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.in.DeleteCustomerUseCase;
import com.example.customers.application.port.in.GetCustomerUseCase;
import com.example.customers.application.port.in.ListCustomersUseCase;
import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.customer.exception.CustomerNotFoundException;
import com.example.customers.domain.genericfield.TenantGenericFields;
import com.example.customers.domain.tenant.TenantId;
import lombok.Builder;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Objects.nonNull;

@Builder
public class CustomerService implements CreateCustomerUseCase, GetCustomerUseCase, ListCustomersUseCase,
    DeleteCustomerUseCase {

  private static final int DEFAULT_PAGE = 1;

  private final CustomerPort customerPort;
  private final GenericFieldPort genericFieldPort;
  private final PagingSettings pagingSettings;

  @Override
  public Customer create(CreateCustomerUseCase.Command command) {
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

  @Override
  public Customer get(GetCustomerUseCase.Query query) {
    var tenantId = new TenantId(query.tenantId());
    var publicId = new PublicId(query.publicId());

    return customerPort.find(tenantId, publicId).orElseThrow(CustomerNotFoundException::new);
  }

  @Override
  public CustomerPage list(ListCustomersUseCase.Query query) {
    var tenantId = new TenantId(query.tenantId());
    int page = nonNull(query.page()) ? query.page() : DEFAULT_PAGE;
    int size = nonNull(query.size()) ? query.size() : pagingSettings.defaultPageSize();
    validatePageSize(size);
    var pageRequest = new PageRequest(page, size);

    return customerPort.getPage(tenantId, pageRequest);
  }

  private void validatePageSize(int size) {
    if (size > pagingSettings.maxPageSize()) {
      throw new InvalidPageRequestException();
    }
  }

  @Override
  public void delete(DeleteCustomerUseCase.Command command) {
    var tenantId = new TenantId(command.tenantId());
    var publicId = new PublicId(command.publicId());

    customerPort.delete(tenantId, publicId);
  }
}
