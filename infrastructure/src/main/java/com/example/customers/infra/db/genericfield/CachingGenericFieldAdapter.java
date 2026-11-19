package com.example.customers.infra.db.genericfield;

import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachingGenericFieldAdapter implements GenericFieldPort, MeterBinder {

  private static final String CACHE_NAME = "genericFields";

  private final MongoGenericFieldRepository db;
  private final Cache<TenantId, List<GenericField>> fieldsByTenant = Caffeine.newBuilder()
      .recordStats()
      .build();

  @Override
  public GenericField insert(GenericField field) {
    var insertedField = db.insert(field);
    var tenantId = insertedField.getTenantId();
    var name = insertedField.getName();
    var type = insertedField.getType();
    fieldsByTenant.asMap()
        .computeIfPresent(tenantId, (cachedTenantId, cachedFields) -> withField(cachedFields, insertedField));
    log.info("Generic field {} ({}) registered for tenant {}", name.value(), type, tenantId.value());
    return insertedField;
  }

  @Override
  public List<GenericField> findAllByTenantIncluding(TenantId tenantId, @NonNull Set<String> expectedNames) {
    var cachedFields = fieldsByTenant.getIfPresent(tenantId);

    if (containsAllNames(cachedFields, expectedNames)) {
      return cachedFields;
    }

    var reloadedFields = db.findAllByTenant(tenantId);
    log.info("Generic fields of tenant {} loaded from DB ({} found)", tenantId.value(), reloadedFields.size());

    if (isNotEmpty(reloadedFields)) {
      fieldsByTenant.put(tenantId, reloadedFields);
    }

    return reloadedFields;
  }

  @Scheduled(fixedRateString = "${customers.generic-fields.cache.refresh-interval-minutes}", timeUnit = MINUTES)
  public void refreshAll() {
    var startNanos = System.nanoTime();

    try {
      var fields = db.findAll();
      var fieldsByTenant = fields.stream()
          .collect(groupingBy(GenericField::getTenantId));
      this.fieldsByTenant.putAll(fieldsByTenant);
      var endNanos = System.nanoTime();
      var elapsedMillis = NANOSECONDS.toMillis(endNanos - startNanos);
      log.info("Generic field cache refreshed: {} fields of {} tenants ({} ms)",
          fields.size(), fieldsByTenant.size(), elapsedMillis);
    } catch (DataAccessException e) {
      var cachedTenantCount = this.fieldsByTenant.estimatedSize();
      log.warn("Generic field cache refresh failed, keeping {} cached tenants: {}", cachedTenantCount, e.getMessage());
    } catch (RuntimeException e) {
      log.error("Generic field cache refresh failed", e);
    }
  }

  @Override
  public void bindTo(MeterRegistry registry) {
    CaffeineCacheMetrics.monitor(registry, fieldsByTenant, CACHE_NAME);
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
