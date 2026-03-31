/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.test.context.bean.override.mockito;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import infra.beans.factory.FactoryBean;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.FailingExampleService;
import infra.test.context.bean.override.example.RealExampleService;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.context.bean.override.mockito.MockReset.BEFORE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * Integration tests for {@link MockitoBean} that validate automatic reset
 * of stubbing.
 *
 * @author Simon Baslé
 * @since 5.0
 */
@JUnitConfig
@TestMethodOrder(OrderAnnotation.class)
public class MockitoBeanWithResetIntegrationTests {

  @MockitoBean(reset = BEFORE)
  ExampleService service;

  @MockitoBean(reset = BEFORE)
  FailingExampleService failingService;

  @Order(1)
  @Test
  void beanFirstEstablishingMock(ApplicationContext ctx) {
    ExampleService mock = ctx.getBean("service", ExampleService.class);
    doReturn("Mocked hello").when(mock).greeting();

    assertThat(this.service.greeting()).isEqualTo("Mocked hello");
  }

  @Order(2)
  @Test
  void beanSecondEnsuringMockReset(ApplicationContext ctx) {
    assertThat(ctx.getBean("service")).isNotNull().isSameAs(this.service);

    assertThat(this.service.greeting()).as("not stubbed").isNull();
  }

  @Order(3)
  @Test
  void factoryBeanFirstEstablishingMock(ApplicationContext ctx) {
    FailingExampleService mock = ctx.getBean(FailingExampleService.class);
    doReturn("Mocked hello").when(mock).greeting();

    assertThat(this.failingService.greeting()).isEqualTo("Mocked hello");
  }

  @Order(4)
  @Test
  void factoryBeanSecondEnsuringMockReset(ApplicationContext ctx) {
    assertThat(ctx.getBean("factory")).isNotNull().isSameAs(this.failingService);

    assertThat(this.failingService.greeting()).as("not stubbed")
            .isNull();
  }

  static class FailingExampleServiceFactory implements FactoryBean<FailingExampleService> {
    @Override
    public @Nullable FailingExampleService getObject() {
      return new FailingExampleService();
    }

    @Override
    public @Nullable Class<?> getObjectType() {
      return FailingExampleService.class;
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean("service")
    ExampleService bean1() {
      return new RealExampleService("Production hello");
    }

    @Bean("factory")
    FailingExampleServiceFactory factory() {
      return new FailingExampleServiceFactory();
    }
  }

}
