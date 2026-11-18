package com.example.customers.application.port.in;

import static java.util.Objects.nonNull;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.domain.exception.ValidationException;
import lombok.Builder;

public interface ListCustomersUseCase {

  CustomerPage list(Query query);

  @Builder
  record Query(String tenantId, Integer page, Integer size) {

    private static final int FIRST_PAGE = 1;
    private static final int MIN_PAGE_SIZE = 1;

    public Query {
      validate(page, size);
    }

    private static void validate(Integer page, Integer size) {
      if (isBelow(page, FIRST_PAGE) || isBelow(size, MIN_PAGE_SIZE)) {
        throw new ValidationException("Page and size must be at least 1");
      }
    }

    private static boolean isBelow(Integer value, int minimum) {
      return nonNull(value) && value < minimum;
    }
  }
}
