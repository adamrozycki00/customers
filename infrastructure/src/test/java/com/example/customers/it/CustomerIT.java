package com.example.customers.it;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.example.customers.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.client.RestTestClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class CustomerIT {

  private static final String GENERIC_FIELDS_COLLECTION = "genericFields";
  private static final String CUSTOMERS_COLLECTION = "customers";

  @Autowired
  private RestTestClient restTestClient;

  @Autowired
  private MongoTemplate mongoTemplate;

  @BeforeEach
  void clearCollections() {
    mongoTemplate.dropCollection(GENERIC_FIELDS_COLLECTION);
    mongoTemplate.dropCollection(CUSTOMERS_COLLECTION);
  }

  @Test
  void shouldCreateAndGetCustomer() {
    // given registered vip, score and since fields, and a customer created with values for them
    registerGenericField("vip", "BOOLEAN");
    registerGenericField("score", "INTEGER");
    registerGenericField("since", "DATETIME");
    String inputCreateRequest = """
        {
          "public_id": "CUST-1",
          "first_name": "Anna",
          "generic_fields": {"vip": true, "score": 42, "since": "1990-05-20T00:00:00+02:00"}
        }
        """;
    restTestClient.post()
        .uri("/customers")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .body(inputCreateRequest)
        .exchange()
        .expectStatus().isCreated();
    String expectedResponse = """
        {
          "public_id": "CUST-1",
          "first_name": "Anna",
          "generic_fields": {"vip": true, "score": 42, "since": "1990-05-19T22:00:00Z"}
        }
        """;

    var resultResponse = restTestClient.get()
        .uri("/customers/CUST-1")
        .header("X-Tenant-Id", "flowers")
        .exchange();

    resultResponse.expectStatus().isOk()
        .expectBody().json(expectedResponse, JsonCompareMode.STRICT);
  }

  private void registerGenericField(String name, String type) {
    String registerRequest = """
        {"name": "%s", "type": "%s"}
        """.formatted(name, type);
    restTestClient.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .body(registerRequest)
        .exchange()
        .expectStatus().isCreated();
  }
}
