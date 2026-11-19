package com.example.customers.infra.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import com.example.customers.infra.rest.mapper.GenericFieldRestMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(GenericFieldController.class)
@Import(GenericFieldRestMapperImpl.class)
class GenericFieldControllerTest {

  private static final TenantId TENANT_ID = new TenantId("flowers");
  private static final FieldName VIP_NAME = new FieldName("vip");

  @Autowired
  private MockMvcTester mockMvc;

  @MockitoBean
  private RegisterGenericFieldUseCase registerGenericFieldUseCase;

  @Test
  void shouldRegisterGenericField() {
    // given a use case registering vip for the tenant from the header
    var expectedCommand = RegisterGenericFieldUseCase.Command.builder()
        .tenantId("flowers")
        .name("vip")
        .type("BOOL")
        .build();
    var registeredField = GenericField.builder()
        .tenantId(TENANT_ID)
        .name(VIP_NAME)
        .type(FieldType.BOOL)
        .build();
    given(registerGenericFieldUseCase.register(expectedCommand)).willReturn(registeredField);
    String inputRequest = """
        {"name": "vip", "type": "BOOL"}
        """;
    String expectedResponse = """
        {"name": "vip", "type": "BOOL"}
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
