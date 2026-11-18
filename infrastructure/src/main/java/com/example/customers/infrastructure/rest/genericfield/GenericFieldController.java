package com.example.customers.infrastructure.rest.genericfield;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.infrastructure.rest.api.GenericFieldApi;
import com.example.customers.infrastructure.rest.api.model.GenericFieldResponse;
import com.example.customers.infrastructure.rest.api.model.RegisterGenericFieldRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GenericFieldController implements GenericFieldApi {

  private final RegisterGenericFieldUseCase useCase;
  private final GenericFieldRestMapper mapper;

  @Override
  public GenericFieldResponse registerGenericField(String xTenantId, RegisterGenericFieldRequest request) {
    var command = mapper.toCommand(xTenantId, request);
    var registeredField = useCase.register(command);
    return mapper.toResponse(registeredField);
  }
}
