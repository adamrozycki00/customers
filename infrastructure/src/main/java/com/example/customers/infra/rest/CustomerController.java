package com.example.customers.infra.rest;

import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.in.DeleteCustomerUseCase;
import com.example.customers.application.port.in.GetCustomerUseCase;
import com.example.customers.application.port.in.ListCustomersUseCase;
import com.example.customers.application.port.in.UpdateCustomerUseCase;
import com.example.customers.infra.rest.api.CustomerApi;
import com.example.customers.infra.rest.api.model.CreateCustomerRequest;
import com.example.customers.infra.rest.api.model.CustomerPageResponse;
import com.example.customers.infra.rest.api.model.CustomerPatchRequest;
import com.example.customers.infra.rest.api.model.CustomerResponse;
import com.example.customers.infra.rest.mapper.CustomerRestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomerApi {

  private final CreateCustomerUseCase createCustomerUseCase;
  private final GetCustomerUseCase getCustomerUseCase;
  private final ListCustomersUseCase listCustomersUseCase;
  private final UpdateCustomerUseCase updateCustomerUseCase;
  private final DeleteCustomerUseCase deleteCustomerUseCase;
  private final CustomerRestMapper mapper;

  @Override
  public CustomerResponse createCustomer(String xTenantId, CreateCustomerRequest request) {
    var command = mapper.toCreateCommand(xTenantId, request);
    var createdCustomer = createCustomerUseCase.create(command);
    return mapper.toResponse(createdCustomer);
  }

  @Override
  public CustomerResponse getCustomer(String xTenantId, String id) {
    var query = new GetCustomerUseCase.Query(xTenantId, id);
    var customer = getCustomerUseCase.get(query);
    return mapper.toResponse(customer);
  }

  @Override
  public CustomerPageResponse listCustomers(String xTenantId, Integer page, Integer size) {
    var query = ListCustomersUseCase.Query.builder()
        .tenantId(xTenantId)
        .page(page)
        .size(size)
        .build();
    var customerPage = listCustomersUseCase.list(query);
    return mapper.toPageResponse(customerPage);
  }

  @Override
  public CustomerResponse updateCustomer(String xTenantId, String id, CustomerPatchRequest request) {
    var command = mapper.toUpdateCommand(xTenantId, id, request);
    var updatedCustomer = updateCustomerUseCase.update(command);
    return mapper.toResponse(updatedCustomer);
  }

  @Override
  public void deleteCustomer(String xTenantId, String id) {
    var command = new DeleteCustomerUseCase.Command(xTenantId, id);
    deleteCustomerUseCase.delete(command);
  }
}
