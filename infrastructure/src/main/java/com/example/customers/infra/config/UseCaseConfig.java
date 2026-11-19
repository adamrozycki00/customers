package com.example.customers.infra.config;

import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.application.service.CustomerService;
import com.example.customers.application.service.GenericFieldService;
import com.example.customers.application.service.UpdateCustomerService;
import com.example.customers.application.service.settings.PagingSettings;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UseCaseConfig.PagingProperties.class)
public class UseCaseConfig {

  @Bean
  GenericFieldService genericFieldService(GenericFieldPort genericFieldPort) {
    return new GenericFieldService(genericFieldPort);
  }

  @Bean
  PagingSettings pagingSettings(PagingProperties pagingProperties) {
    return new PagingSettings(pagingProperties.defaultPageSize(), pagingProperties.maxPageSize());
  }

  @Bean
  CustomerService customerService(CustomerPort customerPort, GenericFieldPort genericFieldPort,
                                  PagingSettings pagingSettings) {
    return CustomerService.builder()
        .customerPort(customerPort)
        .genericFieldPort(genericFieldPort)
        .pagingSettings(pagingSettings)
        .build();
  }

  @Bean
  UpdateCustomerService updateCustomerService(CustomerPort customerPort, GenericFieldPort genericFieldPort) {
    return new UpdateCustomerService(customerPort, genericFieldPort);
  }

  @ConfigurationProperties("customers.paging")
  record PagingProperties(int defaultPageSize, int maxPageSize) {
  }
}
