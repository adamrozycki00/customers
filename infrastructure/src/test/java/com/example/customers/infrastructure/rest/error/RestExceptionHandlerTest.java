package com.example.customers.infrastructure.rest.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValuesException;
import com.example.customers.domain.tenant.TenantId;
import com.example.customers.infrastructure.rest.genericfield.GenericFieldController;
import com.example.customers.infrastructure.rest.genericfield.GenericFieldRestMapperImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(GenericFieldController.class)
@Import(GenericFieldRestMapperImpl.class)
class RestExceptionHandlerTest {

  @Autowired
  private MockMvcTester mockMvc;

  @MockitoBean
  private RegisterGenericFieldUseCase registerGenericFieldUseCase;

  @Test
  void shouldReturnBadRequestForValidationException() {
    var invalidFieldType = new ValidationException("Unknown field type; use one of BOOL, STRING, INTEGER, DATETIME");
    given(registerGenericFieldUseCase.register(any())).willThrow(invalidFieldType);
    String inputRequest = """
        {"name": "vip", "type": "DATE"}
        """;
    String expectedResponse = """
        {
          "status": 400,
          "title": "Bad Request"
        }
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .content(inputRequest)
        .exchange();

    assertThat(resultResponse).hasStatus(BAD_REQUEST)
        .hasContentType(APPLICATION_PROBLEM_JSON)
        .bodyJson().isLenientlyEqualTo(expectedResponse);
  }

  @Test
  void shouldListInvalidFieldNamesForInvalidGenericFieldValues() {
    // given a use case rejecting vipp and ghost as invalid generic fields
    var invalidFieldNames = List.of("vipp", "ghost");
    var invalidGenericFieldValues = new InvalidGenericFieldValuesException(invalidFieldNames);
    given(registerGenericFieldUseCase.register(any())).willThrow(invalidGenericFieldValues);
    String inputRequest = """
        {"name": "vip", "type": "BOOL"}
        """;
    String expectedResponse = """
        {
          "status": 400,
          "title": "Bad Request",
          "errors": ["vipp", "ghost"]
        }
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .content(inputRequest)
        .exchange();

    assertThat(resultResponse).hasStatus(BAD_REQUEST)
        .hasContentType(APPLICATION_PROBLEM_JSON)
        .bodyJson().isLenientlyEqualTo(expectedResponse);
  }

  @Test
  void shouldReturnConflictForConflictException() {
    var duplicateFieldName = new ConflictException("Generic field already registered");
    given(registerGenericFieldUseCase.register(any())).willThrow(duplicateFieldName);
    String inputRequest = """
        {"name": "vip", "type": "BOOL"}
        """;
    String expectedResponse = """
        {
          "status": 409,
          "title": "Conflict"
        }
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .content(inputRequest)
        .exchange();

    assertThat(resultResponse).hasStatus(CONFLICT)
        .hasContentType(APPLICATION_PROBLEM_JSON)
        .bodyJson().isLenientlyEqualTo(expectedResponse);
  }

  @Test
  void shouldReturnServiceUnavailableForUnavailableDatabase() {
    var databaseUnavailable = new DataAccessResourceFailureException("Timed out: mongodb://secret-host");
    given(registerGenericFieldUseCase.register(any())).willThrow(databaseUnavailable);
    String inputRequest = """
        {"name": "vip", "type": "BOOL"}
        """;
    String expectedResponse = """
        {
          "status": 503,
          "title": "Service Unavailable"
        }
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .content(inputRequest)
        .exchange();

    assertThat(resultResponse).hasStatus(SERVICE_UNAVAILABLE)
        .hasContentType(APPLICATION_PROBLEM_JSON)
        .bodyJson().isLenientlyEqualTo(expectedResponse);
    assertThat(resultResponse).headers().hasValue(RETRY_AFTER, "5");
  }

  @Test
  void shouldHideDetailsOfUnexpectedException() {
    var unexpectedException = new RuntimeException("Connection refused: mongodb://secret-host");
    given(registerGenericFieldUseCase.register(any())).willThrow(unexpectedException);
    String inputRequest = """
        {"name": "vip", "type": "BOOL"}
        """;
    String expectedResponse = """
        {
          "status": 500,
          "title": "Internal Server Error"
        }
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .content(inputRequest)
        .exchange();

    assertThat(resultResponse).hasStatus(INTERNAL_SERVER_ERROR)
        .hasContentType(APPLICATION_PROBLEM_JSON)
        .bodyJson().isLenientlyEqualTo(expectedResponse);
    assertThat(resultResponse).bodyText().doesNotContain("secret-host");
  }

  @Test
  void shouldReturnBadRequestForMissingTenantHeader() {
    String inputRequest = """
        {"name": "vip", "type": "BOOL"}
        """;
    String expectedResponse = """
        {
          "status": 400,
          "title": "Bad Request"
        }
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .contentType(APPLICATION_JSON)
        .content(inputRequest)
        .exchange();

    assertThat(resultResponse).hasStatus(BAD_REQUEST)
        .hasContentType(APPLICATION_PROBLEM_JSON)
        .bodyJson().isLenientlyEqualTo(expectedResponse);
  }

  @Test
  void shouldReturnBadRequestForUnknownProperty() {
    // given a use case that would register the field and a request with an unknown property
    var tenantId = new TenantId("flowers");
    var vipName = new FieldName("vip");
    var registeredField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOL)
        .build();
    given(registerGenericFieldUseCase.register(any())).willReturn(registeredField);
    String inputRequestWithUnknownProperty = """
        {"name": "vip", "type": "BOOL", "color": "gold"}
        """;
    String expectedResponse = """
        {
          "status": 400,
          "title": "Bad Request"
        }
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .content(inputRequestWithUnknownProperty)
        .exchange();

    assertThat(resultResponse).hasStatus(BAD_REQUEST)
        .hasContentType(APPLICATION_PROBLEM_JSON)
        .bodyJson().isLenientlyEqualTo(expectedResponse);
  }
}
