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
import org.springframework.test.web.servlet.client.RestTestClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class GenericFieldIT {

  private static final String GENERIC_FIELDS_COLLECTION = "genericFields";

  @Autowired
  private RestTestClient restTestClient;

  @Autowired
  private MongoTemplate mongoTemplate;

  @BeforeEach
  void clearCollection() {
    mongoTemplate.dropCollection(GENERIC_FIELDS_COLLECTION);
  }

  @Test
  void shouldRegisterGenericField() {
    String inputBody = """
        {"name": "vip", "type": "BOOLEAN"}
        """;
    String expectedBody = """
        {"name": "vip", "type": "BOOLEAN"}
        """;

    var resultResponse = restTestClient.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .body(inputBody)
        .exchange();

    resultResponse.expectStatus().isCreated()
        .expectBody().json(expectedBody);
  }
}
