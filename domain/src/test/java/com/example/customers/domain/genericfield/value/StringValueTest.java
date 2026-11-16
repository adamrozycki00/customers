package com.example.customers.domain.genericfield.value;

import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValueException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class StringValueTest {

  @Test
  void shouldRejectNullString() {
    String inputNullString = null;

    var thrown = catchThrowable(() -> new StringValue(inputNullString));

    assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
  }
}
