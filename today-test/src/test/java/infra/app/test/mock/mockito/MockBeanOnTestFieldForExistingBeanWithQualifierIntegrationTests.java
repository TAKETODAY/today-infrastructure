/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.app.test.mock.mockito.example.CustomQualifier;
import infra.app.test.mock.mockito.example.CustomQualifierExampleService;
import infra.app.test.mock.mockito.example.ExampleService;
import infra.app.test.mock.mockito.example.ExampleServiceCaller;
import infra.app.test.mock.mockito.example.RealExampleService;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Test {@link MockBean @MockBean} on a test class field can be used to replace existing
 * bean while preserving qualifiers.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
class MockBeanOnTestFieldForExistingBeanWithQualifierIntegrationTests {

  @MockBean
  @CustomQualifier
  private ExampleService service;

  @Autowired
  private ExampleServiceCaller caller;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void testMocking() {
    this.caller.sayGreeting();
    then(this.service).should().greeting();
  }

  @Test
  void onlyQualifiedBeanIsReplaced() {
    assertThat(this.applicationContext.getBean("service")).isSameAs(this.service);
    ExampleService anotherService = this.applicationContext.getBean("anotherService", ExampleService.class);
    assertThat(anotherService.greeting()).isEqualTo("Another");
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    CustomQualifierExampleService service() {
      return new CustomQualifierExampleService();
    }

    @Bean
    ExampleService anotherService() {
      return new RealExampleService("Another");
    }

    @Bean
    ExampleServiceCaller controller(@CustomQualifier ExampleService service) {
      return new ExampleServiceCaller(service);
    }

  }

}
