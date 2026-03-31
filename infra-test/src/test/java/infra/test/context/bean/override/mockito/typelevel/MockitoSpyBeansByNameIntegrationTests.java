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

package infra.test.context.bean.override.mockito.typelevel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.bean.override.mockito.MockitoSpyBeans;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.mockito.MockitoAssertions.assertIsNotMock;
import static infra.test.mockito.MockitoAssertions.assertIsNotSpy;
import static infra.test.mockito.MockitoAssertions.assertIsSpy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Integration tests for {@link MockitoSpyBeans @MockitoSpyBeans} and
 * {@link MockitoSpyBean @MockitoSpyBean} declared "by name" at the class level
 * as a repeatable annotation.
 *
 * @author Sam Brannen
 * @see MockitoSpyBeansByTypeIntegrationTests
 * @since 5.0
 */
@JUnitConfig
@MockitoSpyBean(name = "s1", types = ExampleService.class)
@MockitoSpyBean(name = "s2", types = ExampleService.class)
class MockitoSpyBeansByNameIntegrationTests {

  @Autowired
  ExampleService s1;

  @Autowired
  ExampleService s2;

  @MockitoSpyBean(name = "s3")
  ExampleService service3;

  @Autowired
  @Qualifier("s4")
  ExampleService service4;

  @BeforeEach
  void configureSpies() {
    given(s1.greeting()).willReturn("spy 1");
    given(s2.greeting()).willReturn("spy 2");
    given(service3.greeting()).willReturn("spy 3");
  }

  @Test
  void checkSpiesAndStandardBean() {
    assertIsSpy(s1, "s1");
    assertIsSpy(s2, "s2");
    assertIsSpy(service3, "service3");
    assertIsNotMock(service4, "service4");
    assertIsNotSpy(service4, "service4");

    assertThat(s1.greeting()).isEqualTo("spy 1");
    assertThat(s2.greeting()).isEqualTo("spy 2");
    assertThat(service3.greeting()).isEqualTo("spy 3");
    assertThat(service4.greeting()).isEqualTo("prod 4");
  }

  @Configuration
  static class Config {

    @Bean
    ExampleService s1() {
      return new ExampleService() {
        @Override
        public String greeting() {
          return "prod 1";
        }
      };
    }

    @Bean
    ExampleService s2() {
      return new ExampleService() {
        @Override
        public String greeting() {
          return "prod 2";
        }
      };
    }

    @Bean
    ExampleService s3() {
      return new ExampleService() {
        @Override
        public String greeting() {
          return "prod 3";
        }
      };
    }

    @Bean
    ExampleService s4() {
      return () -> "prod 4";
    }
  }

}
