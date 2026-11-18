package com.example.customers.infrastructure.rest.customer;

import static com.example.customers.application.port.in.UpdateCustomerUseCase.Command.FIRST_NAME_PATH;
import static com.example.customers.application.port.in.UpdateCustomerUseCase.Command.GENERIC_FIELDS_PATH_PREFIX;
import static com.example.customers.application.port.in.UpdateCustomerUseCase.Command.LAST_NAME_PATH;
import static java.util.Objects.nonNull;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.in.GetCustomerUseCase;
import com.example.customers.application.port.in.ListCustomersUseCase;
import com.example.customers.application.port.in.UpdateCustomerUseCase;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import com.example.customers.infrastructure.rest.api.CustomersApi;
import com.example.customers.infrastructure.rest.api.model.CreateCustomerRequest;
import com.example.customers.infrastructure.rest.api.model.CustomerPatchRequest;
import com.example.customers.infrastructure.rest.api.model.CustomerPageResponse;
import com.example.customers.infrastructure.rest.api.model.CustomerResponse;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomersApi {

  private final CreateCustomerUseCase createCustomerUseCase;
  private final GetCustomerUseCase getCustomerUseCase;
  private final ListCustomersUseCase listCustomersUseCase;
  private final UpdateCustomerUseCase updateCustomerUseCase;

  @Override
  public CustomerResponse createCustomer(String xTenantId, CreateCustomerRequest request) {
    var command = CreateCustomerUseCase.Command.builder()
        .tenantId(xTenantId)
        .publicId(request.getPublicId())
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .genericFields(request.getGenericFields())
        .build();
    var createdCustomer = createCustomerUseCase.create(command);
    return toResponse(createdCustomer);
  }

  @Override
  public CustomerResponse getCustomer(String xTenantId, String publicId) {
    var query = new GetCustomerUseCase.Query(xTenantId, publicId);
    var customer = getCustomerUseCase.get(query);
    return toResponse(customer);
  }

  @Override
  public CustomerPageResponse listCustomers(String xTenantId, Integer page, Integer size) {
    var query = ListCustomersUseCase.Query.builder()
        .tenantId(xTenantId)
        .page(page)
        .size(size)
        .build();
    var customerPage = listCustomersUseCase.list(query);
    return toPageResponse(customerPage);
  }

  @Override
  public CustomerResponse updateCustomer(String xTenantId, String publicId, CustomerPatchRequest request) {
    var fieldMask = toFieldMask(request);
    var command = UpdateCustomerUseCase.Command.builder()
        .tenantId(xTenantId)
        .publicId(publicId)
        .fieldMask(fieldMask)
        .firstName(request.getFirstName())
        .lastName(request.getLastName())
        .genericFields(request.getGenericFields())
        .build();
    var updatedCustomer = updateCustomerUseCase.update(command);
    return toResponse(updatedCustomer);
  }

  private static Set<String> toFieldMask(CustomerPatchRequest request) {
    Set<String> fieldMask = new LinkedHashSet<>();
    fieldMask.add(FIRST_NAME_PATH);
    fieldMask.add(LAST_NAME_PATH);
    var genericFields = request.getGenericFields();

    genericFields.keySet().forEach(name -> {
      var path = GENERIC_FIELDS_PATH_PREFIX + name;
      fieldMask.add(path);
    });

    return fieldMask;
  }

  private static CustomerPageResponse toPageResponse(CustomerPage customerPage) {
    var customers = customerPage.customers().stream()
        .map(CustomerController::toResponse)
        .toList();
    return new CustomerPageResponse(customers, customerPage.page(), customerPage.size(), customerPage.totalElements());
  }

  private static CustomerResponse toResponse(Customer customer) {
    var publicId = customer.getPublicId();
    var firstName = customer.getFirstName();
    var lastName = customer.getLastName();
    var rawFirstName = nonNull(firstName) ? firstName.value() : null;
    var rawLastName = nonNull(lastName) ? lastName.value() : null;
    var rawGenericFields = toRawGenericFields(customer.getGenericFields());
    return new CustomerResponse(publicId.value(), rawGenericFields)
        .firstName(rawFirstName)
        .lastName(rawLastName);
  }

  private static Map<String, Object> toRawGenericFields(Map<FieldName, GenericFieldValue> genericFields) {
    Map<String, Object> rawGenericFields = new LinkedHashMap<>();

    genericFields.forEach((name, value) -> {
      var rawValue = toRawValue(value);
      rawGenericFields.put(name.value(), rawValue);
    });

    return rawGenericFields;
  }

  private static Object toRawValue(GenericFieldValue value) {
    return switch (value) {
      case BooleanValue booleanValue -> booleanValue.value();
      case StringValue stringValue -> stringValue.value();
      case IntegerValue integerValue -> integerValue.value();
      case DatetimeValue datetimeValue -> datetimeValue.value().toString();
    };
  }
}
