package com.example.customers.application.service;

import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.FieldChange;
import com.example.customers.application.port.in.UpdateCustomerUseCase;
import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.TenantGenericFields;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class UpdateCustomerService implements UpdateCustomerUseCase {

  private static final String FIRST_NAME_PATH = "firstName";
  private static final String LAST_NAME_PATH = "lastName";
  private static final String GENERIC_FIELDS_PATH_PREFIX = "genericFields.";

  private final CustomerPort customerPort;
  private final GenericFieldPort genericFieldPort;

  @Override
  public Customer update(UpdateCustomerUseCase.Command command) {
    var tenantId = new TenantId(command.tenantId());
    var publicId = new PublicId(command.publicId());
    var registeredFields = genericFieldPort.findAllByTenant(tenantId);
    var tenantGenericFields = TenantGenericFields.of(registeredFields);
    var genericFieldsToSet = buildGenericFieldsToSet(tenantGenericFields, command);
    var genericFieldsToClear = buildGenericFieldsToClear(tenantGenericFields, command);
    var customerUpdate = buildCustomerUpdate(command, genericFieldsToSet, genericFieldsToClear);

    return customerPort.update(tenantId, publicId, customerUpdate).orElseThrow();
  }

  private static Map<FieldName, GenericFieldValue> buildGenericFieldsToSet(TenantGenericFields tenantGenericFields,
                                                                           UpdateCustomerUseCase.Command command) {
    var genericFieldNamesToUpdate = getGenericFieldNames(command.fieldMask());
    Map<String, Object> rawGenericFieldsToUpdate = nonNull(command.genericFields()) ? command.genericFields() : emptyMap();
    var rawGenericFieldsToSet = genericFieldNamesToUpdate.stream()
        .filter(name -> nonNull(rawGenericFieldsToUpdate.get(name)))
        .collect(toMap(identity(), rawGenericFieldsToUpdate::get));

    return tenantGenericFields.toTypedValues(rawGenericFieldsToSet);
  }

  private static Set<FieldName> buildGenericFieldsToClear(TenantGenericFields tenantGenericFields,
                                                          UpdateCustomerUseCase.Command command) {
    var genericFieldNamesToUpdate = getGenericFieldNames(command.fieldMask());
    Map<String, Object> rawGenericFieldsToUpdate = nonNull(command.genericFields()) ? command.genericFields() : emptyMap();
    var rawGenericFieldsToClear = genericFieldNamesToUpdate.stream()
        .filter(name -> isNull(rawGenericFieldsToUpdate.get(name)))
        .toList();

    return tenantGenericFields.toRegisteredFieldNames(rawGenericFieldsToClear);
  }

  private static List<String> getGenericFieldNames(Set<String> fieldMask) {
    return fieldMask.stream()
        .filter(path -> path.startsWith(GENERIC_FIELDS_PATH_PREFIX))
        .map(path -> path.substring(GENERIC_FIELDS_PATH_PREFIX.length()))
        .toList();
  }

  private static CustomerUpdate buildCustomerUpdate(
      UpdateCustomerUseCase.Command command,
      Map<FieldName, GenericFieldValue> genericFieldsToSet,
      Set<FieldName> genericFieldsToClear) {
    var fieldMask = command.fieldMask();
    var firstNameChange = fieldMask.contains(FIRST_NAME_PATH)
        ? toFieldChange(command.firstName(), FirstName::new)
        : new FieldChange.Keep<FirstName>();
    var lastNameChange = fieldMask.contains(LAST_NAME_PATH)
        ? toFieldChange(command.lastName(), LastName::new)
        : new FieldChange.Keep<LastName>();
    return CustomerUpdate.builder()
        .firstName(firstNameChange)
        .lastName(lastNameChange)
        .genericFieldsToSet(genericFieldsToSet)
        .genericFieldsToClear(genericFieldsToClear)
        .build();
  }

  private static <T> FieldChange<T> toFieldChange(String rawValue, Function<String, T> toField) {
    if (nonNull(rawValue)) {
      var field = toField.apply(rawValue);
      return new FieldChange.Replace<>(field);
    }

    return new FieldChange.Clear<>();
  }
}
