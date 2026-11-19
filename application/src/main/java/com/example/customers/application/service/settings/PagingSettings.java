package com.example.customers.application.service.settings;

/**
 * Paging limits, populated from the application configuration ({@code customers.paging.*}).
 */
public record PagingSettings(int defaultPageSize, int maxPageSize) {
}
