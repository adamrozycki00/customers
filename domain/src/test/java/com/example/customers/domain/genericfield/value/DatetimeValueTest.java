package com.example.customers.domain.genericfield.value;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValueException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class DatetimeValueTest {

  @Test
  void shouldRejectNullInstant() {
    Instant inputNullInstant = null;

    var thrown = catchThrowable(() -> new DatetimeValue(inputNullInstant));

    assertThat(thrown).isInstanceOf(InvalidGenericFieldValueException.class);
  }
}
