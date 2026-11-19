package com.example.customers.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UpdateCustomerCommandTest {

  @Test
  void shouldRejectIdInFieldMask() {
    Set<String> inputFieldMaskWithId = Set.of("id");

    var thrown = catchThrowable(() -> UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .id("CUST-1")
        .fieldMask(inputFieldMaskWithId)
        .build());

    assertThat(thrown).isInstanceOf(ValidationException.class)
        .hasMessage("Only first_name, last_name and generic_fields can be updated");
  }

  @Test
  void shouldRejectUnknownPathInFieldMask() {
    Set<String> inputFieldMaskWithUnknownPath = Set.of("email");

    var thrown = catchThrowable(() -> UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .id("CUST-1")
        .fieldMask(inputFieldMaskWithUnknownPath)
        .build());

    assertThat(thrown).isInstanceOf(ValidationException.class)
        .hasMessage("Only first_name, last_name and generic_fields can be updated");
  }
}
