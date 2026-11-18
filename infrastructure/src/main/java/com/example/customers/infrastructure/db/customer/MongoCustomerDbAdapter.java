package com.example.customers.infrastructure.db.customer;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.PageRequest;
import com.example.customers.application.port.out.CustomerPort;
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
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

@Component
@RequiredArgsConstructor
public class MongoCustomerDbAdapter implements CustomerPort {

  private static final String COLLECTION = "customers";
  private static final String ID = "_id";
  private static final String TENANT_ID = "tenantId";
  private static final String PUBLIC_ID = "publicId";
  private static final String FIRST_NAME = "firstName";
  private static final String LAST_NAME = "lastName";
  private static final String GENERIC_FIELDS = "genericFields";
  private static final String DELETED_AT = "deletedAt";

  private final MongoTemplate mongoTemplate;

  @Override
  public Customer insert(Customer customer) {
    var doc = toDoc(customer);

    try {
      mongoTemplate.insert(doc, COLLECTION);
    } catch (DuplicateKeyException e) {
      throw new DuplicatePublicIdException();
    }

    return customer;
  }

  @Override
  public Optional<Customer> find(TenantId tenantId, PublicId publicId) {
    var id = toId(tenantId, publicId);
    var idFilter = eq(ID, id);
    var notDeletedFilter = eq(DELETED_AT, null);
    var activeCustomerFilter = and(idFilter, notDeletedFilter);
    var doc = mongoTemplate.getCollection(COLLECTION)
        .find(activeCustomerFilter)
        .first();
    return Optional.ofNullable(doc)
        .map(MongoCustomerDbAdapter::toDomain);
  }

  @Override
  public CustomerPage getPage(TenantId tenantId, PageRequest pageRequest) {
    return null;
  }

  @Override
  public Optional<Customer> update(TenantId tenantId, PublicId publicId, CustomerUpdate update) {
    return Optional.empty();
  }

  @Override
  public void delete(TenantId tenantId, PublicId publicId) {
  }

  private static Document toId(TenantId tenantId, PublicId publicId) {
    return new Document(TENANT_ID, tenantId.value())
        .append(PUBLIC_ID, publicId.value());
  }

  private static Customer toDomain(Document doc) {
    var id = doc.get(ID, Document.class);
    var rawTenantId = id.getString(TENANT_ID);
    var rawPublicId = id.getString(PUBLIC_ID);
    var rawFirstName = doc.getString(FIRST_NAME);
    var rawLastName = doc.getString(LAST_NAME);
    var genericFieldsDoc = doc.get(GENERIC_FIELDS, Document.class);
    var tenantId = new TenantId(rawTenantId);
    var publicId = new PublicId(rawPublicId);
    var firstName = nonNull(rawFirstName) ? new FirstName(rawFirstName) : null;
    var lastName = nonNull(rawLastName) ? new LastName(rawLastName) : null;
    var genericFields = toGenericFields(genericFieldsDoc);
    return Customer.builder()
        .tenantId(tenantId)
        .publicId(publicId)
        .firstName(firstName)
        .lastName(lastName)
        .genericFields(genericFields)
        .build();
  }

  private static Map<FieldName, GenericFieldValue> toGenericFields(Document genericFieldsDoc) {
    Map<FieldName, GenericFieldValue> genericFields = new LinkedHashMap<>();

    if (nonNull(genericFieldsDoc)) {
      genericFieldsDoc.forEach((rawName, rawValue) -> {
        var name = new FieldName(rawName);
        var value = toGenericFieldValue(rawValue);
        genericFields.put(name, value);
      });
    }

    return genericFields;
  }

  private static GenericFieldValue toGenericFieldValue(Object rawValue) {
    return switch (rawValue) {
      case Boolean rawBoolean -> new BooleanValue(rawBoolean);
      case String rawString -> new StringValue(rawString);
      case Long rawLong -> new IntegerValue(rawLong);
      case Date rawDate -> toDatetimeValue(rawDate);
      default -> throw new IllegalStateException("Unsupported generic field value type: " + rawValue.getClass());
    };
  }

  private static DatetimeValue toDatetimeValue(Date rawDate) {
    var instant = rawDate.toInstant();
    return new DatetimeValue(instant);
  }

  private static Document toDoc(Customer customer) {
    var tenantId = customer.getTenantId();
    var publicId = customer.getPublicId();
    var firstName = customer.getFirstName();
    var lastName = customer.getLastName();
    var genericFields = customer.getGenericFields();
    var id = toId(tenantId, publicId);
    var doc = new Document(ID, id);

    if (nonNull(firstName)) {
      doc.append(FIRST_NAME, firstName.value());
    }

    if (nonNull(lastName)) {
      doc.append(LAST_NAME, lastName.value());
    }

    if (isNotEmpty(genericFields)) {
      var genericFieldsDoc = toDoc(genericFields);
      doc.append(GENERIC_FIELDS, genericFieldsDoc);
    }

    return doc;
  }

  private static Document toDoc(Map<FieldName, GenericFieldValue> genericFields) {
    var doc = new Document();
    genericFields.forEach((name, value) -> {
      var bsonValue = toBsonValue(value);
      doc.append(name.value(), bsonValue);
    });
    return doc;
  }

  private static Object toBsonValue(GenericFieldValue value) {
    return switch (value) {
      case BooleanValue booleanValue -> booleanValue.value();
      case StringValue stringValue -> stringValue.value();
      case IntegerValue integerValue -> integerValue.value();
      case DatetimeValue datetimeValue -> Date.from(datetimeValue.value());
    };
  }
}
