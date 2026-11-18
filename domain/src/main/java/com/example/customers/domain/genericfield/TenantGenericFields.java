package com.example.customers.domain.genericfield;

import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.example.customers.domain.genericfield.exception.InvalidFieldNameException;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValueException;
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

@RequiredArgsConstructor(access = PRIVATE)
public final class TenantGenericFields {

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
      } catch (InvalidFieldNameException | InvalidGenericFieldValueException e) {
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
      } catch (InvalidFieldNameException | InvalidGenericFieldValueException e) {
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
    var field = findByName(name).orElseThrow(InvalidGenericFieldValueException::new);
    return field.getName();
  }

  private GenericFieldValue toTypedValue(FieldName name, Object rawValue) {
    var field = findByName(name).orElseThrow(InvalidGenericFieldValueException::new);
    return field.getType().parse(rawValue);
  }

  private Optional<GenericField> findByName(FieldName name) {
    return fields.stream()
        .filter(field -> field.getName().equals(name))
        .findFirst();
  }
}
