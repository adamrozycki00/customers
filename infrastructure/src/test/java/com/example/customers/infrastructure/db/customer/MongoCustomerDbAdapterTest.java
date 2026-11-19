package com.example.customers.infrastructure.db.customer;

import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.TestcontainersConfiguration;
import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.FieldUpdate;
import com.example.customers.application.port.PageRequest;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.CustomerId;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import com.example.customers.domain.tenant.TenantId;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

@DataMongoTest
@Import({TestcontainersConfiguration.class, MongoCustomerDbAdapterTest.FixedClockConfiguration.class,
    MongoCustomerDbAdapter.class})
class MongoCustomerDbAdapterTest {

  private static final String CUSTOMERS_COLLECTION = "customers";
  private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");
  private static final Instant EARLIER_DELETED_AT = Instant.parse("2026-01-15T09:30:00Z");

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private MongoCustomerDbAdapter underTest;

  private final TenantId tenantId = new TenantId("FLOWERS");
  private final CustomerId id = new CustomerId("CUST-1");
  private final FirstName firstName = new FirstName("Anna");
  private final LastName lastName = new LastName("Smith");
  private final FieldName vipName = new FieldName("vip");
  private final FieldName noteName = new FieldName("note");
  private final FieldName scoreName = new FieldName("score");
  private final FieldName sinceName = new FieldName("since");

  @BeforeEach
  void clearCollection() {
    mongoTemplate.dropCollection(CUSTOMERS_COLLECTION);
  }

  @AfterEach
  void disableProfiler() {
    var profileNothing = new Document("profile", 0);
    mongoTemplate.getDb().runCommand(profileNothing);
  }

  @Nested
  class Insert {

    @Test
    void shouldInsertCustomer() {
      var inputCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .firstName(firstName)
          .lastName(lastName)
          .build();
      var expectedId = new Document("tenantId", "FLOWERS")
          .append("id", "CUST-1");
      var expectedDoc = new Document("_id", expectedId)
          .append("firstName", "Anna")
          .append("lastName", "Smith");
      var expectedJson = expectedDoc.toJson();

      underTest.insert(inputCustomer);

      var storedDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      assertThat(storedDocs).singleElement().extracting(Document::toJson).isEqualTo(expectedJson);
    }

    @Test
    void shouldStoreGenericValuesAsNativeTypes() {
      // given a customer with one generic value of each type
      var inputBoolean = new BooleanValue(true);
      var inputString = new StringValue("prefers tulips");
      var inputInteger = new IntegerValue(42);
      var instant = Instant.parse("2024-03-01T10:00:00Z");
      var inputDatetime = new DatetimeValue(instant);
      Map<FieldName, GenericFieldValue> inputGenericFields = Map.of(
          vipName, inputBoolean,
          noteName, inputString,
          scoreName, inputInteger,
          sinceName, inputDatetime);
      var inputCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .firstName(firstName)
          .lastName(lastName)
          .genericFields(inputGenericFields)
          .build();
      var expectedDate = Date.from(instant);
      var expectedGenericFields = new Document("vip", true)
          .append("note", "prefers tulips")
          .append("score", 42L)
          .append("since", expectedDate);

      underTest.insert(inputCustomer);

      var storedDoc = mongoTemplate.getCollection(CUSTOMERS_COLLECTION).find().first();
      assertThat(storedDoc).isNotNull();
      assertThat(storedDoc.get("genericFields")).isEqualTo(expectedGenericFields);
    }

    @Test
    void shouldLeaveOutMissingOptionalFields() {
      var inputCustomerWithoutNames = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .build();
      var expectedId = new Document("tenantId", "FLOWERS")
          .append("id", "CUST-1");
      var expectedDoc = new Document("_id", expectedId);
      var expectedJson = expectedDoc.toJson();

      underTest.insert(inputCustomerWithoutNames);

      var storedDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      assertThat(storedDocs).singleElement().extracting(Document::toJson).isEqualTo(expectedJson);
    }

    @Test
    void shouldRejectDuplicateId() {
      storeCustomerDoc("FLOWERS", "CUST-1");
      var inputCustomerWithDuplicateId = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .firstName(firstName)
          .build();

      var thrown = catchThrowable(() -> underTest.insert(inputCustomerWithDuplicateId));

      assertThat(thrown).isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldRejectInsertingDeletedCustomer() {
      storeDeletedCustomerDoc("FLOWERS", "CUST-1");
      var inputCustomerReusingId = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .firstName(firstName)
          .build();

      var thrown = catchThrowable(() -> underTest.insert(inputCustomerReusingId));

      assertThat(thrown).isInstanceOf(ConflictException.class);
    }
  }

  @Nested
  class Find {

    @Test
    void shouldFindCustomer() {
      storeCustomerDocWithNames();
      var expectedCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .firstName(firstName)
          .lastName(lastName)
          .build();

      var resultCustomer = underTest.find(tenantId, id);

      assertThat(resultCustomer).get().usingRecursiveComparison().isEqualTo(expectedCustomer);
    }

    @Test
    void shouldFindGenericValuesOfEveryType() {
      // given a stored customer with one generic value of each type
      var instant = Instant.parse("2024-03-01T10:00:00Z");
      var storedDate = Date.from(instant);
      var storedGenericFields = new Document("vip", true)
          .append("note", "prefers tulips")
          .append("score", 42L)
          .append("since", storedDate);
      var storedId = new Document("tenantId", "FLOWERS")
          .append("id", "CUST-1");
      var storedDoc = new Document("_id", storedId)
          .append("firstName", "Anna")
          .append("lastName", "Smith")
          .append("genericFields", storedGenericFields);
      mongoTemplate.insert(storedDoc, CUSTOMERS_COLLECTION);
      var expectedBoolean = new BooleanValue(true);
      var expectedString = new StringValue("prefers tulips");
      var expectedInteger = new IntegerValue(42);
      var expectedDatetime = new DatetimeValue(instant);
      var expectedGenericFields = Map.of(
          vipName, expectedBoolean,
          noteName, expectedString,
          scoreName, expectedInteger,
          sinceName, expectedDatetime);

      var resultCustomer = underTest.find(tenantId, id);

      assertThat(resultCustomer).get().extracting(Customer::getGenericFields).isEqualTo(expectedGenericFields);
    }

    @Test
    void shouldFindCustomerWithoutOptionalFields() {
      var storedId = new Document("tenantId", "FLOWERS")
          .append("id", "CUST-1");
      var storedDocWithoutOptionalFields = new Document("_id", storedId);
      mongoTemplate.insert(storedDocWithoutOptionalFields, CUSTOMERS_COLLECTION);
      var expectedCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .build();

      var resultCustomer = underTest.find(tenantId, id);

      assertThat(resultCustomer).get().usingRecursiveComparison().isEqualTo(expectedCustomer);
    }

    @Test
    void shouldNotFindDeletedCustomer() {
      storeDeletedCustomerDoc("FLOWERS", "CUST-1");

      var resultCustomer = underTest.find(tenantId, id);

      assertThat(resultCustomer).isEmpty();
    }

    @Test
    void shouldNotFindCustomerOfOtherTenant() {
      var storedIdWithSameIdOfOtherTenant = new Document("tenantId", "TREES")
          .append("id", id.value());
      var storedDocOfOtherTenant = new Document("_id", storedIdWithSameIdOfOtherTenant)
          .append("firstName", "Anna");
      mongoTemplate.insert(storedDocOfOtherTenant, CUSTOMERS_COLLECTION);

      var resultCustomer = underTest.find(tenantId, id);

      assertThat(resultCustomer).isEmpty();
    }
  }

  @Nested
  class GetPage {

    @Test
    void shouldGetPageOrderedById() {
      // given three customers stored out of order
      storeCustomerDoc("FLOWERS", "CUST-3");
      storeCustomerDoc("FLOWERS", "CUST-1");
      storeCustomerDoc("FLOWERS", "CUST-2");
      var inputPageRequest = new PageRequest(1, 10);
      var expectedIds = List.of("CUST-1", "CUST-2", "CUST-3");

      var resultPage = underTest.getPage(tenantId, inputPageRequest);

      assertThat(resultPage.customers())
          .extracting(Customer::getId)
          .extracting(CustomerId::value)
          .containsExactlyElementsOf(expectedIds);
    }

    @Test
    void shouldGetRequestedPage() {
      // given five customers and a request for page 2 of size 2
      storeCustomerDoc("FLOWERS", "CUST-1");
      storeCustomerDoc("FLOWERS", "CUST-2");
      storeCustomerDoc("FLOWERS", "CUST-3");
      storeCustomerDoc("FLOWERS", "CUST-4");
      storeCustomerDoc("FLOWERS", "CUST-5");
      var inputSecondPageOfTwo = new PageRequest(2, 2);
      var thirdId = new CustomerId("CUST-3");
      var fourthId = new CustomerId("CUST-4");
      var expectedThirdCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(thirdId)
          .build();
      var expectedFourthCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(fourthId)
          .build();
      var expectedCustomers = List.of(expectedThirdCustomer, expectedFourthCustomer);
      var expectedPage = CustomerPage.builder()
          .customers(expectedCustomers)
          .page(2)
          .size(2)
          .totalElements(5)
          .build();

      var resultPage = underTest.getPage(tenantId, inputSecondPageOfTwo);

      assertThat(resultPage).usingRecursiveComparison().isEqualTo(expectedPage);
    }

    @Test
    void shouldLeaveOutDeletedCustomers() {
      // given three customers, of which CUST-2 is deleted
      storeCustomerDoc("FLOWERS", "CUST-1");
      storeDeletedCustomerDoc("FLOWERS", "CUST-2");
      storeCustomerDoc("FLOWERS", "CUST-3");
      var inputPageRequest = new PageRequest(1, 10);
      var firstId = new CustomerId("CUST-1");
      var thirdId = new CustomerId("CUST-3");
      var expectedFirstCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(firstId)
          .build();
      var expectedThirdCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(thirdId)
          .build();
      var expectedCustomers = List.of(expectedFirstCustomer, expectedThirdCustomer);
      var expectedPage = CustomerPage.builder()
          .customers(expectedCustomers)
          .page(1)
          .size(10)
          .totalElements(2)
          .build();

      var resultPage = underTest.getPage(tenantId, inputPageRequest);

      assertThat(resultPage).usingRecursiveComparison().isEqualTo(expectedPage);
    }

    @Test
    void shouldGetOnlyCustomersOfGivenTenant() {
      // given customers of flowers and trees
      storeCustomerDoc("FLOWERS", "CUST-1");
      storeCustomerDoc("TREES", "CUST-2");
      storeCustomerDoc("FLOWERS", "CUST-3");
      var inputPageRequest = new PageRequest(1, 10);
      var firstId = new CustomerId("CUST-1");
      var thirdId = new CustomerId("CUST-3");
      var expectedFirstCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(firstId)
          .build();
      var expectedThirdCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(thirdId)
          .build();
      var expectedCustomers = List.of(expectedFirstCustomer, expectedThirdCustomer);
      var expectedPage = CustomerPage.builder()
          .customers(expectedCustomers)
          .page(1)
          .size(10)
          .totalElements(2)
          .build();

      var resultPage = underTest.getPage(tenantId, inputPageRequest);

      assertThat(resultPage).usingRecursiveComparison().isEqualTo(expectedPage);
    }

    @Test
    void shouldGetPageUsingIndex() {
      // given customers of two tenants and the profiler on
      storeCustomerDoc("FLOWERS", "CUST-1");
      storeCustomerDoc("TREES", "CUST-2");
      var inputPageRequest = new PageRequest(1, 10);
      enableProfiler();
      String expectedPlanSummary = "IXSCAN { _id: 1 }";

      underTest.getPage(tenantId, inputPageRequest);

      var planSummaries = getPlanSummaries();
      assertThat(planSummaries).hasSize(2).containsOnly(expectedPlanSummary);
    }
  }

  @Nested
  class Update {

    @Test
    void shouldReplaceNames() {
      // given that Anna Smith is stored and an update replacing both names
      storeCustomerDocWithNames();
      var newFirstName = new FirstName("Maria");
      var newLastName = new LastName("Nowak");
      var replaceFirstName = new FieldUpdate.Replace<>(newFirstName);
      var replaceLastName = new FieldUpdate.Replace<>(newLastName);
      Map<FieldName, GenericFieldValue> noGenericFieldsToSet = emptyMap();
      Set<FieldName> noGenericFieldsToClear = emptySet();
      var inputUpdate = CustomerUpdate.builder()
          .firstName(replaceFirstName)
          .lastName(replaceLastName)
          .genericFieldsToSet(noGenericFieldsToSet)
          .genericFieldsToClear(noGenericFieldsToClear)
          .build();
      var expectedId = new Document("tenantId", "FLOWERS")
          .append("id", "CUST-1");
      var expectedDoc = new Document("_id", expectedId)
          .append("firstName", "Maria")
          .append("lastName", "Nowak");
      var expectedJson = expectedDoc.toJson();
      var expectedCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .firstName(newFirstName)
          .lastName(newLastName)
          .build();

      var resultCustomer = underTest.update(tenantId, id, inputUpdate);

      var storedDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      assertThat(storedDocs).singleElement().extracting(Document::toJson).isEqualTo(expectedJson);
      assertThat(resultCustomer).get().usingRecursiveComparison().isEqualTo(expectedCustomer);
    }

    @Test
    void shouldLeaveKeptNameUnchanged() {
      // given that Anna Smith is stored and an update replacing only the first name
      storeCustomerDocWithNames();
      var newFirstName = new FirstName("Maria");
      var replaceFirstName = new FieldUpdate.Replace<>(newFirstName);
      var keepLastName = new FieldUpdate.Keep<LastName>();
      Map<FieldName, GenericFieldValue> noGenericFieldsToSet = emptyMap();
      Set<FieldName> noGenericFieldsToClear = emptySet();
      var inputUpdate = CustomerUpdate.builder()
          .firstName(replaceFirstName)
          .lastName(keepLastName)
          .genericFieldsToSet(noGenericFieldsToSet)
          .genericFieldsToClear(noGenericFieldsToClear)
          .build();
      var expectedId = new Document("tenantId", "FLOWERS")
          .append("id", "CUST-1");
      var expectedDoc = new Document("_id", expectedId)
          .append("firstName", "Maria")
          .append("lastName", "Smith");
      var expectedJson = expectedDoc.toJson();
      var expectedCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .firstName(newFirstName)
          .lastName(lastName)
          .build();

      var resultCustomer = underTest.update(tenantId, id, inputUpdate);

      var storedDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      assertThat(storedDocs).singleElement().extracting(Document::toJson).isEqualTo(expectedJson);
      assertThat(resultCustomer).get().usingRecursiveComparison().isEqualTo(expectedCustomer);
    }

    @Test
    void shouldSetGenericFieldsKeepingOthers() {
      // given a customer with vip and note, and an update setting vip and score
      var storedGenericFields = new Document("vip", true)
          .append("note", "prefers tulips");
      var storedId = new Document("tenantId", "FLOWERS")
          .append("id", "CUST-1");
      var storedDoc = new Document("_id", storedId)
          .append("genericFields", storedGenericFields);
      mongoTemplate.insert(storedDoc, CUSTOMERS_COLLECTION);
      var inputBoolean = new BooleanValue(false);
      var inputInteger = new IntegerValue(42);
      Map<FieldName, GenericFieldValue> inputGenericFieldsToSet = Map.of(
          vipName, inputBoolean,
          scoreName, inputInteger);
      var keepFirstName = new FieldUpdate.Keep<FirstName>();
      var keepLastName = new FieldUpdate.Keep<LastName>();
      Set<FieldName> noGenericFieldsToClear = emptySet();
      var inputUpdate = CustomerUpdate.builder()
          .firstName(keepFirstName)
          .lastName(keepLastName)
          .genericFieldsToSet(inputGenericFieldsToSet)
          .genericFieldsToClear(noGenericFieldsToClear)
          .build();
      var expectedStoredGenericFields = new Document("vip", false)
          .append("note", "prefers tulips")
          .append("score", 42L);
      var expectedString = new StringValue("prefers tulips");
      var expectedGenericFields = Map.of(
          vipName, inputBoolean,
          noteName, expectedString,
          scoreName, inputInteger);

      var resultCustomer = underTest.update(tenantId, id, inputUpdate);

      var resultStoredDoc = mongoTemplate.getCollection(CUSTOMERS_COLLECTION).find().first();
      assertThat(resultStoredDoc).isNotNull();
      assertThat(resultStoredDoc.get("genericFields")).isEqualTo(expectedStoredGenericFields);
      assertThat(resultCustomer).get().extracting(Customer::getGenericFields).isEqualTo(expectedGenericFields);
    }

    @Test
    void shouldClearGenericFields() {
      // given a customer with vip and note, and an update clearing vip
      var storedGenericFields = new Document("vip", true)
          .append("note", "prefers tulips");
      var storedId = new Document("tenantId", "FLOWERS")
          .append("id", "CUST-1");
      var storedDoc = new Document("_id", storedId)
          .append("genericFields", storedGenericFields);
      mongoTemplate.insert(storedDoc, CUSTOMERS_COLLECTION);
      var keepFirstName = new FieldUpdate.Keep<FirstName>();
      var keepLastName = new FieldUpdate.Keep<LastName>();
      Map<FieldName, GenericFieldValue> noGenericFieldsToSet = emptyMap();
      Set<FieldName> inputGenericFieldsToClear = Set.of(vipName);
      var inputUpdate = CustomerUpdate.builder()
          .firstName(keepFirstName)
          .lastName(keepLastName)
          .genericFieldsToSet(noGenericFieldsToSet)
          .genericFieldsToClear(inputGenericFieldsToClear)
          .build();
      var expectedStoredGenericFields = new Document("note", "prefers tulips");
      var expectedString = new StringValue("prefers tulips");
      var expectedGenericFields = Map.of(noteName, expectedString);

      var resultCustomer = underTest.update(tenantId, id, inputUpdate);

      var resultStoredDoc = mongoTemplate.getCollection(CUSTOMERS_COLLECTION).find().first();
      assertThat(resultStoredDoc).isNotNull();
      assertThat(resultStoredDoc.get("genericFields")).isEqualTo(expectedStoredGenericFields);
      assertThat(resultCustomer).get().extracting(Customer::getGenericFields).isEqualTo(expectedGenericFields);
    }

    @Test
    void shouldNotUpdateDeletedCustomer() {
      // given a deleted customer and an update replacing the first name
      storeDeletedCustomerDoc("FLOWERS", "CUST-1");
      var newFirstName = new FirstName("Maria");
      var replaceFirstName = new FieldUpdate.Replace<>(newFirstName);
      var keepLastName = new FieldUpdate.Keep<LastName>();
      Map<FieldName, GenericFieldValue> noGenericFieldsToSet = emptyMap();
      Set<FieldName> noGenericFieldsToClear = emptySet();
      var inputUpdate = CustomerUpdate.builder()
          .firstName(replaceFirstName)
          .lastName(keepLastName)
          .genericFieldsToSet(noGenericFieldsToSet)
          .genericFieldsToClear(noGenericFieldsToClear)
          .build();
      var expectedStoredDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);

      var resultCustomer = underTest.update(tenantId, id, inputUpdate);

      var resultStoredDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      assertThat(resultCustomer).isEmpty();
      assertThat(resultStoredDocs).isEqualTo(expectedStoredDocs);
    }

    @Test
    void shouldNotCreateMissingCustomerOnUpdate() {
      // given no stored customer and an update replacing the first name
      var newFirstName = new FirstName("Maria");
      var replaceFirstName = new FieldUpdate.Replace<>(newFirstName);
      var keepLastName = new FieldUpdate.Keep<LastName>();
      Map<FieldName, GenericFieldValue> noGenericFieldsToSet = emptyMap();
      Set<FieldName> noGenericFieldsToClear = emptySet();
      var inputUpdate = CustomerUpdate.builder()
          .firstName(replaceFirstName)
          .lastName(keepLastName)
          .genericFieldsToSet(noGenericFieldsToSet)
          .genericFieldsToClear(noGenericFieldsToClear)
          .build();

      var resultCustomer = underTest.update(tenantId, id, inputUpdate);

      var resultStoredDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      assertThat(resultCustomer).isEmpty();
      assertThat(resultStoredDocs).isEmpty();
    }

    @Test
    void shouldReturnCustomerUnchangedForEmptyUpdate() {
      // given that Anna Smith is stored and an empty update
      storeCustomerDocWithNames();
      var keepFirstName = new FieldUpdate.Keep<FirstName>();
      var keepLastName = new FieldUpdate.Keep<LastName>();
      Map<FieldName, GenericFieldValue> noGenericFieldsToSet = emptyMap();
      Set<FieldName> noGenericFieldsToClear = emptySet();
      var inputEmptyUpdate = CustomerUpdate.builder()
          .firstName(keepFirstName)
          .lastName(keepLastName)
          .genericFieldsToSet(noGenericFieldsToSet)
          .genericFieldsToClear(noGenericFieldsToClear)
          .build();
      var expectedStoredDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      var expectedCustomer = Customer.builder()
          .tenantId(tenantId)
          .id(id)
          .firstName(firstName)
          .lastName(lastName)
          .build();

      var resultCustomer = underTest.update(tenantId, id, inputEmptyUpdate);

      var resultStoredDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      assertThat(resultCustomer).get().usingRecursiveComparison().isEqualTo(expectedCustomer);
      assertThat(resultStoredDocs).isEqualTo(expectedStoredDocs);
    }
  }

  @Nested
  class Delete {

    @Test
    void shouldSetDeletedAt() {
      storeCustomerDocWithNames();
      var expectedDeletedAt = Date.from(NOW);

      underTest.delete(tenantId, id);

      var resultStoredDoc = mongoTemplate.getCollection(CUSTOMERS_COLLECTION).find().first();
      assertThat(resultStoredDoc).isNotNull();
      assertThat(resultStoredDoc.get("deletedAt")).isEqualTo(expectedDeletedAt);
    }

    @Test
    void shouldKeepFirstDeletedAt() {
      storeDeletedCustomerDoc("FLOWERS", "CUST-1");
      var expectedDeletedAt = Date.from(EARLIER_DELETED_AT);

      underTest.delete(tenantId, id);

      var resultStoredDoc = mongoTemplate.getCollection(CUSTOMERS_COLLECTION).find().first();
      assertThat(resultStoredDoc).isNotNull();
      assertThat(resultStoredDoc.get("deletedAt")).isEqualTo(expectedDeletedAt);
    }
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class FixedClockConfiguration {

    @Bean
    Clock fixedClock() {
      return Clock.fixed(NOW, UTC);
    }
  }

  private void enableProfiler() {
    var profileAllOperations = new Document("profile", 2);
    mongoTemplate.getDb().runCommand(profileAllOperations);
  }

  private List<String> getPlanSummaries() {
    var database = mongoTemplate.getDb();
    var namespace = database.getName() + "." + CUSTOMERS_COLLECTION;
    var customersOperationsFilter = new Document("ns", namespace);
    return database.getCollection("system.profile")
        .find(customersOperationsFilter)
        .map(entry -> entry.getString("planSummary"))
        .into(new ArrayList<>());
  }

  private void storeCustomerDoc(String tenantId, String id) {
    var storedId = new Document("tenantId", tenantId)
        .append("id", id);
    var storedDoc = new Document("_id", storedId);
    mongoTemplate.insert(storedDoc, CUSTOMERS_COLLECTION);
  }

  private void storeCustomerDocWithNames() {
    var storedId = new Document("tenantId", "FLOWERS")
        .append("id", "CUST-1");
    var storedDoc = new Document("_id", storedId)
        .append("firstName", "Anna")
        .append("lastName", "Smith");
    mongoTemplate.insert(storedDoc, CUSTOMERS_COLLECTION);
  }

  private void storeDeletedCustomerDoc(String tenantId, String id) {
    var storedId = new Document("tenantId", tenantId)
        .append("id", id);
    var storedDate = Date.from(EARLIER_DELETED_AT);
    var storedDeletedDoc = new Document("_id", storedId)
        .append("deletedAt", storedDate);
    mongoTemplate.insert(storedDeletedDoc, CUSTOMERS_COLLECTION);
  }
}
