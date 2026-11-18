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

  private final TenantId validTenantId = new TenantId("flowers");
  private final PublicId validPublicId = new PublicId("CUST-1");

  @Nested
  class Equality {

    @Test
    void shouldBeEqualWhenIdsAreEqual() {
      var firstNameBefore = new FirstName("Anna");
      var firstNameAfter = new FirstName("Maria");
      var customerBeforeRename = Customer.builder()
          .tenantId(validTenantId)
          .publicId(validPublicId)
          .firstName(firstNameBefore)
          .build();
      var customerAfterRename = Customer.builder()
          .tenantId(validTenantId)
          .publicId(validPublicId)
          .firstName(firstNameAfter)
          .build();

      var resultEquality = customerBeforeRename.equals(customerAfterRename);

      assertThat(resultEquality).isTrue();
    }

    @Test
    void shouldNotBeEqualWhenPublicIdsDiffer() {
      var otherPublicId = new PublicId("CUST-2");
      var customer = Customer.builder()
          .tenantId(validTenantId)
          .publicId(validPublicId)
          .build();
      var otherCustomer = Customer.builder()
          .tenantId(validTenantId)
          .publicId(otherPublicId)
          .build();

      var resultEquality = customer.equals(otherCustomer);

      assertThat(resultEquality).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenTenantIdsDiffer() {
      var otherTenantId = new TenantId("roses");
      var customer = Customer.builder()
          .tenantId(validTenantId)
          .publicId(validPublicId)
          .build();
      var otherTenantsCustomer = Customer.builder()
          .tenantId(otherTenantId)
          .publicId(validPublicId)
          .build();

      var resultEquality = customer.equals(otherTenantsCustomer);

      assertThat(resultEquality).isFalse();
    }
  }

  @Nested
  class ValidCustomers {

    @Test
    void shouldCreateCustomerWithOnlyPublicId() {
      var inputValidPublicId = new PublicId("CUST-1");

      var resultCustomer = Customer.builder()
          .tenantId(validTenantId)
          .publicId(inputValidPublicId)
          .build();

      assertThat(resultCustomer.getFirstName()).isNull();
      assertThat(resultCustomer.getLastName()).isNull();
    }

    @Test
    void shouldHaveEmptyGenericFieldsWhenOmitted() {
      var customerBuilderWithoutGenericFields = Customer.builder()
          .tenantId(validTenantId)
          .publicId(validPublicId);

      var resultCustomer = customerBuilderWithoutGenericFields.build();

      assertThat(resultCustomer.getGenericFields()).isEmpty();
    }

    @Test
    void shouldTreatMissingGenericFieldsAsEmpty() {
      Map<FieldName, GenericFieldValue> inputMissingGenericFields = null;

      var resultCustomer = Customer.builder()
          .tenantId(validTenantId)
          .publicId(validPublicId)
          .genericFields(inputMissingGenericFields)
          .build();

      assertThat(resultCustomer.getGenericFields()).isEmpty();
    }
  }

  @Nested
  class InvalidCustomers {

    @Test
    void shouldRejectMissingPublicId() {
      PublicId inputMissingPublicId = null;

      var thrown = catchThrowable(() -> Customer.builder()
          .tenantId(validTenantId)
          .publicId(inputMissingPublicId)
          .build());

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Customer requires a tenant id and a public id");
    }

    @Test
    void shouldRejectMissingTenantId() {
      TenantId inputMissingTenantId = null;

      var thrown = catchThrowable(() -> Customer.builder()
          .tenantId(inputMissingTenantId)
          .publicId(validPublicId)
          .build());

      assertThat(thrown).isInstanceOf(ValidationException.class)
          .hasMessage("Customer requires a tenant id and a public id");
    }
  }
}
