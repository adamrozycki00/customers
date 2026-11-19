package com.example.customers.infrastructure.db.genericfield;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.TestcontainersConfiguration;
import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

@DataMongoTest
@Import({TestcontainersConfiguration.class, MongoGenericFieldRepository.class})
class MongoGenericFieldRepositoryTest {

  private static final String GENERIC_FIELDS_COLLECTION = "genericFields";

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private MongoGenericFieldRepository underTest;

  private final TenantId tenantId = new TenantId("FLOWERS");
  private final FieldName vipName = new FieldName("vip");
  private final Set<String> noExpectedNames = emptySet();

  @BeforeEach
  void clearCollection() {
    mongoTemplate.dropCollection(GENERIC_FIELDS_COLLECTION);
  }

  @AfterEach
  void disableProfiler() {
    var profileNothing = new Document("profile", 0);
    mongoTemplate.getDb().runCommand(profileNothing);
  }

  @Test
  void shouldInsertField() {
    var inputField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOLEAN)
        .build();
    var expectedId = new Document("tenantId", "FLOWERS")
        .append("name", "vip");
    var expectedDoc = new Document("_id", expectedId)
        .append("type", "BOOLEAN");
    var expectedJson = expectedDoc.toJson();

    underTest.insert(inputField);

    var storedDocs = mongoTemplate.findAll(Document.class, GENERIC_FIELDS_COLLECTION);
    assertThat(storedDocs).singleElement().extracting(Document::toJson).isEqualTo(expectedJson);
  }

  @Test
  void shouldRejectDuplicateFieldName() {
    storeFieldDocument("FLOWERS", "vip", "BOOLEAN");
    var inputFieldWithDuplicateName = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.STRING)
        .build();

    var thrown = catchThrowable(() -> underTest.insert(inputFieldWithDuplicateName));

    assertThat(thrown).isInstanceOf(ConflictException.class)
        .hasMessage("Generic field already registered");
  }

  @Test
  void shouldFindField() {
    storeFieldDocument("FLOWERS", "vip", "BOOLEAN");
    var expectedField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOLEAN)
        .build();

    var resultFields = underTest.findAllByTenantIncluding(tenantId, noExpectedNames);

    assertThat(resultFields).singleElement().usingRecursiveComparison().isEqualTo(expectedField);
  }

  @ParameterizedTest
  @EnumSource(FieldType.class)
  void shouldFindFieldOfEveryType(FieldType fieldType) {
    var storedType = fieldType.name();
    storeFieldDocument("FLOWERS", "vip", storedType);

    var resultFields = underTest.findAllByTenantIncluding(tenantId, noExpectedNames);

    assertThat(resultFields).singleElement().extracting(GenericField::getType).isEqualTo(fieldType);
  }

  @Test
  void shouldFindOnlyFieldsOfGivenTenant() {
    storeFieldDocument("FLOWERS", "vip", "BOOLEAN");
    storeFieldDocument("TREES", "vip", "STRING");
    var expectedField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOLEAN)
        .build();

    var resultFields = underTest.findAllByTenantIncluding(tenantId, noExpectedNames);

    assertThat(resultFields).singleElement().usingRecursiveComparison().isEqualTo(expectedField);
  }

  @Test
  void shouldFindTenantFieldsUsingIndex() {
    storeFieldDocument("FLOWERS", "vip", "BOOLEAN");
    storeFieldDocument("TREES", "vip", "STRING");
    enableProfiler();
    String expectedPlanSummary = "IXSCAN { _id: 1 }";

    underTest.findAllByTenantIncluding(tenantId, noExpectedNames);

    var planSummaries = getFindPlanSummaries();
    assertThat(planSummaries).singleElement().isEqualTo(expectedPlanSummary);
  }

  private void enableProfiler() {
    var profileAllOperations = new Document("profile", 2);
    mongoTemplate.getDb().runCommand(profileAllOperations);
  }

  private List<String> getFindPlanSummaries() {
    var database = mongoTemplate.getDb();
    var namespace = database.getName() + "." + GENERIC_FIELDS_COLLECTION;
    var findOperationsFilter = new Document("ns", namespace)
        .append("op", "query");
    return database.getCollection("system.profile")
        .find(findOperationsFilter)
        .map(entry -> entry.getString("planSummary"))
        .into(new ArrayList<>());
  }

  private void storeFieldDocument(String tenantId, String name, String type) {
    var storedId = new Document("tenantId", tenantId)
        .append("name", name);
    var storedDoc = new Document("_id", storedId)
        .append("type", type);
    mongoTemplate.insert(storedDoc, GENERIC_FIELDS_COLLECTION);
  }
}
