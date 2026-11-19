package com.example.customers.domain.genericfield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValuesException;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.tenant.TenantId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TenantGenericFieldsTest {

  private static final TenantId TENANT_ID = new TenantId("flowers");
  private static final FieldName REGISTERED_FIELD_NAME = new FieldName("vip");
  private static final GenericField REGISTERED_FIELD = GenericField.builder()
      .tenantId(TENANT_ID)
      .name(REGISTERED_FIELD_NAME)
      .type(FieldType.BOOL)
      .build();
  private static final List<GenericField> REGISTERED_FIELDS = List.of(REGISTERED_FIELD);

  private final TenantGenericFields underTest = TenantGenericFields.of(REGISTERED_FIELDS);

  @Test
  void shouldAcceptValueOfRegisteredField() {
    var inputValueOfRegisteredField = Map.of("vip", true);
    var expectedValues = Map.of(new FieldName("vip"), new BooleanValue(true));

    var resultValues = underTest.toTypedValues(inputValueOfRegisteredField);

    assertThat(resultValues).isEqualTo(expectedValues);
  }

  @Test
  void shouldRejectUnknownField() {
    var inputValueOfUnknownField = Map.of("vipp", true);
    var expectedInvalidFieldNames = List.of("vipp");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> underTest.toTypedValues(inputValueOfUnknownField));

    assertThat(thrown)
        .extracting(InvalidGenericFieldValuesException::getInvalidFieldNames)
        .isEqualTo(expectedInvalidFieldNames);
  }

  @Test
  void shouldRejectValueOfWrongType() {
    var inputValueOfWrongType = Map.of("vip", "yes");
    var expectedInvalidFieldNames = List.of("vip");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> underTest.toTypedValues(inputValueOfWrongType));

    assertThat(thrown)
        .extracting(InvalidGenericFieldValuesException::getInvalidFieldNames)
        .isEqualTo(expectedInvalidFieldNames);
  }

  @Test
  void shouldRejectInvalidFieldName() {
    var inputValueOfInvalidFieldName = Map.of("date.of.birth", "1990-05-20T00:00:00Z");
    var expectedInvalidFieldNames = List.of("date.of.birth");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> underTest.toTypedValues(inputValueOfInvalidFieldName));

    assertThat(thrown)
        .extracting(InvalidGenericFieldValuesException::getInvalidFieldNames)
        .isEqualTo(expectedInvalidFieldNames);
  }

  @Test
  void shouldReportAllInvalidFieldNames() {
    var inputValuesOfSeveralInvalidFields = Map.of("vipp", true, "vip", "yes");
    var expectedInvalidFieldNames = List.of("vipp", "vip");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> underTest.toTypedValues(inputValuesOfSeveralInvalidFields));

    assertThat(thrown).isNotNull();
    assertThat(thrown.getInvalidFieldNames()).containsExactlyInAnyOrderElementsOf(expectedInvalidFieldNames);
  }

  @Test
  void shouldReturnRegisteredFieldNames() {
    var inputRegisteredNames = List.of("vip");
    var expectedFieldNames = Set.of(REGISTERED_FIELD_NAME);

    var resultFieldNames = underTest.toRegisteredFieldNames(inputRegisteredNames);

    assertThat(resultFieldNames).isEqualTo(expectedFieldNames);
  }

  @Test
  void shouldRejectUnregisteredFieldNames() {
    var inputUnregisteredNames = List.of("vipp");
    var expectedInvalidFieldNames = List.of("vipp");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> underTest.toRegisteredFieldNames(inputUnregisteredNames));

    assertThat(thrown).isNotNull();
    assertThat(thrown.getInvalidFieldNames()).isEqualTo(expectedInvalidFieldNames);
  }

  @Test
  void shouldRejectInvalidFieldNamesToClear() {
    var inputInvalidNames = List.of("date.of.birth");
    var expectedInvalidFieldNames = List.of("date.of.birth");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> underTest.toRegisteredFieldNames(inputInvalidNames));

    assertThat(thrown).isNotNull();
    assertThat(thrown.getInvalidFieldNames()).isEqualTo(expectedInvalidFieldNames);
  }
}
