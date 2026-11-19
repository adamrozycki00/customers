package com.example.customers.infrastructure.db.customer;

import static java.util.Objects.nonNull;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.CustomerId;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.BooleanValue;
import com.example.customers.domain.genericfield.value.DatetimeValue;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.genericfield.value.IntegerValue;
import com.example.customers.domain.genericfield.value.StringValue;
import com.example.customers.domain.tenant.TenantId;
import com.example.customers.infrastructure.db.exception.CorruptDataException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.bson.Document;

@NoArgsConstructor(access = PRIVATE)
final class CustomerDocMapper {

  static final String ID = "_id";
  static final String TENANT_ID = "tenantId";
  static final String CUSTOMER_ID = "id";
  static final String FIRST_NAME = "firstName";
  static final String LAST_NAME = "lastName";
  static final String GENERIC_FIELDS = "genericFields";
  static final String DELETED_AT = "deletedAt";

  static Document toId(TenantId tenantId, CustomerId id) {
    return new Document(TENANT_ID, tenantId.value())
        .append(CUSTOMER_ID, id.value());
  }

  static Customer toDomain(Document doc) {
    try {
      return toCustomer(doc);
    } catch (RuntimeException e) {
      var docId = doc.get(ID);
      throw new CorruptDataException("Corrupt customer doc " + docId, e);
    }
  }

  private static Customer toCustomer(Document doc) {
    var idDoc = doc.get(ID, Document.class);
    var rawTenantId = idDoc.getString(TENANT_ID);
    var rawId = idDoc.getString(CUSTOMER_ID);
    var rawFirstName = doc.getString(FIRST_NAME);
    var rawLastName = doc.getString(LAST_NAME);
    var genericFieldsDoc = doc.get(GENERIC_FIELDS, Document.class);
    var tenantId = new TenantId(rawTenantId);
    var id = new CustomerId(rawId);
    var firstName = nonNull(rawFirstName) ? new FirstName(rawFirstName) : null;
    var lastName = nonNull(rawLastName) ? new LastName(rawLastName) : null;
    var genericFields = toGenericFields(genericFieldsDoc);
    return Customer.builder()
        .tenantId(tenantId)
        .id(id)
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

  static Document toDoc(Customer customer) {
    var tenantId = customer.getTenantId();
    var id = customer.getId();
    var firstName = customer.getFirstName();
    var lastName = customer.getLastName();
    var genericFields = customer.getGenericFields();
    var idDoc = toId(tenantId, id);
    var doc = new Document(ID, idDoc);

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

  static Object toBsonValue(GenericFieldValue value) {
    return switch (value) {
      case BooleanValue booleanValue -> booleanValue.value();
      case StringValue stringValue -> stringValue.value();
      case IntegerValue integerValue -> integerValue.value();
      case DatetimeValue datetimeValue -> Date.from(datetimeValue.value());
    };
  }
}
