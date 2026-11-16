package com.example.customers.domain.genericfield;

import com.example.customers.domain.genericfield.exception.InvalidGenericFieldException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class GenericFieldTest {

  @Test
  void shouldRejectNullName() {
    FieldName inputNullName = null;
    var inputValidType = FieldType.BOOLEAN;

    var thrown = catchThrowable(() -> new GenericField(inputNullName, inputValidType));

    assertThat(thrown).isInstanceOf(InvalidGenericFieldException.class);
  }

  @Test
  void shouldRejectNullType() {
    var inputValidName = new FieldName("vip");
    FieldType inputNullType = null;

    var thrown = catchThrowable(() -> new GenericField(inputValidName, inputNullType));

    assertThat(thrown).isInstanceOf(InvalidGenericFieldException.class);
  }
}
