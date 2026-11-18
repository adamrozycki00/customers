package com.example.customers.infrastructure.db.customer;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.PageRequest;
import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import com.example.customers.domain.tenant.TenantId;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

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

  private final MongoTemplate mongoTemplate;

  @Override
  public Customer insert(Customer customer) {
    var doc = toDoc(customer);
    mongoTemplate.insert(doc, COLLECTION);
    return customer;
  }

  @Override
  public Optional<Customer> find(TenantId tenantId, PublicId publicId) {
    return Optional.empty();
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

  private static Document toDoc(Customer customer) {
    var tenantId = customer.getTenantId();
    var publicId = customer.getPublicId();
    var firstName = customer.getFirstName();
    var lastName = customer.getLastName();
    var genericFields = customer.getGenericFields();
    var id = new Document(TENANT_ID, tenantId.value())
        .append(PUBLIC_ID, publicId.value());
    var doc = new Document(ID, id)
        .append(FIRST_NAME, firstName.value())
        .append(LAST_NAME, lastName.value());

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
