package com.example.customers.infrastructure.db.genericfield;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;

import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.FieldType;
import com.example.customers.domain.genericfield.GenericField;
import com.example.customers.domain.genericfield.exception.DuplicateFieldNameException;
import com.example.customers.domain.tenant.TenantId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoGenericFieldDbAdapter implements GenericFieldPort {

  private static final String COLLECTION = "genericFields";
  private static final String ID = "_id";
  private static final String TENANT_ID = "tenantId";
  private static final String NAME = "name";
  private static final String TYPE = "type";

  private final MongoTemplate mongoTemplate;

  @Override
  public GenericField insert(GenericField field) {
    var doc = toDoc(field);

    try {
      mongoTemplate.insert(doc, COLLECTION);
    } catch (DuplicateKeyException e) {
      throw new DuplicateFieldNameException();
    }

    return field;
  }

  @Override
  public List<GenericField> findAllByTenant(TenantId tenantId) {
    var rawTenantId = tenantId.value();
    var lowestName = new MinKey();
    var highestName = new MaxKey();
    var firstIdOfTenant = new Document(TENANT_ID, rawTenantId)
        .append(NAME, lowestName);
    var lastIdOfTenant = new Document(TENANT_ID, rawTenantId)
        .append(NAME, highestName);
    var fromFirstIdOfTenantFilter = gte(ID, firstIdOfTenant);
    var toLastIdOfTenantFilter = lte(ID, lastIdOfTenant);
    var tenantIdRangeFilter = and(fromFirstIdOfTenantFilter, toLastIdOfTenantFilter);
    var fields = new ArrayList<GenericField>();

    mongoTemplate.getCollection(COLLECTION)
        .find(tenantIdRangeFilter)
        .map(MongoGenericFieldDbAdapter::toDomain)
        .into(fields);

    return fields;
  }

  private static Document toDoc(GenericField field) {
    var tenantId = field.getTenantId();
    var name = field.getName();
    var type = field.getType();
    var id = new Document(TENANT_ID, tenantId.value())
        .append(NAME, name.value());
    return new Document(ID, id)
        .append(TYPE, type.name());
  }

  private static GenericField toDomain(Document doc) {
    var id = doc.get(ID, Document.class);
    var rawTenantId = id.getString(TENANT_ID);
    var rawName = id.getString(NAME);
    var rawType = doc.getString(TYPE);
    var tenantId = new TenantId(rawTenantId);
    var name = new FieldName(rawName);
    var type = FieldType.of(rawType);
    return GenericField.builder()
        .tenantId(tenantId)
        .name(name)
        .type(type)
        .build();
  }
}
