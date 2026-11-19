package com.example.customers.infrastructure.config;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.example.customers.infrastructure.db.UnavailabilityAwareMongoExceptionTranslator;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.mongodb.autoconfigure.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoDatabaseFactorySupport;
import org.springframework.resilience.annotation.EnableResilientMethods;

@Configuration(proxyBeanMethods = false)
@EnableResilientMethods
public class MongoConfiguration {

  /** One time budget per operation (server selection, connecting, the query itself), so no call can hang. */
  @Bean
  MongoClientSettingsBuilderCustomizer mongoTimeoutCustomizer(@Value("${customers.mongodb.timeout}") Duration timeout) {
    var timeoutMillis = timeout.toMillis();
    return builder -> builder.timeout(timeoutMillis, MILLISECONDS);
  }

  /** Installs the translator on the database factory, before {@code MongoTemplate} takes it from there. */
  @Bean
  static BeanPostProcessor mongoExceptionTranslatorInstaller() {
    return new BeanPostProcessor() {

      @Override
      public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof MongoDatabaseFactorySupport<?> databaseFactory) {
          var exceptionTranslator = new UnavailabilityAwareMongoExceptionTranslator();
          databaseFactory.setExceptionTranslator(exceptionTranslator);
        }

        return bean;
      }
    };
  }
}
