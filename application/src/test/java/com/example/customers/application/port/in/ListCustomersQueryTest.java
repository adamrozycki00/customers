package com.example.customers.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.application.exception.InvalidPageRequestException;
import org.junit.jupiter.api.Test;

class ListCustomersQueryTest {

  @Test
  void shouldRejectZeroPage() {
    int inputZeroPage = 0;

    var thrown = catchThrowable(() -> ListCustomersUseCase.Query.builder()
        .tenantId("flowers")
        .page(inputZeroPage)
        .build());

    assertThat(thrown).isInstanceOf(InvalidPageRequestException.class);
  }

  @Test
  void shouldRejectZeroSize() {
    int inputZeroSize = 0;

    var thrown = catchThrowable(() -> ListCustomersUseCase.Query.builder()
        .tenantId("flowers")
        .size(inputZeroSize)
        .build());

    assertThat(thrown).isInstanceOf(InvalidPageRequestException.class);
  }
}
