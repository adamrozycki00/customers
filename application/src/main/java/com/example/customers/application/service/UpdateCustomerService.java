package com.example.customers.application.service;

import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.FieldUpdate;
import com.example.customers.application.port.in.UpdateCustomerUseCase;
import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.TenantGenericFields;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValuesException;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

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
    var rawGenericFieldsToUpdate = getRawGenericFieldsToUpdate(command);
    var genericFieldsToUpdate = buildGenericFieldsToUpdate(tenantGenericFields, rawGenericFieldsToUpdate);
    var customerUpdate = buildCustomerUpdate(command, genericFieldsToUpdate);

    return customerPort.update(tenantId, publicId, customerUpdate).orElseThrow();
  }

  private static GenericFieldsToUpdate buildGenericFieldsToUpdate(TenantGenericFields tenantGenericFields,
                                                                  Map<String, Object> rawGenericFieldsToUpdate) {
    Map<FieldName, GenericFieldValue> genericFieldsToSet = emptyMap();
    Set<FieldName> genericFieldsToClear = emptySet();
    List<String> invalidFieldNames = new ArrayList<>();

    try {
      genericFieldsToSet = buildGenericFieldsToSet(tenantGenericFields, rawGenericFieldsToUpdate);
    } catch (InvalidGenericFieldValuesException e) {
      invalidFieldNames.addAll(e.getInvalidFieldNames());
    }

    try {
      genericFieldsToClear = buildGenericFieldsToClear(tenantGenericFields, rawGenericFieldsToUpdate);
    } catch (InvalidGenericFieldValuesException e) {
      invalidFieldNames.addAll(e.getInvalidFieldNames());
    }

    if (isNotEmpty(invalidFieldNames)) {
      throw new InvalidGenericFieldValuesException(invalidFieldNames);
    }

    return new GenericFieldsToUpdate(genericFieldsToSet, genericFieldsToClear);
  }

  private static Map<String, Object> getRawGenericFieldsToUpdate(UpdateCustomerUseCase.Command command) {
    var rawGenericFields = command.genericFields();
    var genericFieldNamesToUpdate = getGenericFieldNamesToUpdate(command.fieldMask());
    return genericFieldNamesToUpdate.stream()
        .collect(LinkedHashMap::new,
            (updates, fieldName) -> {
              var rawValue = rawGenericFields.get(fieldName);
              updates.put(fieldName, rawValue);
            }, Map::putAll);
  }

  private static Map<FieldName, GenericFieldValue> buildGenericFieldsToSet(TenantGenericFields tenantGenericFields,
                                                                           Map<String, Object> rawGenericFieldsToUpdate) {
    var rawGenericFieldsToSet = rawGenericFieldsToUpdate.entrySet().stream()
        .filter(fieldToUpdate -> nonNull(fieldToUpdate.getValue()))
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

    return tenantGenericFields.toTypedValues(rawGenericFieldsToSet);
  }

  private static Set<FieldName> buildGenericFieldsToClear(TenantGenericFields tenantGenericFields,
                                                          Map<String, Object> rawGenericFieldsToUpdate) {
    var rawGenericFieldsToClear = rawGenericFieldsToUpdate.entrySet().stream()
        .filter(fieldToUpdate -> isNull(fieldToUpdate.getValue()))
        .map(Map.Entry::getKey)
        .toList();

    return tenantGenericFields.toRegisteredFieldNames(rawGenericFieldsToClear);
  }

  private static List<String> getGenericFieldNamesToUpdate(Set<String> fieldMask) {
    return fieldMask.stream()
        .filter(path -> path.startsWith(GENERIC_FIELDS_PATH_PREFIX))
        .map(path -> path.substring(GENERIC_FIELDS_PATH_PREFIX.length()))
        .toList();
  }

  private static CustomerUpdate buildCustomerUpdate(UpdateCustomerUseCase.Command command,
                                                    UpdateCustomerService.GenericFieldsToUpdate genericFieldsToUpdate) {
    var fieldMask = command.fieldMask();
    var firstNameChange = fieldMask.contains(FIRST_NAME_PATH)
        ? toFieldChange(command.firstName(), FirstName::new)
        : new FieldUpdate.Keep<FirstName>();
    var lastNameChange = fieldMask.contains(LAST_NAME_PATH)
        ? toFieldChange(command.lastName(), LastName::new)
        : new FieldUpdate.Keep<LastName>();
    return CustomerUpdate.builder()
        .firstName(firstNameChange)
        .lastName(lastNameChange)
        .genericFieldsToSet(genericFieldsToUpdate.toSet())
        .genericFieldsToClear(genericFieldsToUpdate.toClear())
        .build();
  }

  private static <T> FieldUpdate<T> toFieldChange(String rawValue, Function<String, T> toField) {
    if (nonNull(rawValue)) {
      var field = toField.apply(rawValue);
      return new FieldUpdate.Replace<>(field);
    }

    return new FieldUpdate.Clear<>();
  }

  private record GenericFieldsToUpdate(Map<FieldName, GenericFieldValue> toSet, Set<FieldName> toClear) {
  }
}
