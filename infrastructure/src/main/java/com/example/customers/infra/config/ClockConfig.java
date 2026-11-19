package com.example.customers.infra.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock for soft-delete timestamps ({@code deletedAt}); a bean so tests can replace it with a fixed one.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
