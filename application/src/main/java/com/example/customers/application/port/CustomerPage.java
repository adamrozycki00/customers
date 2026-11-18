package com.example.customers.application.port;

import com.example.customers.domain.customer.Customer;
import java.util.List;
import lombok.Builder;

@Builder
public record CustomerPage(List<Customer> customers, int page, int size, long totalElements) {
}
