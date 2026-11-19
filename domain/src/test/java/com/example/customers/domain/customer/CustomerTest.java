package com.example.customers.domain.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.example.customers.domain.exception.ValidationException;
import com.example.customers.domain.genericfield.FieldName;
import com.example.customers.domain.genericfield.value.GenericFieldValue;
import com.example.customers.domain.tenant.TenantId;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CustomerTest {

  private static final TenantId VALID_TENANT_ID = new TenantId("flowers");
  private static final CustomerId VALID_ID = new CustomerId("CUST-1");

  @Nested
  class Equality {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
      var firstNameBefore = new FirstName("Anna");
      var firstNameAfter = new FirstName("Maria");
      var customerBeforeRename = Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(VALID_ID)
          .firstName(firstNameBefore)
          .build();
      var customerAfterRename = Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(VALID_ID)
          .firstName(firstNameAfter)
          .build();

      var resultEquality = customerBeforeRename.equals(customerAfterRename);

      assertThat(resultEquality).isTrue();
    }

    @Test
    void shouldNotBeEqualWhenIdsDiffer() {
      var otherId = new CustomerId("CUST-2");
      var customer = Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(VALID_ID)
          .build();
      var otherCustomer = Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(otherId)
          .build();

      var resultEquality = customer.equals(otherCustomer);

      assertThat(resultEquality).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenTenantIdsDiffer() {
      var otherTenantId = new TenantId("roses");
      var customer = Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(VALID_ID)
          .build();
      var otherTenantsCustomer = Customer.builder()
          .tenantId(otherTenantId)
          .id(VALID_ID)
          .build();

      var resultEquality = customer.equals(otherTenantsCustomer);

      assertThat(resultEquality).isFalse();
    }
  }

  @Nested
  class ValidCustomers {

    @Test
    void shouldCreateCustomerWithOnlyId() {
      var inputValidId = new CustomerId("CUST-1");

      var resultCustomer = Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(inputValidId)
          .build();

      assertThat(resultCustomer.getFirstName()).isNull();
      assertThat(resultCustomer.getLastName()).isNull();
    }

    @Test
    void shouldHaveEmptyGenericFieldsWhenOmitted() {
      var customerBuilderWithoutGenericFields = Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(VALID_ID);

      var resultCustomer = customerBuilderWithoutGenericFields.build();

      assertThat(resultCustomer.getGenericFields()).isEmpty();
    }

    @Test
    void shouldTreatMissingGenericFieldsAsEmpty() {
      Map<FieldName, GenericFieldValue> inputMissingGenericFields = null;

      var resultCustomer = Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(VALID_ID)
          .genericFields(inputMissingGenericFields)
          .build();

      assertThat(resultCustomer.getGenericFields()).isEmpty();
    }
  }

  @Nested
  class InvalidCustomers {

    @Test
    void shouldRejectMissingId() {
      CustomerId inputMissingId = null;

      var thrown = catchThrowable(() -> Customer.builder()
          .tenantId(VALID_TENANT_ID)
          .id(inputMissingId)
          .build());

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }

    @Test
    void shouldRejectMissingTenantId() {
      TenantId inputMissingTenantId = null;

      var thrown = catchThrowable(() -> Customer.builder()
          .tenantId(inputMissingTenantId)
          .id(VALID_ID)
          .build());

      assertThat(thrown).isInstanceOf(ValidationException.class);
    }
  }
}
