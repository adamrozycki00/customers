package com.example.customers.infrastructure.config;

import com.example.customers.application.port.out.CustomerPort;
import com.example.customers.application.port.out.GenericFieldPort;
import com.example.customers.application.service.CustomerService;
import com.example.customers.application.service.GenericFieldService;
import com.example.customers.application.service.PagingSettings;
import com.example.customers.application.service.UpdateCustomerService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(PagingProperties.class)
public class UseCaseConfiguration {

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
}
