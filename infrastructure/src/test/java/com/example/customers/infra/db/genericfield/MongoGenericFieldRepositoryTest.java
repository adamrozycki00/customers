package com.example.customers.infra.db.genericfield;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.tenant.TenantId;
import com.example.customers.infra.config.TestcontainersConfig;
import java.util.ArrayList;
import java.util.List;
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
@Import({TestcontainersConfig.class, MongoGenericFieldRepository.class})
class MongoGenericFieldRepositoryTest {

  private static final String GENERIC_FIELDS_COLLECTION = "genericFields";
  private static final TenantId TENANT_ID = new TenantId("FLOWERS");
  private static final FieldName VIP_NAME = new FieldName("vip");

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private MongoGenericFieldRepository underTest;

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
        .tenantId(TENANT_ID)
        .name(VIP_NAME)
        .type(FieldType.BOOL)
        .build();
    var expectedId = new Document("tenantId", "FLOWERS")
        .append("name", "vip");
    var expectedDoc = new Document("_id", expectedId)
        .append("type", "BOOL");
    var expectedJson = expectedDoc.toJson();

    underTest.insert(inputField);

    var storedDocs = mongoTemplate.findAll(Document.class, GENERIC_FIELDS_COLLECTION);
    assertThat(storedDocs).singleElement().extracting(Document::toJson).isEqualTo(expectedJson);
  }

  @Test
  void shouldRejectDuplicateFieldName() {
    storeFieldDocument("FLOWERS", "vip", "BOOL");
    var inputFieldWithDuplicateName = GenericField.builder()
        .tenantId(TENANT_ID)
        .name(VIP_NAME)
        .type(FieldType.STRING)
        .build();

    var thrown = catchThrowable(() -> underTest.insert(inputFieldWithDuplicateName));

    assertThat(thrown).isInstanceOf(ConflictException.class);
  }

  @Test
  void shouldFindField() {
    storeFieldDocument("FLOWERS", "vip", "BOOL");
    var expectedField = GenericField.builder()
        .tenantId(TENANT_ID)
        .name(VIP_NAME)
        .type(FieldType.BOOL)
        .build();

    var resultFields = underTest.findAllByTenant(TENANT_ID);

    assertThat(resultFields).singleElement().usingRecursiveComparison().isEqualTo(expectedField);
  }

  @ParameterizedTest
  @EnumSource(FieldType.class)
  void shouldFindFieldOfEveryType(FieldType fieldType) {
    var storedType = fieldType.name();
    storeFieldDocument("FLOWERS", "vip", storedType);

    var resultFields = underTest.findAllByTenant(TENANT_ID);

    assertThat(resultFields).singleElement().extracting(GenericField::getType).isEqualTo(fieldType);
  }

  @Test
  void shouldFindOnlyFieldsOfGivenTenant() {
    storeFieldDocument("FLOWERS", "vip", "BOOL");
    storeFieldDocument("TREES", "vip", "STRING");
    var expectedField = GenericField.builder()
        .tenantId(TENANT_ID)
        .name(VIP_NAME)
        .type(FieldType.BOOL)
        .build();

    var resultFields = underTest.findAllByTenant(TENANT_ID);

    assertThat(resultFields).singleElement().usingRecursiveComparison().isEqualTo(expectedField);
  }

  @Test
  void shouldFindTenantFieldsUsingIndex() {
    storeFieldDocument("FLOWERS", "vip", "BOOL");
    storeFieldDocument("TREES", "vip", "STRING");
    enableProfiler();
    String expectedPlanSummary = "IXSCAN { _id: 1 }";

    underTest.findAllByTenant(TENANT_ID);

    var planSummaries = getFindPlanSummaries();
    assertThat(planSummaries).singleElement().isEqualTo(expectedPlanSummary);
  }

  @Test
  void shouldFindAllFields() {
    // given fields of two tenants
    storeFieldDocument("FLOWERS", "vip", "BOOL");
    storeFieldDocument("TREES", "score", "INTEGER");
    var treesTenantId = new TenantId("TREES");
    var scoreName = new FieldName("score");
    var expectedFlowersField = GenericField.builder()
        .tenantId(TENANT_ID)
        .name(VIP_NAME)
        .type(FieldType.BOOL)
        .build();
    var expectedTreesField = GenericField.builder()
        .tenantId(treesTenantId)
        .name(scoreName)
        .type(FieldType.INTEGER)
        .build();

    var resultFields = underTest.findAll();

    assertThat(resultFields).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(expectedFlowersField, expectedTreesField);
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

  private void storeFieldDocument(String rawTenantId, String name, String type) {
    var storedId = new Document("tenantId", rawTenantId)
        .append("name", name);
    var storedDoc = new Document("_id", storedId)
        .append("type", type);
    mongoTemplate.insert(storedDoc, GENERIC_FIELDS_COLLECTION);
  }
}
