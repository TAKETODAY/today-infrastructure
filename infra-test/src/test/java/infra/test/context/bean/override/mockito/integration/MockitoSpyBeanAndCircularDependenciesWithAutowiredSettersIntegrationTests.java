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

import org.junit.jupiter.api.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.mockito.BDDMockito.then;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} can be used to replace an
 * existing bean with circular dependencies with {@link Autowired @Autowired}
 * setter methods.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @see MockitoSpyBeanAndCircularDependenciesWithLazyResolutionProxyIntegrationTests
 * @since 5.0
 */
@JUnitConfig
@DisabledInAotMode("Circular dependencies cannot be resolved in AOT mode unless a @Lazy resolution proxy is used")
class MockitoSpyBeanAndCircularDependenciesWithAutowiredSettersIntegrationTests {

  @MockitoSpyBean
  One one;

  @Autowired
  Two two;

  @Test
  void beanWithCircularDependenciesCanBeSpied() {
    two.callOne();
    then(one).should().doSomething();
  }

  @Configuration
  @Import({ One.class, Two.class })
  static class Config {
  }

  static class One {

    @SuppressWarnings("unused")
    private Two two;

    @Autowired
    void setTwo(Two two) {
      this.two = two;
    }

    void doSomething() {
    }
  }

  static class Two {

    private One one;

    @Autowired
    void setOne(One one) {
      this.one = one;
    }

    void callOne() {
      this.one.doSomething();
    }
  }

}
