package com.example.customers.application.service;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.example.customers.application.adapter.InMemoryCustomerDbAdapter;
import com.example.customers.application.adapter.InMemoryGenericFieldDbAdapter;
import com.example.customers.application.port.in.UpdateCustomerUseCase;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.exception.NotFoundException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValuesException;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class UpdateCustomerServiceTest {

  private final InMemoryCustomerDbAdapter customerRepo = new InMemoryCustomerDbAdapter();
  private final InMemoryGenericFieldDbAdapter genericFieldRepo = new InMemoryGenericFieldDbAdapter();

  private final UpdateCustomerService underTest = new UpdateCustomerService(customerRepo, genericFieldRepo);

  private final TenantId tenantId = new TenantId("flowers");
  private final PublicId publicId = new PublicId("CUST-1");
  private final FirstName storedFirstName = new FirstName("Anna");
  private final LastName storedLastName = new LastName("Smith");
  private final FieldName vipName = new FieldName("vip");

  @Test
  void shouldSetFirstName() {
    // given that Anna Smith is stored and a command sets the first name to Maria
    storeCustomer();
    Set<String> firstNameMask = Set.of("firstName");
    var inputCommand = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(firstNameMask)
        .firstName("Maria")
        .build();
    var expectedFirstName = new FirstName("Maria");
    var expectedCustomer = Customer.builder()
        .tenantId(tenantId)
        .publicId(publicId)
        .firstName(expectedFirstName)
        .lastName(storedLastName)
        .build();

    var resultCustomer = underTest.update(inputCommand);

    assertThat(resultCustomer).usingRecursiveComparison().isEqualTo(expectedCustomer);
  }

  @Test
  void shouldSetLastName() {
    // given that Anna Smith is stored and a command sets the last name to Nowak
    storeCustomer();
    Set<String> lastNameMask = Set.of("lastName");
    var inputCommand = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(lastNameMask)
        .lastName("Nowak")
        .build();
    var expectedLastName = new LastName("Nowak");
    var expectedCustomer = Customer.builder()
        .tenantId(tenantId)
        .publicId(publicId)
        .firstName(storedFirstName)
        .lastName(expectedLastName)
        .build();

    var resultCustomer = underTest.update(inputCommand);

    assertThat(resultCustomer).usingRecursiveComparison().isEqualTo(expectedCustomer);
  }

  @Test
  void shouldSetGenericField() {
    // given a registered vip field and a command setting vip to true
    registerVipField();
    storeCustomer();
    Set<String> vipMask = Set.of("genericFields.vip");
    Map<String, Object> inputGenericFields = Map.of("vip", true);
    var inputCommand = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(vipMask)
        .genericFields(inputGenericFields)
        .build();
    var expectedVipValue = new BooleanValue(true);
    Map<FieldName, GenericFieldValue> expectedGenericFields = Map.of(vipName, expectedVipValue);
    var expectedCustomer = Customer.builder()
        .tenantId(tenantId)
        .publicId(publicId)
        .firstName(storedFirstName)
        .lastName(storedLastName)
        .genericFields(expectedGenericFields)
        .build();

    var resultCustomer = underTest.update(inputCommand);

    assertThat(resultCustomer).usingRecursiveComparison().isEqualTo(expectedCustomer);
  }

  @Test
  void shouldClearGenericField() {
    // given a customer with vip set and a command clearing it
    registerVipField();
    var storedVipValue = new BooleanValue(true);
    Map<FieldName, GenericFieldValue> storedGenericFields = Map.of(vipName, storedVipValue);
    storeCustomer(storedGenericFields);
    Set<String> vipMask = Set.of("genericFields.vip");
    Map<String, Object> inputGenericFieldsClearingVip = new HashMap<>();
    inputGenericFieldsClearingVip.put("vip", null);
    var inputCommand = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(vipMask)
        .genericFields(inputGenericFieldsClearingVip)
        .build();

    var resultCustomer = underTest.update(inputCommand);

    assertThat(resultCustomer.getGenericFields()).isEmpty();
  }

  @Test
  void shouldRejectSettingUnregisteredGenericField() {
    // given a command setting the unregistered field vipp
    registerVipField();
    storeCustomer();
    Set<String> unregisteredFieldMask = Set.of("genericFields.vipp");
    Map<String, Object> inputValueOfUnregisteredField = Map.of("vipp", true);
    var inputCommand = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(unregisteredFieldMask)
        .genericFields(inputValueOfUnregisteredField)
        .build();
    var expectedInvalidFieldNames = List.of("vipp");

    var thrown = catchThrowableOfType(InvalidGenericFieldValuesException.class, () -> underTest.update(inputCommand));

    assertThat(thrown.getInvalidFieldNames()).isEqualTo(expectedInvalidFieldNames);
  }

  @Test
  void shouldRejectClearingUnregisteredGenericField() {
    // given a command clearing the unregistered field vipp
    registerVipField();
    storeCustomer();
    Set<String> unregisteredFieldMask = Set.of("genericFields.vipp");
    Map<String, Object> inputGenericFieldsClearingUnregisteredField = new HashMap<>();
    inputGenericFieldsClearingUnregisteredField.put("vipp", null);
    var inputCommand = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(unregisteredFieldMask)
        .genericFields(inputGenericFieldsClearingUnregisteredField)
        .build();
    var expectedInvalidFieldNames = List.of("vipp");

    var thrown = catchThrowableOfType(InvalidGenericFieldValuesException.class, () -> underTest.update(inputCommand));

    assertThat(thrown.getInvalidFieldNames()).isEqualTo(expectedInvalidFieldNames);
  }

  @Test
  void shouldReportInvalidFieldNamesFromSetAndClearTogether() {
    // given a command setting vipp and clearing ghost, both unregistered
    registerVipField();
    storeCustomer();
    Set<String> unregisteredFieldsMask = Set.of("genericFields.vipp", "genericFields.ghost");
    Map<String, Object> inputSettingAndClearingUnregisteredFields = new HashMap<>();
    inputSettingAndClearingUnregisteredFields.put("vipp", true);
    inputSettingAndClearingUnregisteredFields.put("ghost", null);
    var inputCommand = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(unregisteredFieldsMask)
        .genericFields(inputSettingAndClearingUnregisteredFields)
        .build();
    var expectedInvalidFieldNames = List.of("vipp", "ghost");

    var thrown = catchThrowableOfType(InvalidGenericFieldValuesException.class, () -> underTest.update(inputCommand));

    assertThat(thrown.getInvalidFieldNames()).containsExactlyInAnyOrderElementsOf(expectedInvalidFieldNames);
  }

  @Test
  void shouldTreatMissingFieldMaskAsEmpty() {
    storeCustomer();
    Set<String> missingFieldMask = null;
    var inputCommand = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .fieldMask(missingFieldMask)
        .firstName("Maria")
        .build();
    var expectedCustomer = Customer.builder()
        .tenantId(tenantId)
        .publicId(publicId)
        .firstName(storedFirstName)
        .lastName(storedLastName)
        .build();

    var resultCustomer = underTest.update(inputCommand);

    assertThat(resultCustomer).usingRecursiveComparison().isEqualTo(expectedCustomer);
  }

  @Test
  void shouldRejectUnknownCustomer() {
    var inputCommandForUnknownCustomer = UpdateCustomerUseCase.Command.builder()
        .tenantId("flowers")
        .publicId("CUST-404")
        .build();

    var thrown = catchThrowable(() -> underTest.update(inputCommandForUnknownCustomer));

    assertThat(thrown).isInstanceOf(NotFoundException.class)
        .hasMessage("Customer not found");
  }

  private void registerVipField() {
    var vipField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOLEAN)
        .build();
    genericFieldRepo.insert(vipField);
  }

  private void storeCustomer() {
    Map<FieldName, GenericFieldValue> noGenericFields = emptyMap();
    storeCustomer(noGenericFields);
  }

  private void storeCustomer(Map<FieldName, GenericFieldValue> genericFields) {
    var storedCustomer = Customer.builder()
        .tenantId(tenantId)
        .publicId(publicId)
        .firstName(storedFirstName)
        .lastName(storedLastName)
        .genericFields(genericFields)
        .build();
    customerRepo.insert(storedCustomer);
  }
}
