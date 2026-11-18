package com.example.customers.infrastructure.rest.customer;

import static java.util.Objects.nonNull;

import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import com.example.customers.infrastructure.rest.api.CustomersApi;
import com.example.customers.infrastructure.rest.api.model.CreateCustomerRequest;
import com.example.customers.infrastructure.rest.api.model.CustomerResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CustomerController implements CustomersApi {

  private final CreateCustomerUseCase createCustomerUseCase;

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
