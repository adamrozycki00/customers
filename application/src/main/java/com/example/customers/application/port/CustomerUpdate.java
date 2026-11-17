package com.example.customers.application.port;

import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import lombok.Builder;

@Builder
public record CustomerUpdate(FieldChange<FirstName> firstName, FieldChange<LastName> lastName) {
}
