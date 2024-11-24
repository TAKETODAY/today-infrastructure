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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.FactoryBean;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.app.test.mock.mockito.example.ExampleService;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResetMocksTestExecutionListener}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(InfraExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
class ResetMocksTestExecutionListenerTests {

  @Autowired
  private ApplicationContext context;

  @Test
  void test001() {
    given(getMock("none").greeting()).willReturn("none");
    given(getMock("before").greeting()).willReturn("before");
    given(getMock("after").greeting()).willReturn("after");
  }

  @Test
  void test002() {
    Assertions.assertThat(getMock("none").greeting()).isEqualTo("none");
    Assertions.assertThat(getMock("before").greeting()).isNull();
    Assertions.assertThat(getMock("after").greeting()).isNull();
  }

  ExampleService getMock(String name) {
    return this.context.getBean(name, ExampleService.class);
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    ExampleService before(MockitoBeans mockedBeans) {
      ExampleService mock = mock(ExampleService.class, MockReset.before());
      mockedBeans.add(mock);
      return mock;
    }

    @Bean
    ExampleService after(MockitoBeans mockedBeans) {
      ExampleService mock = mock(ExampleService.class, MockReset.after());
      mockedBeans.add(mock);
      return mock;
    }

    @Bean
    ExampleService none(MockitoBeans mockedBeans) {
      ExampleService mock = mock(ExampleService.class);
      mockedBeans.add(mock);
      return mock;
    }

    @Bean
    @Lazy
    ExampleService fail() {
      // gh-5870
      throw new RuntimeException();
    }

    @Bean
    BrokenFactoryBean brokenFactoryBean() {
      // gh-7270
      return new BrokenFactoryBean();
    }

  }

  static class BrokenFactoryBean implements FactoryBean<String> {

    @Override
    public String getObject() {
      throw new IllegalStateException();
    }

    @Override
    public Class<?> getObjectType() {
      return String.class;
    }

    @Override
    public boolean isSingleton() {
      return true;
    }

  }

}
