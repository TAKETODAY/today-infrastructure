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

package infra.test.context.bean.override.mockito.integration;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.JUnitConfig;

/**
 * Abstract base class for tests for {@link MockitoBean @MockitoBean} with generics.
 *
 * @param <T> type of thing
 * @param <S> type of something
 * @author Madhura Bhave
 * @author Sam Brannen
 * @see MockitoBeanAndGenericsIntegrationTests
 * @since 5.0
 */
@JUnitConfig
abstract class AbstractMockitoBeanAndGenericsIntegrationTests<T extends AbstractMockitoBeanAndGenericsIntegrationTests.Thing<S>, S extends AbstractMockitoBeanAndGenericsIntegrationTests.Something> {

  @Autowired
  T thing;

  @MockitoBean
  S something;

  static class Something {
    String speak() {
      return "Hi";
    }
  }

  static class SomethingImpl extends Something {
  }

  abstract static class Thing<S extends Something> {

    @Autowired
    private S something;

    S getSomething() {
      return this.something;
    }
  }

  static class ThingImpl extends Thing<SomethingImpl> {
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    ThingImpl thing() {
      return new ThingImpl();
    }
  }

}
