package com.example.customers.infrastructure.db.customer;

import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.DELETED_AT;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.FIRST_NAME;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.GENERIC_FIELDS;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.ID;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.LAST_NAME;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.CUSTOMER_ID;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.TENANT_ID;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.toBsonValue;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.toId;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.MapUtils.isEmpty;

import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.FieldUpdate;
import com.example.customers.domain.customer.CustomerId;
import com.example.customers.domain.customer.FirstName;
import com.example.customers.domain.customer.LastName;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.tenant.TenantId;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import lombok.NoArgsConstructor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;

@NoArgsConstructor(access = PRIVATE)
final class CustomerDbHelper {

  private static final Bson NOT_DELETED_FILTER = eq(DELETED_AT, null);

  static boolean hasNoUpdates(CustomerUpdate update) {
    return update.firstName() instanceof FieldUpdate.Keep<FirstName>
        && update.lastName() instanceof FieldUpdate.Keep<LastName>
        && isEmpty(update.genericFieldsToSet())
        && isEmpty(update.genericFieldsToClear());
  }

  static Bson toUpdateBson(CustomerUpdate update) {
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
      case FieldUpdate.Keep<T> _ -> {
      }
    }
  }

  static Bson toActiveCustomerFilter(TenantId tenantId, CustomerId id) {
    var idDoc = toId(tenantId, id);
    var idFilter = eq(ID, idDoc);
    return and(idFilter, NOT_DELETED_FILTER);
  }

  static Bson toActiveCustomersOfTenantFilter(TenantId tenantId) {
    var tenantIdRangeFilter = toTenantIdRangeFilter(tenantId);
    return and(tenantIdRangeFilter, NOT_DELETED_FILTER);
  }

  private static Bson toTenantIdRangeFilter(TenantId tenantId) {
    var rawTenantId = tenantId.value();
    var lowestId = new MinKey();
    var highestId = new MaxKey();
    var firstIdOfTenant = new Document(TENANT_ID, rawTenantId)
        .append(CUSTOMER_ID, lowestId);
    var lastIdOfTenant = new Document(TENANT_ID, rawTenantId)
        .append(CUSTOMER_ID, highestId);
    var fromFirstIdOfTenantFilter = gte(ID, firstIdOfTenant);
    var toLastIdOfTenantFilter = lte(ID, lastIdOfTenant);
    return and(fromFirstIdOfTenantFilter, toLastIdOfTenantFilter);
  }
}
