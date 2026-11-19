package com.example.customers.infra.config;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.example.customers.infra.db.exception.UnavailabilityAwareMongoExceptionTranslator;
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
public class MongoConfig {

  /**
   * One time budget per operation, so no call can hang.
   */
  @Bean
  MongoClientSettingsBuilderCustomizer mongoTimeoutCustomizer(@Value("${customers.mongodb.timeout}") Duration timeout) {
    var timeoutMillis = timeout.toMillis();
    return builder -> builder.timeout(timeoutMillis, MILLISECONDS);
  }

  /**
   * Installs {@link UnavailabilityAwareMongoExceptionTranslator} on the database factory, so driver timeouts become
   * {@code DataAccessResourceFailureException} (retried, then 503) instead of uncategorized errors (500).
   * {@code MongoTemplate} copies the factory's translator when created, hence a post-processor.
   */
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
