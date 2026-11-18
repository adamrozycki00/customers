package com.example.customers.infrastructure.rest.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.domain.genericfield.exception.DuplicateFieldNameException;
import com.example.customers.domain.genericfield.exception.InvalidFieldTypeException;
import com.example.customers.domain.genericfield.exception.InvalidGenericFieldValuesException;
import com.example.customers.infrastructure.rest.genericfield.GenericFieldController;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(GenericFieldController.class)
class RestExceptionHandlerTest {

  @Autowired
  private MockMvcTester mockMvc;

  @MockitoBean
  private RegisterGenericFieldUseCase registerGenericFieldUseCase;

  @Test
  void shouldReturnBadRequestForDomainValidationException() {
    var invalidFieldType = new InvalidFieldTypeException();
    given(registerGenericFieldUseCase.register(any())).willThrow(invalidFieldType);
    String inputRequest = """
        {"name": "vip", "type": "DATE"}
        """;
    String expectedResponse = """
        {
          "status": 400,
          "title": "Bad Request",
          "detail": "Unknown field type; use one of BOOLEAN, STRING, INTEGER, DATETIME"
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
        {"name": "vip", "type": "BOOLEAN"}
        """;
    String expectedResponse = """
        {
          "status": 400,
          "title": "Bad Request",
          "detail": "Unregistered generic fields or values of the wrong type",
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
    var duplicateFieldName = new DuplicateFieldNameException();
    given(registerGenericFieldUseCase.register(any())).willThrow(duplicateFieldName);
    String inputRequest = """
        {"name": "vip", "type": "BOOLEAN"}
        """;
    String expectedResponse = """
        {
          "status": 409,
          "title": "Conflict",
          "detail": "Generic field already registered"
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
  void shouldHideDetailsOfUnexpectedException() {
    var unexpectedException = new RuntimeException("Connection refused: mongodb://secret-host");
    given(registerGenericFieldUseCase.register(any())).willThrow(unexpectedException);
    String inputRequest = """
        {"name": "vip", "type": "BOOLEAN"}
        """;
    String expectedResponse = """
        {
          "status": 500,
          "title": "Internal Server Error",
          "detail": "Unexpected error"
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
        {"name": "vip", "type": "BOOLEAN"}
        """;
    String expectedResponse = """
        {
          "status": 400,
          "title": "Bad Request",
          "detail": "Required header 'X-Tenant-Id' is not present."
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
  void shouldReturnBadRequestForMalformedJson() {
    String inputMalformedRequest = """
        {"name": "vip",
        """;
    String expectedResponse = """
        {
          "status": 400,
          "title": "Bad Request",
          "detail": "Failed to read request"
        }
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .content(inputMalformedRequest)
        .exchange();

    assertThat(resultResponse).hasStatus(BAD_REQUEST)
        .hasContentType(APPLICATION_PROBLEM_JSON)
        .bodyJson().isLenientlyEqualTo(expectedResponse);
  }
}
