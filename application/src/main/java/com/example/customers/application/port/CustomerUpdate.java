package com.example.customers.application.port;

import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import lombok.Builder;

import java.util.Map;
import java.util.Set;

@Builder
public record CustomerUpdate(
    FieldUpdate<FirstName> firstName,
    FieldUpdate<LastName> lastName,
    Map<FieldName, GenericFieldValue> genericFieldsToSet,
    Set<FieldName> genericFieldsToClear) {
}
