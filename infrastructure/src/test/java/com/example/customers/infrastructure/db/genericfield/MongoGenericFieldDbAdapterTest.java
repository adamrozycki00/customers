package com.example.customers.infrastructure.db.genericfield;

import com.example.customers.TestcontainersConfiguration;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.genericfield.exception.DuplicateFieldNameException;
import com.example.customers.domain.tenant.TenantId;
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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataMongoTest
@Import({TestcontainersConfiguration.class, MongoGenericFieldDbAdapter.class})
class MongoGenericFieldDbAdapterTest {

  private static final String GENERIC_FIELDS_COLLECTION = "generic_fields";

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private MongoGenericFieldDbAdapter underTest;

  private final TenantId tenantId = new TenantId("flowers");
  private final FieldName vipName = new FieldName("vip");

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
    var expectedId = new Document("tenantId", "flowers")
        .append("name", "vip");
    var expectedDocument = new Document("_id", expectedId)
        .append("type", "BOOLEAN");
    var expectedJson = expectedDocument.toJson();

    underTest.insert(inputField);

    var storedDocuments = mongoTemplate.findAll(Document.class, GENERIC_FIELDS_COLLECTION);
    assertThat(storedDocuments).singleElement().extracting(Document::toJson).isEqualTo(expectedJson);
  }

  @Test
  void shouldRejectDuplicateFieldName() {
    storeFieldDocument("flowers", "vip", "BOOLEAN");
    var inputFieldWithDuplicateName = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.STRING)
        .build();

    var thrown = catchThrowable(() -> underTest.insert(inputFieldWithDuplicateName));

    assertThat(thrown).isInstanceOf(DuplicateFieldNameException.class);
  }

  @Test
  void shouldFindField() {
    storeFieldDocument("flowers", "vip", "BOOLEAN");
    var expectedField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOLEAN)
        .build();

    var resultFields = underTest.findAllByTenant(tenantId);

    assertThat(resultFields).singleElement().usingRecursiveComparison().isEqualTo(expectedField);
  }

  @ParameterizedTest
  @EnumSource(FieldType.class)
  void shouldFindFieldOfEveryType(FieldType fieldType) {
    var storedType = fieldType.name();
    storeFieldDocument("flowers", "vip", storedType);

    var resultFields = underTest.findAllByTenant(tenantId);

    assertThat(resultFields).singleElement().extracting(GenericField::getType).isEqualTo(fieldType);
  }

  @Test
  void shouldFindOnlyFieldsOfGivenTenant() {
    storeFieldDocument("flowers", "vip", "BOOLEAN");
    storeFieldDocument("trees", "vip", "STRING");
    var expectedField = GenericField.builder()
        .tenantId(tenantId)
        .name(vipName)
        .type(FieldType.BOOLEAN)
        .build();

    var resultFields = underTest.findAllByTenant(tenantId);

    assertThat(resultFields).singleElement().usingRecursiveComparison().isEqualTo(expectedField);
  }

  @Test
  void shouldFindTenantFieldsUsingIndex() {
    storeFieldDocument("flowers", "vip", "BOOLEAN");
    storeFieldDocument("trees", "vip", "STRING");
    enableProfiler();
    String expectedPlanSummary = "IXSCAN { _id: 1 }";

    underTest.findAllByTenant(tenantId);

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
    var findOperations = new Document("ns", namespace)
        .append("op", "query");
    return database.getCollection("system.profile")
        .find(findOperations)
        .map(entry -> entry.getString("planSummary"))
        .into(new ArrayList<>());
  }

  private void storeFieldDocument(String tenantId, String name, String type) {
    var storedId = new Document("tenantId", tenantId)
        .append("name", name);
    var storedDocument = new Document("_id", storedId)
        .append("type", type);
    mongoTemplate.insert(storedDocument, GENERIC_FIELDS_COLLECTION);
  }
}
