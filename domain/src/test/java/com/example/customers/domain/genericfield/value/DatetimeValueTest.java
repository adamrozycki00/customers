package com.example.customers.domain.genericfield.value;

import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValueException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class DatetimeValueTest {

  @Test
  void shouldRejectNullInstant() {
    Instant inputNullInstant = null;

    var thrown = catchThrowable(() -> new DatetimeValue(inputNullInstant));

    assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
  }
}
