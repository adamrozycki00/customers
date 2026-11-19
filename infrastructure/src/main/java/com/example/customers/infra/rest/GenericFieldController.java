package com.example.customers.infra.rest;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.infra.rest.api.GenericFieldApi;
import com.example.customers.infra.rest.api.model.GenericFieldResponse;
import com.example.customers.infra.rest.api.model.RegisterGenericFieldRequest;
import com.example.customers.infra.rest.mapper.GenericFieldRestMapper;
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
