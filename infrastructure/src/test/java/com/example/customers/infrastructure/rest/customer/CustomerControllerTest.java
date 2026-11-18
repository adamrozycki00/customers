package com.example.customers.infrastructure.rest.customer;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.application.port.in.GetCustomerUseCase;
import com.example.customers.application.port.in.ListCustomersUseCase;
import com.example.customers.application.port.in.UpdateCustomerUseCase;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.exception.NotFoundException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.tenant.TenantId;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

  @Autowired
  private MockMvcTester mockMvc;

  @MockitoBean
  private CreateCustomerUseCase createCustomerUseCase;

  @MockitoBean
  private GetCustomerUseCase getCustomerUseCase;

  @MockitoBean
  private ListCustomersUseCase listCustomersUseCase;

  @MockitoBean
  private UpdateCustomerUseCase updateCustomerUseCase;

  private final TenantId tenantId = new TenantId("flowers");
  private final PublicId publicId = new PublicId("CUST-1");
  private final FirstName firstName = new FirstName("Anna");
  private final LastName lastName = new LastName("Smith");
  private final FieldName vipName = new FieldName("vip");
  private final FieldName scoreName = new FieldName("score");
  private final FieldName sinceName = new FieldName("since");

  @Nested
  class Create {

    @Test
    void shouldCreateCustomer() {
      // given that the use case creates Anna Smith with vip, score and since for the expected command
      Map<String, Object> expectedRawGenericFields = Map.of(
          "vip", true,
          "score", 42,
          "since", "2024-03-01T10:00:00Z");
      var expectedCommand = CreateCustomerUseCase.Command.builder()
          .tenantId("flowers")
          .publicId("CUST-1")
          .firstName("Anna")
          .lastName("Smith")
          .genericFields(expectedRawGenericFields)
          .build();
      var since = Instant.parse("2024-03-01T10:00:00Z");
      var storedBoolean = new BooleanValue(true);
      var storedInteger = new IntegerValue(42);
      var storedDatetime = new DatetimeValue(since);
      Map<FieldName, GenericFieldValue> storedGenericFields = Map.of(
          vipName, storedBoolean,
          scoreName, storedInteger,
          sinceName, storedDatetime);
      var createdCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .firstName(firstName)
          .lastName(lastName)
          .genericFields(storedGenericFields)
          .build();
      given(createCustomerUseCase.create(expectedCommand)).willReturn(createdCustomer);
      String inputRequest = """
          {
            "public_id": "CUST-1",
            "first_name": "Anna",
            "last_name": "Smith",
            "generic_fields": {"vip": true, "score": 42, "since": "2024-03-01T10:00:00Z"}
          }
          """;
      String expectedResponse = """
          {
            "public_id": "CUST-1",
            "first_name": "Anna",
            "last_name": "Smith",
            "generic_fields": {"vip": true, "score": 42, "since": "2024-03-01T10:00:00Z"}
          }
          """;

      var resultResponse = mockMvc.post()
          .uri("/customers")
          .header("X-Tenant-Id", "flowers")
          .contentType(APPLICATION_JSON)
          .content(inputRequest)
          .exchange();

      assertThat(resultResponse).hasStatus(CREATED)
          .bodyJson().isStrictlyEqualTo(expectedResponse);
    }

    @Test
    void shouldLeaveOutMissingNames() {
      // given that the use case creates a customer without names for the expected command
      Map<String, Object> noRawGenericFields = emptyMap();
      var expectedCommand = CreateCustomerUseCase.Command.builder()
          .tenantId("flowers")
          .publicId("CUST-1")
          .genericFields(noRawGenericFields)
          .build();
      var createdCustomerWithoutNames = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .build();
      given(createCustomerUseCase.create(expectedCommand)).willReturn(createdCustomerWithoutNames);
      String inputRequest = """
          {"public_id": "CUST-1"}
          """;
      String expectedResponse = """
          {"public_id": "CUST-1", "generic_fields": {}}
          """;

      var resultResponse = mockMvc.post()
          .uri("/customers")
          .header("X-Tenant-Id", "flowers")
          .contentType(APPLICATION_JSON)
          .content(inputRequest)
          .exchange();

      assertThat(resultResponse).hasStatus(CREATED)
          .bodyJson().isStrictlyEqualTo(expectedResponse);
    }

    @Test
    void shouldReturnConflictForDuplicatePublicId() {
      var duplicatePublicId = new ConflictException("Customer with this public id already exists");
      given(createCustomerUseCase.create(any())).willThrow(duplicatePublicId);
      String inputRequest = """
          {"public_id": "CUST-1"}
          """;
      String expectedResponse = """
          {
            "status": 409,
            "title": "Conflict",
            "detail": "Customer with this public id already exists"
          }
          """;

      var resultResponse = mockMvc.post()
          .uri("/customers")
          .header("X-Tenant-Id", "flowers")
          .contentType(APPLICATION_JSON)
          .content(inputRequest)
          .exchange();

      assertThat(resultResponse).hasStatus(CONFLICT)
          .hasContentType(APPLICATION_PROBLEM_JSON)
          .bodyJson().isLenientlyEqualTo(expectedResponse);
    }
  }

  @Nested
  class Get {

    @Test
    void shouldGetCustomer() {
      // given that the use case finds Anna Smith with vip for the expected query
      var expectedQuery = new GetCustomerUseCase.Query("flowers", "CUST-1");
      var storedBoolean = new BooleanValue(true);
      Map<FieldName, GenericFieldValue> storedGenericFields = Map.of(vipName, storedBoolean);
      var storedCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .firstName(firstName)
          .lastName(lastName)
          .genericFields(storedGenericFields)
          .build();
      given(getCustomerUseCase.get(expectedQuery)).willReturn(storedCustomer);
      String expectedResponse = """
          {
            "public_id": "CUST-1",
            "first_name": "Anna",
            "last_name": "Smith",
            "generic_fields": {"vip": true}
          }
          """;

      var resultResponse = mockMvc.get()
          .uri("/customers/CUST-1")
          .header("X-Tenant-Id", "flowers")
          .exchange();

      assertThat(resultResponse).hasStatus(OK)
          .bodyJson().isStrictlyEqualTo(expectedResponse);
    }

    @Test
    void shouldReturnNotFoundForUnknownCustomer() {
      var customerNotFound = new NotFoundException("Customer not found");
      given(getCustomerUseCase.get(any())).willThrow(customerNotFound);
      String expectedResponse = """
          {
            "status": 404,
            "title": "Not Found",
            "detail": "Customer not found"
          }
          """;

      var resultResponse = mockMvc.get()
          .uri("/customers/CUST-404")
          .header("X-Tenant-Id", "flowers")
          .exchange();

      assertThat(resultResponse).hasStatus(NOT_FOUND)
          .hasContentType(APPLICATION_PROBLEM_JSON)
          .bodyJson().isLenientlyEqualTo(expectedResponse);
    }
  }

  @Nested
  class ListCustomers {

    @Test
    void shouldListCustomers() {
      // given that the use case returns page 2 of size 5 with CUST-6 and CUST-7 for the expected query
      var expectedQuery = ListCustomersUseCase.Query.builder()
          .tenantId("flowers")
          .page(2)
          .size(5)
          .build();
      var sixthPublicId = new PublicId("CUST-6");
      var seventhPublicId = new PublicId("CUST-7");
      var sixthCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(sixthPublicId)
          .build();
      var seventhCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(seventhPublicId)
          .build();
      var storedCustomers = List.of(sixthCustomer, seventhCustomer);
      var storedPage = CustomerPage.builder()
          .customers(storedCustomers)
          .page(2)
          .size(5)
          .totalElements(7)
          .build();
      given(listCustomersUseCase.list(expectedQuery)).willReturn(storedPage);
      String expectedResponse = """
          {
            "customers": [
              {"public_id": "CUST-6", "generic_fields": {}},
              {"public_id": "CUST-7", "generic_fields": {}}
            ],
            "page": 2,
            "size": 5,
            "total_elements": 7
          }
          """;

      var resultResponse = mockMvc.get()
          .uri("/customers?page=2&size=5")
          .header("X-Tenant-Id", "flowers")
          .exchange();

      assertThat(resultResponse).hasStatus(OK)
          .bodyJson().isStrictlyEqualTo(expectedResponse);
    }

    @Test
    void shouldPassMissingPagingAsNull() {
      // given that the use case returns an empty first page for a query without page and size
      var expectedQueryWithoutPaging = ListCustomersUseCase.Query.builder()
          .tenantId("flowers")
          .build();
      List<Customer> noCustomers = emptyList();
      var emptyPage = CustomerPage.builder()
          .customers(noCustomers)
          .page(1)
          .size(20)
          .totalElements(0)
          .build();
      given(listCustomersUseCase.list(expectedQueryWithoutPaging)).willReturn(emptyPage);
      String expectedResponse = """
          {"customers": [], "page": 1, "size": 20, "total_elements": 0}
          """;

      var resultResponse = mockMvc.get()
          .uri("/customers")
          .header("X-Tenant-Id", "flowers")
          .exchange();

      assertThat(resultResponse).hasStatus(OK)
          .bodyJson().isStrictlyEqualTo(expectedResponse);
    }

    @Test
    void shouldReturnBadRequestForNonNumericPage() {
      String expectedResponse = """
          {
            "status": 400,
            "title": "Bad Request",
            "detail": "Failed to convert 'page' with value: 'abc'"
          }
          """;

      var resultResponse = mockMvc.get()
          .uri("/customers?page=abc")
          .header("X-Tenant-Id", "flowers")
          .exchange();

      assertThat(resultResponse).hasStatus(BAD_REQUEST)
          .hasContentType(APPLICATION_PROBLEM_JSON)
          .bodyJson().isLenientlyEqualTo(expectedResponse);
    }
  }

  @Nested
  class Patch {

    @Test
    void shouldUpdateCustomer() {
      // given that the use case updates names, sets vip and clears score for the expected command
      Set<String> expectedFieldMask = Set.of("firstName", "lastName", "genericFields.vip", "genericFields.score");
      Map<String, Object> expectedRawGenericFields = new HashMap<>();
      expectedRawGenericFields.put("vip", false);
      expectedRawGenericFields.put("score", null);
      var expectedCommand = UpdateCustomerUseCase.Command.builder()
          .tenantId("flowers")
          .publicId("CUST-1")
          .fieldMask(expectedFieldMask)
          .firstName("Maria")
          .lastName("Nowak")
          .genericFields(expectedRawGenericFields)
          .build();
      var updatedFirstName = new FirstName("Maria");
      var updatedLastName = new LastName("Nowak");
      var updatedBoolean = new BooleanValue(false);
      Map<FieldName, GenericFieldValue> updatedGenericFields = Map.of(vipName, updatedBoolean);
      var updatedCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .firstName(updatedFirstName)
          .lastName(updatedLastName)
          .genericFields(updatedGenericFields)
          .build();
      given(updateCustomerUseCase.update(expectedCommand)).willReturn(updatedCustomer);
      String inputRequest = """
          {
            "first_name": "Maria",
            "last_name": "Nowak",
            "generic_fields": {"vip": false, "score": null}
          }
          """;
      String expectedResponse = """
          {
            "public_id": "CUST-1",
            "first_name": "Maria",
            "last_name": "Nowak",
            "generic_fields": {"vip": false}
          }
          """;

      var resultResponse = mockMvc.patch()
          .uri("/customers/CUST-1")
          .header("X-Tenant-Id", "flowers")
          .contentType(APPLICATION_JSON)
          .content(inputRequest)
          .exchange();

      assertThat(resultResponse).hasStatus(OK)
          .bodyJson().isStrictlyEqualTo(expectedResponse);
    }
  }
}
