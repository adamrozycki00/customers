package com.example.customers.domain.genericfield;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValuesException;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * A tenant's registered generic fields; converts incoming values to their types, rejecting unregistered names.
 */
@RequiredArgsConstructor(access = PRIVATE)
public final class TenantGenericFields {

  private static final String NOT_REGISTERED = "Generic field is not registered";

  private final List<GenericField> fields;

  public static TenantGenericFields of(List<GenericField> fields) {
    return new TenantGenericFields(fields);
  }

  public Map<FieldName, GenericFieldValue> toTypedValues(Map<String, ?> rawFields) {
    Map<FieldName, GenericFieldValue> typedValues = new LinkedHashMap<>();
    List<String> invalidFieldNames = new ArrayList<>();

    for (var rawNameAndValue : rawFields.entrySet()) {
      String rawName = rawNameAndValue.getKey();
      var rawValue = rawNameAndValue.getValue();

      try {
        var name = new FieldName(rawName);
        var typedValue = toTypedValue(name, rawValue);
        typedValues.put(name, typedValue);
      } catch (ValidationException e) {
        invalidFieldNames.add(rawName);
      }
    }

    if (isNotEmpty(invalidFieldNames)) {
      throw new InvalidGenericFieldValuesException(invalidFieldNames);
    }

    return typedValues;
  }

  public Set<FieldName> toRegisteredFieldNames(Collection<String> rawNames) {
    Set<FieldName> registeredNames = new LinkedHashSet<>();
    List<String> invalidFieldNames = new ArrayList<>();

    for (String rawName : rawNames) {
      try {
        var name = toRegisteredFieldName(rawName);
        registeredNames.add(name);
      } catch (ValidationException e) {
        invalidFieldNames.add(rawName);
      }
    }

    if (isNotEmpty(invalidFieldNames)) {
      throw new InvalidGenericFieldValuesException(invalidFieldNames);
    }

    return registeredNames;
  }

  private FieldName toRegisteredFieldName(String rawName) {
    var name = new FieldName(rawName);
    var field = findByName(name).orElseThrow(() -> new ValidationException(NOT_REGISTERED));
    return field.getName();
  }

  private GenericFieldValue toTypedValue(FieldName name, Object rawValue) {
    var field = findByName(name).orElseThrow(() -> new ValidationException(NOT_REGISTERED));
    return field.getType().parse(rawValue);
  }

  private Optional<GenericField> findByName(FieldName name) {
    return fields.stream()
        .filter(field -> field.getName().equals(name))
        .findFirst();
  }
}
