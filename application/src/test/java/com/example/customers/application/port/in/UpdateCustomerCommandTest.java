package com.example.customers.application.port.in;

import com.example.customers.application.exception.InvalidFieldMaskException;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class UpdateCustomerCommandTest {

  @Test
  void shouldRejectPublicIdInFieldMask() {
    Set<String> inputFieldMaskWithPublicId = Set.of("publicId");

    var thrown = catchThrowable(() -> UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(inputFieldMaskWithPublicId)
        .build());

    assertThat(thrown).isInstanceOf(InvalidFieldMaskException.class);
  }

  @Test
  void shouldRejectUnknownPathInFieldMask() {
    Set<String> inputFieldMaskWithUnknownPath = Set.of("email");

    var thrown = catchThrowable(() -> UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(inputFieldMaskWithUnknownPath)
        .build());

    assertThat(thrown).isInstanceOf(InvalidFieldMaskException.class);
  }
}
