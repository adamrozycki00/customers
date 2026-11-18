package com.example.customers.infrastructure.db.customer;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.MapUtils.isEmpty;
import static org.apache.commons.collections4.MapUtils.isNotEmpty;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.FieldUpdate;
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
import com.mongodb.client.model.FindOneAndUpdateOptions;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

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
  private static final Bson NOT_DELETED_FILTER = eq(DELETED_AT, null);

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
    var activeCustomerFilter = toActiveCustomerFilter(tenantId, publicId);
    var doc = mongoTemplate.getCollection(COLLECTION)
        .find(activeCustomerFilter)
        .first();

    if (nonNull(doc)) {
      var customer = toDomain(doc);
      return Optional.of(customer);
    }

    return Optional.empty();
  }

  @Override
  public CustomerPage getPage(TenantId tenantId, PageRequest pageRequest) {
    var page = pageRequest.page();
    var size = pageRequest.size();
    var customersOfEarlierPages = (page - 1) * size;
    var tenantIdRangeFilter = toTenantIdRangeFilter(tenantId);
    var activeCustomersOfTenantFilter = and(tenantIdRangeFilter, NOT_DELETED_FILTER);
    var byPublicId = ascending(ID);
    var collection = mongoTemplate.getCollection(COLLECTION);
    var customers = new ArrayList<Customer>();

    collection.find(activeCustomersOfTenantFilter)
        .sort(byPublicId)
        .skip(customersOfEarlierPages)
        .limit(size)
        .map(MongoCustomerDbAdapter::toDomain)
        .into(customers);

    var totalElements = collection.countDocuments(activeCustomersOfTenantFilter);

    return CustomerPage.builder()
        .customers(customers)
        .page(page)
        .size(size)
        .totalElements(totalElements)
        .build();
  }

  @Override
  public Optional<Customer> update(TenantId tenantId, PublicId publicId, CustomerUpdate update) {
    if (hasNoUpdates(update)) {
      return find(tenantId, publicId);
    }

    var activeCustomerFilter = toActiveCustomerFilter(tenantId, publicId);
    var updates = toUpdateBson(update);
    var returnUpdatedDoc = new FindOneAndUpdateOptions().returnDocument(AFTER);
    var doc = mongoTemplate.getCollection(COLLECTION)
        .findOneAndUpdate(activeCustomerFilter, updates, returnUpdatedDoc);

    if (nonNull(doc)) {
      var customer = toDomain(doc);
      return Optional.of(customer);
    }

    return Optional.empty();
  }

  private static boolean hasNoUpdates(CustomerUpdate update) {
    return update.firstName() instanceof FieldUpdate.Keep<FirstName>
        && update.lastName() instanceof FieldUpdate.Keep<LastName>
        && isEmpty(update.genericFieldsToSet())
        && isEmpty(update.genericFieldsToClear());
  }

  private static Bson toUpdateBson(CustomerUpdate update) {
    var updates = new ArrayList<Bson>();
    addNameUpdate(updates, FIRST_NAME, update.firstName(), FirstName::value);
    addNameUpdate(updates, LAST_NAME, update.lastName(), LastName::value);

    update.genericFieldsToSet().forEach((name, value) -> {
      var path = toGenericFieldPath(name);
      var bsonValue = toBsonValue(value);
      var setGenericField = set(path, bsonValue);
      updates.add(setGenericField);
    });

    update.genericFieldsToClear().forEach(name -> {
      var path = toGenericFieldPath(name);
      var unsetGenericField = unset(path);
      updates.add(unsetGenericField);
    });

    return combine(updates);
  }

  private static String toGenericFieldPath(FieldName name) {
    return GENERIC_FIELDS + "." + name.value();
  }

  private static <T> void addNameUpdate(List<Bson> updates, String fieldName, FieldUpdate<T> nameUpdate,
                                        Function<T, String> toRawName) {
    switch (nameUpdate) {
      case FieldUpdate.Replace<T> replace -> {
        var rawName = toRawName.apply(replace.value());
        var setName = set(fieldName, rawName);
        updates.add(setName);
      }
      case FieldUpdate.Clear<T> _ -> {
        var unsetName = unset(fieldName);
        updates.add(unsetName);
      }
      case FieldUpdate.Keep<T> _ -> {
      }
    }
  }

  @Override
  public void delete(TenantId tenantId, PublicId publicId) {
  }

  private static Bson toActiveCustomerFilter(TenantId tenantId, PublicId publicId) {
    var id = toId(tenantId, publicId);
    var idFilter = eq(ID, id);
    return and(idFilter, NOT_DELETED_FILTER);
  }

  private static Bson toTenantIdRangeFilter(TenantId tenantId) {
    var rawTenantId = tenantId.value();
    var lowestPublicId = new MinKey();
    var highestPublicId = new MaxKey();
    var firstIdOfTenant = new Document(TENANT_ID, rawTenantId)
        .append(PUBLIC_ID, lowestPublicId);
    var lastIdOfTenant = new Document(TENANT_ID, rawTenantId)
        .append(PUBLIC_ID, highestPublicId);
    var fromFirstIdOfTenantFilter = gte(ID, firstIdOfTenant);
    var toLastIdOfTenantFilter = lte(ID, lastIdOfTenant);
    return and(fromFirstIdOfTenantFilter, toLastIdOfTenantFilter);
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
