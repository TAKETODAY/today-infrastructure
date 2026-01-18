/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.app.test.mock.mockito;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.app.test.mock.mockito.example.ExampleService;
import infra.beans.factory.FactoryBean;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Lazy;
import infra.test.context.junit.jupiter.InfraExtension;

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
