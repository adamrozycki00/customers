package com.example.customers.application.service;

import com.example.customers.application.adapter.InMemoryCustomerDbAdapter;
import com.example.customers.application.adapter.InMemoryGenericFieldDbAdapter;
import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.in.CreateCustomerUseCase.Command;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerServiceTest {

  private final InMemoryCustomerDbAdapter customerRepo = new InMemoryCustomerDbAdapter();
  private final InMemoryGenericFieldDbAdapter genericFieldRepo = new InMemoryGenericFieldDbAdapter();

  private final CreateCustomerUseCase underTest = new CustomerService(customerRepo, genericFieldRepo);

  @Test
  void shouldReturnCreatedCustomer() {
    var tenantId = new TenantId("flowers");
    var vipName = new FieldName("vip");
    var vipField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOLEAN)
        .build();
    genericFieldRepo.insert(vipField);
    Map<String, Object> inputGenericFields = Map.of("vip", true);
    var inputCommand = Command.builder()
        .tenantId("flowers")
        .publicId("CUST-1")
        .firstName("Anna")
        .lastName("Smith")
        .genericFields(inputGenericFields)
        .build();
    var expectedPublicId = new PublicId("CUST-1");
    var expectedFirstName = new FirstName("Anna");
    var expectedLastName = new LastName("Smith");
    var expectedVipValue = new BooleanValue(true);
    Map<FieldName, GenericFieldValue> expectedGenericFields = Map.of(vipName, expectedVipValue);
    var expectedCustomer = Customer.builder()
        .tenantId(tenantId)
        .publicId(expectedPublicId)
        .firstName(expectedFirstName)
        .lastName(expectedLastName)
        .genericFields(expectedGenericFields)
        .build();

    var resultCustomer = underTest.create(inputCommand);

    assertThat(resultCustomer).usingRecursiveComparison().isEqualTo(expectedCustomer);
  }
}
