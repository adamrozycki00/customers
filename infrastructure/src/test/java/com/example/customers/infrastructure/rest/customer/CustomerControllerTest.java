package com.example.customers.infrastructure.rest.customer;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

import com.example.customers.application.port.in.CreateCustomerUseCase;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.tenant.TenantId;
import java.time.Instant;
import java.util.Map;
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
}
