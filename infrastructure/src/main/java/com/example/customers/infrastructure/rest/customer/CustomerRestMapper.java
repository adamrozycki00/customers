package com.example.customers.infrastructure.rest.customer;

import static com.example.customers.application.port.in.UpdateCustomerUseCase.Command.FIRST_NAME_PATH;
import static com.example.customers.application.port.in.UpdateCustomerUseCase.Command.GENERIC_FIELDS_PATH_PREFIX;
import static com.example.customers.application.port.in.UpdateCustomerUseCase.Command.LAST_NAME_PATH;
import static java.util.Objects.nonNull;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.in.UpdateCustomerUseCase;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import com.example.customers.infrastructure.rest.RestMapperConfig;
import com.example.customers.infrastructure.rest.api.model.CreateCustomerRequest;
import com.example.customers.infrastructure.rest.api.model.CustomerPageResponse;
import com.example.customers.infrastructure.rest.api.model.CustomerPatchRequest;
import com.example.customers.infrastructure.rest.api.model.CustomerResponse;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = RestMapperConfig.class)
public interface CustomerRestMapper {

  @Mapping(target = "tenantId", source = "tenantId")
  CreateCustomerUseCase.Command toCreateCommand(String tenantId, CreateCustomerRequest request);

  @Mapping(target = "tenantId", source = "tenantId")
  @Mapping(target = "publicId", source = "publicId")
  @Mapping(target = "fieldMask", source = "request", qualifiedByName = "toFieldMask")
  UpdateCustomerUseCase.Command toUpdateCommand(String tenantId, String publicId, CustomerPatchRequest request);

  @Mapping(target = "publicId", source = "publicId.value")
  @Mapping(target = "firstName", source = "firstName.value")
  @Mapping(target = "lastName", source = "lastName.value")
  @Mapping(target = "genericFields", source = "genericFields", qualifiedByName = "toRawGenericFields")
  CustomerResponse toResponse(Customer customer);

  CustomerPageResponse toPageResponse(CustomerPage customerPage);

  @Named("toFieldMask")
  default Set<String> toFieldMask(CustomerPatchRequest request) {
    Set<String> fieldMask = new LinkedHashSet<>();

    if (nonNull(request.getFirstName())) {
      fieldMask.add(FIRST_NAME_PATH);
    }

    if (nonNull(request.getLastName())) {
      fieldMask.add(LAST_NAME_PATH);
    }

    var genericFields = request.getGenericFields();

    if (nonNull(genericFields)) {
      genericFields.keySet().forEach(name -> {
        var path = GENERIC_FIELDS_PATH_PREFIX + name;
        fieldMask.add(path);
      });
    }

    return fieldMask;
  }

  @Named("toRawGenericFields")
  default Map<String, Object> toRawGenericFields(Map<FieldName, GenericFieldValue> genericFields) {
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
