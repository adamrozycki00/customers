package com.example.customers.infra.db.genericfield;

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

  private static final TenantId TENANT_ID = new TenantId("FLOWERS");
  private static final FieldName VIP_NAME = new FieldName("vip");
  private static final GenericField VIP_FIELD = GenericField.builder()
      .tenantId(TENANT_ID)
      .name(VIP_NAME)
      .type(FieldType.BOOL)
      .build();
  private static final FieldName SCORE_NAME = new FieldName("score");
  private static final GenericField SCORE_FIELD = GenericField.builder()
      .tenantId(TENANT_ID)
      .name(SCORE_NAME)
      .type(FieldType.INTEGER)
      .build();

  @Mock
  private MongoGenericFieldRepository repo;

  @InjectMocks
  private CachingGenericFieldAdapter underTest;

  @Test
  void shouldServeCachedFieldsWithoutQueryingDatabase() {
    var storedFields = List.of(VIP_FIELD);
    var inputExpectedNames = Set.of("vip");
    when(repo.findAll()).thenReturn(storedFields);
    underTest.refreshAll();

    var resultFields = underTest.findAllByTenantIncluding(TENANT_ID, inputExpectedNames);

    assertThat(resultFields).containsExactly(VIP_FIELD);
    verify(repo).findAll();
    verifyNoMoreInteractions(repo);
  }

  @Test
  void shouldReloadTenantWhenExpectedFieldIsMissing() {
    // given the cache holding only vip while the database also has score
    var cachedFields = List.of(VIP_FIELD);
    var storedFields = List.of(VIP_FIELD, SCORE_FIELD);
    var inputExpectedNames = Set.of("score");
    when(repo.findAll()).thenReturn(cachedFields);
    when(repo.findAllByTenant(TENANT_ID)).thenReturn(storedFields);
    underTest.refreshAll();

    var resultFields = underTest.findAllByTenantIncluding(TENANT_ID, inputExpectedNames);

    assertThat(resultFields).containsExactly(VIP_FIELD, SCORE_FIELD);
    verify(repo).findAllByTenant(TENANT_ID);
  }

  @Test
  void shouldCacheReloadedTenant() {
    // given score already requested once while the cache held only vip
    var cachedFields = List.of(VIP_FIELD);
    var storedFields = List.of(VIP_FIELD, SCORE_FIELD);
    var inputExpectedNames = Set.of("score");
    when(repo.findAll()).thenReturn(cachedFields);
    when(repo.findAllByTenant(TENANT_ID)).thenReturn(storedFields);
    underTest.refreshAll();
    underTest.findAllByTenantIncluding(TENANT_ID, inputExpectedNames);

    var resultFields = underTest.findAllByTenantIncluding(TENANT_ID, inputExpectedNames);

    assertThat(resultFields).containsExactly(VIP_FIELD, SCORE_FIELD);
    verify(repo).findAllByTenant(TENANT_ID);
  }

  @Test
  void shouldLoadTenantOnFirstRequest() {
    var storedFields = List.of(VIP_FIELD);
    var inputExpectedNames = Set.of("vip");
    when(repo.findAllByTenant(TENANT_ID)).thenReturn(storedFields);

    var resultFields = underTest.findAllByTenantIncluding(TENANT_ID, inputExpectedNames);

    assertThat(resultFields).containsExactly(VIP_FIELD);
    verify(repo).findAllByTenant(TENANT_ID);
  }

  @Test
  void shouldNotCacheTenantWithoutFields() {
    Set<String> inputNoExpectedNames = emptySet();
    underTest.findAllByTenantIncluding(TENANT_ID, inputNoExpectedNames);

    var resultFields = underTest.findAllByTenantIncluding(TENANT_ID, inputNoExpectedNames);

    assertThat(resultFields).isEmpty();
    verify(repo, times(2)).findAllByTenant(TENANT_ID);
  }

  @Test
  void shouldRefreshAllTenants() {
    // given score added to the database between two refreshes
    var initialFields = List.of(VIP_FIELD);
    var refreshedFields = List.of(VIP_FIELD, SCORE_FIELD);
    var inputExpectedNames = Set.of("vip");
    when(repo.findAll()).thenReturn(initialFields, refreshedFields);
    underTest.refreshAll();
    underTest.refreshAll();

    var resultFields = underTest.findAllByTenantIncluding(TENANT_ID, inputExpectedNames);

    assertThat(resultFields).containsExactly(VIP_FIELD, SCORE_FIELD);
    verify(repo, times(2)).findAll();
    verifyNoMoreInteractions(repo);
  }

  @Test
  void shouldAddRegisteredFieldToCache() {
    // given the cache holding vip
    var cachedFields = List.of(VIP_FIELD);
    var inputExpectedNames = Set.of("score");
    when(repo.findAll()).thenReturn(cachedFields);
    when(repo.insert(SCORE_FIELD)).thenReturn(SCORE_FIELD);
    underTest.refreshAll();

    underTest.insert(SCORE_FIELD);

    var resultFields = underTest.findAllByTenantIncluding(TENANT_ID, inputExpectedNames);
    assertThat(resultFields).containsExactly(VIP_FIELD, SCORE_FIELD);
    verify(repo).findAll();
    verify(repo).insert(SCORE_FIELD);
    verifyNoMoreInteractions(repo);
  }
}
