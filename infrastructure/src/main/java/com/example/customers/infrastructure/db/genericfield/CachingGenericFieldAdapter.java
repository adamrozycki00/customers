package com.example.customers.infrastructure.db.genericfield;

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CachingGenericFieldAdapter implements GenericFieldPort {

  private final MongoGenericFieldRepository db;
  private final Cache<TenantId, List<GenericField>> fieldsByTenant = Caffeine.newBuilder().build();

  @Override
  public GenericField insert(GenericField field) {
    var insertedField = db.insert(field);
    var tenantId = insertedField.getTenantId();
    fieldsByTenant.asMap()
        .computeIfPresent(tenantId, (cachedTenantId, cachedFields) -> withField(cachedFields, insertedField));
    return insertedField;
  }

  @Override
  public List<GenericField> findAllByTenantIncluding(TenantId tenantId, @NonNull Set<String> expectedNames) {
    var cachedFields = fieldsByTenant.getIfPresent(tenantId);

    if (containsAllNames(cachedFields, expectedNames)) {
      return cachedFields;
    }

    var reloadedFields = db.findAllByTenant(tenantId);

    if (isNotEmpty(reloadedFields)) {
      fieldsByTenant.put(tenantId, reloadedFields);
    }

    return reloadedFields;
  }

  @Scheduled(
      fixedRateString = "${customers.generic-fields.cache.refresh-interval-minutes}",
      initialDelayString = "${customers.generic-fields.cache.refresh-interval-minutes}",
      timeUnit = MINUTES)
  public void refreshAll() {
    var fields = db.findAll();
    var fieldsByTenant = fields.stream()
        .collect(groupingBy(GenericField::getTenantId));
    this.fieldsByTenant.putAll(fieldsByTenant);
  }

  private static List<GenericField> withField(List<GenericField> fields, GenericField field) {
    var updatedFields = new ArrayList<>(fields);
    updatedFields.add(field);
    return updatedFields;
  }

  private static boolean containsAllNames(List<GenericField> fields, Set<String> names) {
    if (isNull(fields)) {
      return false;
    }

    var fieldNames = fields.stream()
        .map(field -> field.getName().value())
        .collect(toSet());
    return fieldNames.containsAll(names);
  }
}
