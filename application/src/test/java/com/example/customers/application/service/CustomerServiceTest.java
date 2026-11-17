package com.example.customers.application.service;

import com.example.customers.application.adapter.InMemoryCustomerDbAdapter;
import com.example.customers.application.adapter.InMemoryGenericFieldDbAdapter;
import com.example.customers.application.exception.InvalidPageRequestException;
import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.in.DeleteCustomerUseCase;
import com.example.customers.application.port.in.GetCustomerUseCase;
import com.example.customers.application.port.in.ListCustomersUseCase;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.customer.exception.CustomerNotFoundException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CustomerServiceTest {

  private final InMemoryCustomerDbAdapter customerRepo = new InMemoryCustomerDbAdapter();
  private final InMemoryGenericFieldDbAdapter genericFieldRepo = new InMemoryGenericFieldDbAdapter();

  private final PagingSettings pagingSettings = new PagingSettings(7, 15);

  private final CustomerService underTest = CustomerService.builder()
      .customerPort(customerRepo)
      .genericFieldPort(genericFieldRepo)
      .pagingSettings(pagingSettings)
      .build();

  @Nested
  class CreateCustomer {

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
      var inputCommand = CreateCustomerUseCase.Command.builder()
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

    @Test
    void shouldStoreCreatedCustomer() {
      Map<String, Object> noGenericFields = emptyMap();
      var inputCommand = CreateCustomerUseCase.Command.builder()
          .tenantId("flowers")
          .publicId("CUST-1")
          .firstName("Anna")
          .lastName("Smith")
          .genericFields(noGenericFields)
          .build();
      var tenantId = new TenantId("flowers");
      var publicId = new PublicId("CUST-1");

      underTest.create(inputCommand);

      var resultStoredCustomer = customerRepo.find(tenantId, publicId);
      assertThat(resultStoredCustomer).isPresent();
    }

    @Test
    void shouldCreateCustomerWithoutNames() {
      Map<String, Object> noGenericFields = emptyMap();
      var inputCommandWithoutNames = CreateCustomerUseCase.Command.builder()
          .tenantId("flowers")
          .publicId("CUST-1")
          .genericFields(noGenericFields)
          .build();

      var resultCustomer = underTest.create(inputCommandWithoutNames);

      assertThat(resultCustomer.getFirstName()).isNull();
      assertThat(resultCustomer.getLastName()).isNull();
    }

    @Test
    void shouldCreateCustomerWithoutGenericFields() {
      Map<String, Object> inputMissingGenericFields = null;
      var inputCommandWithoutGenericFields = CreateCustomerUseCase.Command.builder()
          .tenantId("flowers")
          .publicId("CUST-1")
          .genericFields(inputMissingGenericFields)
          .build();

      var resultCustomer = underTest.create(inputCommandWithoutGenericFields);

      assertThat(resultCustomer.getGenericFields()).isEmpty();
    }
  }

  @Nested
  class GetCustomer {

    @Test
    void shouldReturnExistingCustomer() {
      var tenantId = new TenantId("flowers");
      var publicId = new PublicId("CUST-1");
      var firstName = new FirstName("Anna");
      var storedCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .firstName(firstName)
          .build();
      customerRepo.insert(storedCustomer);
      var inputQuery = new GetCustomerUseCase.Query("flowers", "CUST-1");

      var resultCustomer = underTest.get(inputQuery);

      assertThat(resultCustomer).usingRecursiveComparison().isEqualTo(storedCustomer);
    }

    @Test
    void shouldRejectUnknownCustomer() {
      var inputQueryForUnknownCustomer = new GetCustomerUseCase.Query("flowers", "CUST-404");

      var thrown = catchThrowable(() -> underTest.get(inputQueryForUnknownCustomer));

      assertThat(thrown).isInstanceOf(CustomerNotFoundException.class);
    }
  }

  @Nested
  class DeleteCustomer {

    @Test
    void shouldDeleteCustomer() {
      var tenantId = new TenantId("flowers");
      var publicId = new PublicId("CUST-1");
      var storedCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .build();
      customerRepo.insert(storedCustomer);
      var inputCommand = new DeleteCustomerUseCase.Command("flowers", "CUST-1");

      underTest.delete(inputCommand);

      var resultStoredCustomer = customerRepo.find(tenantId, publicId);
      assertThat(resultStoredCustomer).isEmpty();
    }
  }

  @Nested
  class ListCustomers {

    private final TenantId tenantId = new TenantId("flowers");

    @Test
    void shouldReturnFirstPageWithDefaultSize() {
      var storedCustomers = storeCustomers(25);
      var inputQueryWithoutPaging = ListCustomersUseCase.Query.builder()
          .tenantId("flowers")
          .build();
      var expectedCustomers = storedCustomers.subList(0, 7);
      var expectedPage = CustomerPage.builder()
          .customers(expectedCustomers)
          .page(1)
          .size(7)
          .totalElements(25)
          .build();

      var resultPage = underTest.list(inputQueryWithoutPaging);

      assertThat(resultPage).usingRecursiveComparison().isEqualTo(expectedPage);
    }

    @Test
    void shouldReturnRequestedPage() {
      var storedCustomers = storeCustomers(25);
      var inputQueryForSecondPageOfTen = ListCustomersUseCase.Query.builder()
          .tenantId("flowers")
          .page(2)
          .size(10)
          .build();
      var expectedCustomers = storedCustomers.subList(10, 20);
      var expectedPage = CustomerPage.builder()
          .customers(expectedCustomers)
          .page(2)
          .size(10)
          .totalElements(25)
          .build();

      var resultPage = underTest.list(inputQueryForSecondPageOfTen);

      assertThat(resultPage).usingRecursiveComparison().isEqualTo(expectedPage);
    }

    @Test
    void shouldAcceptMaxPageSize() {
      storeCustomers(1);
      var inputQueryForMaxPageSize = ListCustomersUseCase.Query.builder()
          .tenantId("flowers")
          .size(15)
          .build();

      var resultPage = underTest.list(inputQueryForMaxPageSize);

      assertThat(resultPage.size()).isEqualTo(15);
    }

    @Test
    void shouldRejectTooLargeSize() {
      var inputQueryForTooLargeSize = ListCustomersUseCase.Query.builder()
          .tenantId("flowers")
          .size(16)
          .build();

      var thrown = catchThrowable(() -> underTest.list(inputQueryForTooLargeSize));

      assertThat(thrown).isInstanceOf(InvalidPageRequestException.class);
    }

    private List<Customer> storeCustomers(int count) {
      List<Customer> storedCustomers = new ArrayList<>();

      for (int number = 1; number <= count; number++) {
        String rawPublicId = "CUST-%02d".formatted(number);
        var publicId = new PublicId(rawPublicId);
        var customer = Customer.builder()
            .tenantId(tenantId)
            .publicId(publicId)
            .build();
        customerRepo.insert(customer);
        storedCustomers.add(customer);
      }

      return storedCustomers;
    }
  }
}
