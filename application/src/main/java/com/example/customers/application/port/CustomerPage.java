package com.example.customers.application.port;

import com.example.customers.domain.customer.Customer;
import lombok.Builder;

import java.util.List;

@Builder
public record CustomerPage(List<Customer> customers, int page, int size, long totalElements) {
}
