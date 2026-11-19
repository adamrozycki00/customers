package com.example.customers.infrastructure.db.customer;

import static com.example.customers.infrastructure.db.customer.CustomerDbHelper.hasNoUpdates;
import static com.example.customers.infrastructure.db.customer.CustomerDbHelper.toActiveCustomerFilter;
import static com.example.customers.infrastructure.db.customer.CustomerDbHelper.toActiveCustomersOfTenantFilter;
import static com.example.customers.infrastructure.db.customer.CustomerDbHelper.toDeletedCustomerFilter;
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
import com.example.customers.domain.customer.CustomerId;
import com.example.customers.domain.exception.ConflictException;
import com.example.customers.domain.tenant.TenantId;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.resilience.annotation.Retryable;
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
      var tenantId = customer.getTenantId();
      var id = customer.getId();
      var deletedCustomerFilter = toDeletedCustomerFilter(tenantId, id);
      var deletedCustomerCount = mongoTemplate.execute(COLLECTION, collection -> collection
          .countDocuments(deletedCustomerFilter));

      if (deletedCustomerCount > 0) {
        throw new ConflictException("Customer with this id was deleted; ids of deleted customers cannot be reused");
      }

      throw new ConflictException("Customer with this id already exists");
    }

    return customer;
  }

  @Override
  @Retryable(includes = {DataAccessResourceFailureException.class, TransientDataAccessException.class},
      maxRetries = 2, delay = 100, multiplier = 2)
  public Optional<Customer> find(TenantId tenantId, CustomerId id) {
    var activeCustomerFilter = toActiveCustomerFilter(tenantId, id);
    var doc = mongoTemplate.execute(COLLECTION, collection -> collection
        .find(activeCustomerFilter)
        .first());

    if (nonNull(doc)) {
      var customer = toDomain(doc);
      return Optional.of(customer);
    }

    return Optional.empty();
  }

  @Override
  @Retryable(includes = {DataAccessResourceFailureException.class, TransientDataAccessException.class},
      maxRetries = 2, delay = 100, multiplier = 2)
  public CustomerPage getPage(TenantId tenantId, PageRequest pageRequest) {
    var page = pageRequest.page();
    var size = pageRequest.size();
    var customersOfEarlierPages = (page - 1) * size;
    var activeCustomersOfTenantFilter = toActiveCustomersOfTenantFilter(tenantId);
    var byId = ascending(ID);
    var customers = new ArrayList<Customer>();

    mongoTemplate.execute(COLLECTION, collection -> collection
        .find(activeCustomersOfTenantFilter)
        .sort(byId)
        .skip(customersOfEarlierPages)
        .limit(size)
        .map(CustomerDocMapper::toDomain)
        .into(customers));

    var totalElements = mongoTemplate.execute(COLLECTION, collection -> collection
        .countDocuments(activeCustomersOfTenantFilter));

    return CustomerPage.builder()
        .customers(customers)
        .page(page)
        .size(size)
        .totalElements(totalElements)
        .build();
  }

  @Override
  public Optional<Customer> update(TenantId tenantId, CustomerId id, CustomerUpdate update) {
    if (hasNoUpdates(update)) {
      return find(tenantId, id);
    }

    var activeCustomerFilter = toActiveCustomerFilter(tenantId, id);
    var updates = toUpdateBson(update);
    var returnUpdatedDoc = new FindOneAndUpdateOptions().returnDocument(AFTER);
    var doc = mongoTemplate.execute(COLLECTION, collection -> collection
        .findOneAndUpdate(activeCustomerFilter, updates, returnUpdatedDoc));

    if (nonNull(doc)) {
      var customer = toDomain(doc);
      return Optional.of(customer);
    }

    return Optional.empty();
  }

  @Override
  public void delete(TenantId tenantId, CustomerId id) {
    var activeCustomerFilter = toActiveCustomerFilter(tenantId, id);
    var now = clock.instant();
    var deletedAt = Date.from(now);
    var setDeletedAt = set(DELETED_AT, deletedAt);
    mongoTemplate.execute(COLLECTION, collection -> collection
        .updateOne(activeCustomerFilter, setDeletedAt));
  }
}
