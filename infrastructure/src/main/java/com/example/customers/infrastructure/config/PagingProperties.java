package com.example.customers.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("customers.paging")
public record PagingProperties(int defaultPageSize, int maxPageSize) {
}
