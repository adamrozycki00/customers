package com.example.customers.infrastructure.config;

import com.example.customers.infrastructure.db.genericfield.CachingGenericFieldAdapter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class GenericFieldCacheConfiguration {

  @Bean
  ApplicationRunner genericFieldCacheLoader(CachingGenericFieldAdapter cachingGenericFieldAdapter) {
    return args -> cachingGenericFieldAdapter.refreshAll();
  }
}
