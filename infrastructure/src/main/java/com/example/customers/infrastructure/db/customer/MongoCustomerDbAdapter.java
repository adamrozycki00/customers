package com.example.customers.infrastructure.db.customer;

import static com.example.customers.infrastructure.db.customer.CustomerDbHelper.hasNoUpdates;
import static com.example.customers.infrastructure.db.customer.CustomerDbHelper.toActiveCustomerFilter;
import static com.example.customers.infrastructure.db.customer.CustomerDbHelper.toActiveCustomersOfTenantFilter;
import static com.example.customers.infrastructure.db.customer.CustomerDbHelper.toUpdateBson;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.DELETED_AT;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.ID;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.toDoc;
import static com.example.customers.infrastructure.db.customer.CustomerDocMapper.toDomain;
import static com.mongodb.client.model.ReturnDocument.AFTER;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Updates.set;
import static java.util.Objects.nonNull;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.PageRequest;
import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.customer.exception.DuplicatePublicIdException;
import com.example.customers.domain.tenant.TenantId;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MongoCustomerDbAdapter implements CustomerPort {

  private static final String COLLECTION = "customers";

  private final MongoTemplate mongoTemplate;
  private final Clock clock;

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
    var activeCustomersOfTenantFilter = toActiveCustomersOfTenantFilter(tenantId);
    var byPublicId = ascending(ID);
    var collection = mongoTemplate.getCollection(COLLECTION);
    var customers = new ArrayList<Customer>();

    collection.find(activeCustomersOfTenantFilter)
        .sort(byPublicId)
        .skip(customersOfEarlierPages)
        .limit(size)
        .map(CustomerDocMapper::toDomain)
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

  @Override
  public void delete(TenantId tenantId, PublicId publicId) {
    var activeCustomerFilter = toActiveCustomerFilter(tenantId, publicId);
    var now = clock.instant();
    var deletedAt = Date.from(now);
    var setDeletedAt = set(DELETED_AT, deletedAt);
    mongoTemplate.getCollection(COLLECTION)
        .updateOne(activeCustomerFilter, setDeletedAt);
  }
}
