package com.example.customers.infrastructure.db.customer;

import com.example.customers.TestcontainersConfiguration;
import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.PageRequest;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.customer.exception.DuplicatePublicIdException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import com.example.customers.domain.tenant.TenantId;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DataMongoTest
@Import({TestcontainersConfiguration.class, MongoCustomerDbAdapter.class})
class MongoCustomerDbAdapterTest {

  private static final String CUSTOMERS_COLLECTION = "customers";

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private MongoCustomerDbAdapter underTest;

  private final TenantId tenantId = new TenantId("flowers");
  private final PublicId publicId = new PublicId("CUST-1");
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
          .publicId(publicId)
          .firstName(firstName)
          .lastName(lastName)
          .build();
      var expectedId = new Document("tenantId", "flowers")
          .append("publicId", "CUST-1");
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
          .publicId(publicId)
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
          .publicId(publicId)
          .build();
      var expectedId = new Document("tenantId", "flowers")
          .append("publicId", "CUST-1");
      var expectedDoc = new Document("_id", expectedId);
      var expectedJson = expectedDoc.toJson();

      underTest.insert(inputCustomerWithoutNames);

      var storedDocs = mongoTemplate.findAll(Document.class, CUSTOMERS_COLLECTION);
      assertThat(storedDocs).singleElement().extracting(Document::toJson).isEqualTo(expectedJson);
    }

    @Test
    void shouldRejectDuplicatePublicId() {
      storeCustomerDoc("flowers", "CUST-1");
      var inputCustomerWithDuplicatePublicId = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .firstName(firstName)
          .build();

      var thrown = catchThrowable(() -> underTest.insert(inputCustomerWithDuplicatePublicId));

      assertThat(thrown).isInstanceOf(DuplicatePublicIdException.class);
    }

    @Test
    void shouldRejectInsertingDeletedCustomer() {
      storeDeletedCustomerDoc("flowers", "CUST-1");
      var inputCustomerReusingPublicId = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .firstName(firstName)
          .build();

      var thrown = catchThrowable(() -> underTest.insert(inputCustomerReusingPublicId));

      assertThat(thrown).isInstanceOf(DuplicatePublicIdException.class);
    }
  }

  @Nested
  class Find {

    @Test
    void shouldFindCustomer() {
      var storedId = new Document("tenantId", "flowers")
          .append("publicId", "CUST-1");
      var storedDoc = new Document("_id", storedId)
          .append("firstName", "Anna")
          .append("lastName", "Smith");
      mongoTemplate.insert(storedDoc, CUSTOMERS_COLLECTION);
      var expectedCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .firstName(firstName)
          .lastName(lastName)
          .build();

      var resultCustomer = underTest.find(tenantId, publicId);

      assertThat(resultCustomer).get().usingRecursiveComparison().isEqualTo(expectedCustomer);
    }

    @Test
    void shouldFindGenericValuesOfEveryType() {
      var instant = Instant.parse("2024-03-01T10:00:00Z");
      var storedDate = Date.from(instant);
      var storedGenericFields = new Document("vip", true)
          .append("note", "prefers tulips")
          .append("score", 42L)
          .append("since", storedDate);
      var storedId = new Document("tenantId", "flowers")
          .append("publicId", "CUST-1");
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

      var resultCustomer = underTest.find(tenantId, publicId);

      assertThat(resultCustomer).get().extracting(Customer::getGenericFields).isEqualTo(expectedGenericFields);
    }

    @Test
    void shouldFindCustomerWithoutOptionalFields() {
      var storedId = new Document("tenantId", "flowers")
          .append("publicId", "CUST-1");
      var storedDocWithoutOptionalFields = new Document("_id", storedId);
      mongoTemplate.insert(storedDocWithoutOptionalFields, CUSTOMERS_COLLECTION);
      var expectedCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .build();

      var resultCustomer = underTest.find(tenantId, publicId);

      assertThat(resultCustomer).get().usingRecursiveComparison().isEqualTo(expectedCustomer);
    }

    @Test
    void shouldNotFindDeletedCustomer() {
      storeDeletedCustomerDoc("flowers", "CUST-1");

      var resultCustomer = underTest.find(tenantId, publicId);

      assertThat(resultCustomer).isEmpty();
    }

    @Test
    void shouldNotFindCustomerOfOtherTenant() {
      var storedIdWithSamePublicIdOfOtherTenant = new Document("tenantId", "trees")
          .append("publicId", publicId.value());
      var storedDocOfOtherTenant = new Document("_id", storedIdWithSamePublicIdOfOtherTenant)
          .append("firstName", "Anna");
      mongoTemplate.insert(storedDocOfOtherTenant, CUSTOMERS_COLLECTION);

      var resultCustomer = underTest.find(tenantId, publicId);

      assertThat(resultCustomer).isEmpty();
    }
  }

  @Nested
  class GetPage {

    @Test
    void shouldGetPageOrderedByPublicId() {
      storeCustomerDoc("flowers", "CUST-3");
      storeCustomerDoc("flowers", "CUST-1");
      storeCustomerDoc("flowers", "CUST-2");
      var inputPageRequest = new PageRequest(1, 10);
      var expectedPublicIds = List.of("CUST-1", "CUST-2", "CUST-3");

      var resultPage = underTest.getPage(tenantId, inputPageRequest);

      assertThat(resultPage.customers())
          .extracting(Customer::getPublicId)
          .extracting(PublicId::value)
          .containsExactlyElementsOf(expectedPublicIds);
    }

    @Test
    void shouldGetRequestedPage() {
      storeCustomerDoc("flowers", "CUST-1");
      storeCustomerDoc("flowers", "CUST-2");
      storeCustomerDoc("flowers", "CUST-3");
      storeCustomerDoc("flowers", "CUST-4");
      storeCustomerDoc("flowers", "CUST-5");
      var inputSecondPageOfTwo = new PageRequest(2, 2);
      var thirdPublicId = new PublicId("CUST-3");
      var fourthPublicId = new PublicId("CUST-4");
      var expectedThirdCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(thirdPublicId)
          .build();
      var expectedFourthCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(fourthPublicId)
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
      storeCustomerDoc("flowers", "CUST-1");
      storeDeletedCustomerDoc("flowers", "CUST-2");
      storeCustomerDoc("flowers", "CUST-3");
      var inputPageRequest = new PageRequest(1, 10);
      var firstPublicId = new PublicId("CUST-1");
      var thirdPublicId = new PublicId("CUST-3");
      var expectedFirstCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(firstPublicId)
          .build();
      var expectedThirdCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(thirdPublicId)
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
      storeCustomerDoc("flowers", "CUST-1");
      storeCustomerDoc("trees", "CUST-2");
      storeCustomerDoc("flowers", "CUST-3");
      var inputPageRequest = new PageRequest(1, 10);
      var firstPublicId = new PublicId("CUST-1");
      var thirdPublicId = new PublicId("CUST-3");
      var expectedFirstCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(firstPublicId)
          .build();
      var expectedThirdCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(thirdPublicId)
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
      storeCustomerDoc("flowers", "CUST-1");
      storeCustomerDoc("trees", "CUST-2");
      var inputPageRequest = new PageRequest(1, 10);
      enableProfiler();
      String expectedPlanSummary = "IXSCAN { _id: 1 }";

      underTest.getPage(tenantId, inputPageRequest);

      var planSummaries = getPlanSummaries();
      assertThat(planSummaries).hasSize(2).containsOnly(expectedPlanSummary);
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

  private void storeCustomerDoc(String tenantId, String publicId) {
    var storedId = new Document("tenantId", tenantId)
        .append("publicId", publicId);
    var storedDoc = new Document("_id", storedId);
    mongoTemplate.insert(storedDoc, CUSTOMERS_COLLECTION);
  }

  private void storeDeletedCustomerDoc(String tenantId, String publicId) {
    var storedId = new Document("tenantId", tenantId)
        .append("publicId", publicId);
    var instant = Instant.parse("2026-01-15T09:30:00Z");
    var storedDate = Date.from(instant);
    var storedDeletedDoc = new Document("_id", storedId)
        .append("deletedAt", storedDate);
    mongoTemplate.insert(storedDeletedDoc, CUSTOMERS_COLLECTION);
  }
}
