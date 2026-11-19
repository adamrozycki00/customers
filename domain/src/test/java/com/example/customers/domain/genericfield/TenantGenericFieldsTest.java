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

  private final TenantId tenantId = new TenantId("flowers");
  private final FieldName registeredFieldName = new FieldName("vip");
  private final GenericField registeredField = GenericField.builder()
      .tenantId(tenantId)
      .name(registeredFieldName)
      .type(FieldType.BOOL)
      .build();
  private final List<GenericField> registeredFields = List.of(registeredField);
  private final TenantGenericFields tenantGenericFields = TenantGenericFields.of(registeredFields);

  @Test
  void shouldAcceptValueOfRegisteredField() {
    var inputValueOfRegisteredField = Map.of("vip", true);
    var expectedValues = Map.of(new FieldName("vip"), new BooleanValue(true));

    var resultValues = tenantGenericFields.toTypedValues(inputValueOfRegisteredField);

    assertThat(resultValues).isEqualTo(expectedValues);
  }

  @Test
  void shouldRejectUnknownField() {
    var inputValueOfUnknownField = Map.of("vipp", true);
    var expectedInvalidFieldNames = List.of("vipp");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> tenantGenericFields.toTypedValues(inputValueOfUnknownField));

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
        () -> tenantGenericFields.toTypedValues(inputValueOfWrongType));

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
        () -> tenantGenericFields.toTypedValues(inputValueOfInvalidFieldName));

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
        () -> tenantGenericFields.toTypedValues(inputValuesOfSeveralInvalidFields));

    assertThat(thrown).isNotNull();
    assertThat(thrown.getInvalidFieldNames()).containsExactlyInAnyOrderElementsOf(expectedInvalidFieldNames);
  }

  @Test
  void shouldReturnRegisteredFieldNames() {
    var inputRegisteredNames = List.of("vip");
    var expectedFieldNames = Set.of(registeredFieldName);

    var resultFieldNames = tenantGenericFields.toRegisteredFieldNames(inputRegisteredNames);

    assertThat(resultFieldNames).isEqualTo(expectedFieldNames);
  }

  @Test
  void shouldRejectUnregisteredFieldNames() {
    var inputUnregisteredNames = List.of("vipp");
    var expectedInvalidFieldNames = List.of("vipp");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> tenantGenericFields.toRegisteredFieldNames(inputUnregisteredNames));

    assertThat(thrown).isNotNull();
    assertThat(thrown.getInvalidFieldNames()).isEqualTo(expectedInvalidFieldNames);
  }

  @Test
  void shouldRejectInvalidFieldNamesToClear() {
    var inputInvalidNames = List.of("date.of.birth");
    var expectedInvalidFieldNames = List.of("date.of.birth");

    var thrown = catchThrowableOfType(
        InvalidGenericFieldValuesException.class,
        () -> tenantGenericFields.toRegisteredFieldNames(inputInvalidNames));

    assertThat(thrown).isNotNull();
    assertThat(thrown.getInvalidFieldNames()).isEqualTo(expectedInvalidFieldNames);
  }
}
