package com.example.customers.infrastructure.db.customer;

import com.example.customers.TestcontainersConfiguration;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import com.example.customers.domain.tenant.TenantId;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
      var vipValue = new BooleanValue(true);
      var noteValue = new StringValue("prefers tulips");
      var scoreValue = new IntegerValue(42);
      var since = Instant.parse("2024-03-01T10:00:00Z");
      var sinceValue = new DatetimeValue(since);
      Map<FieldName, GenericFieldValue> inputGenericFields = Map.of(
          vipName, vipValue,
          noteName, noteValue,
          scoreName, scoreValue,
          sinceName, sinceValue);
      var inputCustomer = Customer.builder()
          .tenantId(tenantId)
          .publicId(publicId)
          .firstName(firstName)
          .lastName(lastName)
          .genericFields(inputGenericFields)
          .build();
      var expectedSince = Date.from(since);
      var expectedGenericFields = new Document("vip", true)
          .append("note", "prefers tulips")
          .append("score", 42L)
          .append("since", expectedSince);

      underTest.insert(inputCustomer);

      var storedDoc = mongoTemplate.getCollection(CUSTOMERS_COLLECTION).find().first();
      assertThat(storedDoc).isNotNull();
      assertThat(storedDoc.get("genericFields")).isEqualTo(expectedGenericFields);
    }
  }
}
