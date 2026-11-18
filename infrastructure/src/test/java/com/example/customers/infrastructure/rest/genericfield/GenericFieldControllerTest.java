package com.example.customers.infrastructure.rest.genericfield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(GenericFieldController.class)
class GenericFieldControllerTest {

  @Autowired
  private MockMvcTester mockMvc;

  @MockitoBean
  private RegisterGenericFieldUseCase registerGenericFieldUseCase;

  @Test
  void shouldRegisterGenericField() {
    // given that the use case registers vip as BOOLEAN for the expected command
    var expectedCommand = RegisterGenericFieldUseCase.Command.builder()
        .tenantId("flowers")
        .name("vip")
        .type("BOOLEAN")
        .build();
    var tenantId = new TenantId("flowers");
    var vipName = new FieldName("vip");
    var registeredField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOLEAN)
        .build();
    given(registerGenericFieldUseCase.register(expectedCommand)).willReturn(registeredField);
    String inputRequest = """
        {"name": "vip", "type": "BOOLEAN"}
        """;
    String expectedResponse = """
        {"name": "vip", "type": "BOOLEAN"}
        """;

    var resultResponse = mockMvc.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .content(inputRequest)
        .exchange();

    assertThat(resultResponse).hasStatus(CREATED)
        .bodyJson().isStrictlyEqualTo(expectedResponse);
  }
}
