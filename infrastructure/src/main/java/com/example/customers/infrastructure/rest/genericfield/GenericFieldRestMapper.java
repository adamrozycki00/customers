package com.example.customers.infrastructure.rest.genericfield;

import com.example.customers.application.port.in.RegisterGenericFieldUseCase;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.infrastructure.rest.RestMapperConfig;
import com.example.customers.infrastructure.rest.api.model.GenericFieldResponse;
import com.example.customers.infrastructure.rest.api.model.RegisterGenericFieldRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = RestMapperConfig.class)
public interface GenericFieldRestMapper {

  @Mapping(target = "tenantId", source = "tenantId")
  RegisterGenericFieldUseCase.Command toCommand(String tenantId, RegisterGenericFieldRequest request);

  @Mapping(target = "name", source = "name.value")
  GenericFieldResponse toResponse(GenericField field);
}
