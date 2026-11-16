package com.example.customers;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

  @Bean
  @ServiceConnection
  MongoDBContainer mongoDbContainer() {
    var mongoImage = DockerImageName.parse("mongo:9.0.8");
    return new MongoDBContainer(mongoImage);
  }
}
