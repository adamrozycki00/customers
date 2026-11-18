package com.example.customers.infrastructure.rest.genericfield;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.infrastructure.rest.api.GenericFieldsApi;
import com.example.customers.infrastructure.rest.api.model.GenericFieldResponse;
import com.example.customers.infrastructure.rest.api.model.RegisterGenericFieldRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GenericFieldController implements GenericFieldsApi {

  private final RegisterGenericFieldUseCase registerGenericFieldUseCase;

  @Override
  public GenericFieldResponse registerGenericField(String xTenantId, RegisterGenericFieldRequest request) {
    var command = RegisterGenericFieldUseCase.Command.builder()
        .tenantId(xTenantId)
        .name(request.getName())
        .type(request.getType())
        .build();
    var registeredField = registerGenericFieldUseCase.register(command);
    return toResponse(registeredField);
  }

  private static GenericFieldResponse toResponse(GenericField field) {
    var name = field.getName();
    var type = field.getType();
    return new GenericFieldResponse(name.value(), type.name());
  }
}
