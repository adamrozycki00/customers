package com.example.customers.application.service;

import static java.util.Objects.nonNull;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.PageRequest;
import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.in.DeleteCustomerUseCase;
import com.example.customers.application.port.in.GetCustomerUseCase;
import com.example.customers.application.port.in.ListCustomersUseCase;
import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.CustomerId;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.exception.NotFoundException;
import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.genericfield.TenantGenericFields;
import com.example.customers.domain.tenant.TenantId;
import lombok.Builder;

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
    var id = new CustomerId(command.id());
    var firstName = nonNull(command.firstName()) ? new FirstName(command.firstName()) : null;
    var lastName = nonNull(command.lastName()) ? new LastName(command.lastName()) : null;

    var expectedFieldNames = command.genericFields().keySet();
    var registeredFields = genericFieldPort.findAllByTenantIncluding(tenantId, expectedFieldNames);
    var tenantGenericFields = TenantGenericFields.of(registeredFields);
    var genericFields = tenantGenericFields.toTypedValues(command.genericFields());

    var newCustomer = Customer.builder()
        .tenantId(tenantId)
        .id(id)
        .firstName(firstName)
        .lastName(lastName)
        .genericFields(genericFields)
        .build();

    return customerPort.insert(newCustomer);
  }

  @Override
  public Customer get(GetCustomerUseCase.Query query) {
    var tenantId = new TenantId(query.tenantId());
    var id = new CustomerId(query.id());

    return customerPort.find(tenantId, id).orElseThrow(() -> new NotFoundException("Customer not found"));
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

  @Override
  public void delete(DeleteCustomerUseCase.Command command) {
    var tenantId = new TenantId(command.tenantId());
    var id = new CustomerId(command.id());

    customerPort.delete(tenantId, id);
  }

  private void validatePageSize(int size) {
    if (size > pagingSettings.maxPageSize()) {
      throw new ValidationException("Size must be at most " + pagingSettings.maxPageSize());
    }
  }
}
