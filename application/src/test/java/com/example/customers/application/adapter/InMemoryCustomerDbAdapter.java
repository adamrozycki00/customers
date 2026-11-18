package com.example.customers.application.adapter;

import com.example.customers.application.port.CustomerPage;
import com.example.customers.application.port.CustomerUpdate;
import com.example.customers.application.port.FieldUpdate;
import com.example.customers.application.port.PageRequest;
import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.domain.customer.Customer;
import com.example.customers.domain.customer.PublicId;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCustomerDbAdapter implements CustomerPort {

  private final List<Customer> customers = new ArrayList<>();

  @Override
  public Customer insert(Customer customer) {
    customers.add(customer);
    return customer;
  }

  @Override
  public Optional<Customer> find(TenantId tenantId, PublicId publicId) {
    return customers.stream()
        .filter(customer -> customer.getTenantId().equals(tenantId))
        .filter(customer -> customer.getPublicId().equals(publicId))
        .findFirst();
  }

  @Override
  public CustomerPage getPage(TenantId tenantId, PageRequest pageRequest) {
    var tenantCustomers = customers.stream()
        .filter(customer -> customer.getTenantId().equals(tenantId))
        .toList();
    long offset = (long) (pageRequest.page() - 1) * pageRequest.size();
    var pageCustomers = tenantCustomers.stream()
        .skip(offset)
        .limit(pageRequest.size())
        .toList();

    return CustomerPage.builder()
        .customers(pageCustomers)
        .page(pageRequest.page())
        .size(pageRequest.size())
        .totalElements(tenantCustomers.size())
        .build();
  }

  @Override
  public Optional<Customer> update(TenantId tenantId, PublicId publicId, CustomerUpdate update) {
    var storedCustomer = find(tenantId, publicId);

    if (storedCustomer.isEmpty()) {
      return Optional.empty();
    }

    var current = storedCustomer.get();
    var firstName = getUpdatedField(update.firstName(), current.getFirstName());
    var lastName = getUpdatedField(update.lastName(), current.getLastName());
    Map<FieldName, GenericFieldValue> genericFields = new LinkedHashMap<>(current.getGenericFields());
    genericFields.putAll(update.genericFieldsToSet());
    genericFields.keySet().removeAll(update.genericFieldsToClear());
    var updatedCustomer = Customer.builder()
        .tenantId(current.getTenantId())
        .publicId(current.getPublicId())
        .firstName(firstName)
        .lastName(lastName)
        .genericFields(genericFields)
        .build();
    customers.remove(current);
    customers.add(updatedCustomer);

    return Optional.of(updatedCustomer);
  }

  private static <T> T getUpdatedField(FieldUpdate<T> fieldUpdate, T currentValue) {
    return switch (fieldUpdate) {
      case FieldUpdate.Keep<T> _ -> currentValue;
      case FieldUpdate.Replace<T> replace -> replace.value();
    };
  }

  @Override
  public void delete(TenantId tenantId, PublicId publicId) {
    customers.removeIf(customer -> customer.getTenantId().equals(tenantId) && customer.getPublicId().equals(publicId));
  }
}
