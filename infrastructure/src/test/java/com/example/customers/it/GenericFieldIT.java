package com.example.customers.it;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import com.example.customers.infra.config.TestcontainersConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

@Import(TestcontainersConfig.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class GenericFieldIT {

  private static final String GENERIC_FIELDS_COLLECTION = "genericFields";

  @Autowired
  private RestTestClient client;

  @Autowired
  private MongoTemplate mongo;

  @BeforeEach
  void clearCollection() {
    mongo.dropCollection(GENERIC_FIELDS_COLLECTION);
  }

  @Test
  void shouldRegisterGenericField() {
    String inputRequest = """
        {"name": "vip", "type": "BOOL"}
        """;
    String expectedResponse = """
        {"name": "vip", "type": "BOOL"}
        """;

    var resultResponse = client.post()
        .uri("/generic-fields")
        .header("X-Tenant-Id", "flowers")
        .contentType(APPLICATION_JSON)
        .body(inputRequest)
        .exchange();

    resultResponse.expectStatus().isCreated()
        .expectBody().json(expectedResponse);
  }
}
