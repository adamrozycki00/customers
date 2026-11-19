package com.example.customers.infrastructure.db.genericfield;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CachingGenericFieldAdapterTest {

  @Mock
  private MongoGenericFieldRepository repository;

  @InjectMocks
  private CachingGenericFieldAdapter underTest;

  private final TenantId tenantId = new TenantId("FLOWERS");
  private final FieldName vipName = new FieldName("vip");
  private final GenericField vipField = GenericField.builder()
      .tenantId(tenantId)
      .name(vipName)
      .type(FieldType.BOOLEAN)
      .build();
  private final FieldName scoreName = new FieldName("score");
  private final GenericField scoreField = GenericField.builder()
      .tenantId(tenantId)
      .name(scoreName)
      .type(FieldType.INTEGER)
      .build();

  @Test
  void shouldServeCachedFieldsWithoutQueryingDatabase() {
    var storedFields = List.of(vipField);
    var inputExpectedNames = Set.of("vip");
    when(repository.findAll()).thenReturn(storedFields);
    underTest.refreshAll();

    var resultFields = underTest.findAllByTenantIncluding(tenantId, inputExpectedNames);

    assertThat(resultFields).containsExactly(vipField);
    verify(repository).findAll();
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldReloadTenantWhenExpectedFieldIsMissing() {
    // given the cache holding only vip while the database also has score
    var cachedFields = List.of(vipField);
    var storedFields = List.of(vipField, scoreField);
    var inputExpectedNames = Set.of("score");
    when(repository.findAll()).thenReturn(cachedFields);
    when(repository.findAllByTenant(tenantId)).thenReturn(storedFields);
    underTest.refreshAll();

    var resultFields = underTest.findAllByTenantIncluding(tenantId, inputExpectedNames);

    assertThat(resultFields).containsExactly(vipField, scoreField);
    verify(repository).findAllByTenant(tenantId);
  }

  @Test
  void shouldCacheReloadedTenant() {
    // given score already requested once while the cache held only vip
    var cachedFields = List.of(vipField);
    var storedFields = List.of(vipField, scoreField);
    var inputExpectedNames = Set.of("score");
    when(repository.findAll()).thenReturn(cachedFields);
    when(repository.findAllByTenant(tenantId)).thenReturn(storedFields);
    underTest.refreshAll();
    underTest.findAllByTenantIncluding(tenantId, inputExpectedNames);

    var resultFields = underTest.findAllByTenantIncluding(tenantId, inputExpectedNames);

    assertThat(resultFields).containsExactly(vipField, scoreField);
    verify(repository).findAllByTenant(tenantId);
  }

  @Test
  void shouldLoadTenantOnFirstRequest() {
    var storedFields = List.of(vipField);
    var inputExpectedNames = Set.of("vip");
    when(repository.findAllByTenant(tenantId)).thenReturn(storedFields);

    var resultFields = underTest.findAllByTenantIncluding(tenantId, inputExpectedNames);

    assertThat(resultFields).containsExactly(vipField);
    verify(repository).findAllByTenant(tenantId);
  }

  @Test
  void shouldNotCacheTenantWithoutFields() {
    Set<String> inputNoExpectedNames = emptySet();
    underTest.findAllByTenantIncluding(tenantId, inputNoExpectedNames);

    var resultFields = underTest.findAllByTenantIncluding(tenantId, inputNoExpectedNames);

    assertThat(resultFields).isEmpty();
    verify(repository, times(2)).findAllByTenant(tenantId);
  }

  @Test
  void shouldRefreshAllTenants() {
    // given score added to the database between two refreshes
    var initialFields = List.of(vipField);
    var refreshedFields = List.of(vipField, scoreField);
    var inputExpectedNames = Set.of("vip");
    when(repository.findAll()).thenReturn(initialFields, refreshedFields);
    underTest.refreshAll();
    underTest.refreshAll();

    var resultFields = underTest.findAllByTenantIncluding(tenantId, inputExpectedNames);

    assertThat(resultFields).containsExactly(vipField, scoreField);
    verify(repository, times(2)).findAll();
    verifyNoMoreInteractions(repository);
  }

  @Test
  void shouldAddRegisteredFieldToCache() {
    // given the cache holding vip
    var cachedFields = List.of(vipField);
    var inputExpectedNames = Set.of("score");
    when(repository.findAll()).thenReturn(cachedFields);
    when(repository.insert(scoreField)).thenReturn(scoreField);
    underTest.refreshAll();

    underTest.insert(scoreField);

    var resultFields = underTest.findAllByTenantIncluding(tenantId, inputExpectedNames);
    assertThat(resultFields).containsExactly(vipField, scoreField);
    verify(repository).findAll();
    verify(repository).insert(scoreField);
    verifyNoMoreInteractions(repository);
  }
}
