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
import infra.test.context.bean.override.example.ExampleGenericService;
import infra.test.context.bean.override.example.ExampleGenericServiceCaller;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests that {@link MockitoBean @MockitoBean} on fields with generics can be used
 * to inject new mock instances.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests
 * @since 5.0
 */
@JUnitConfig
class MockitoBeanWithGenericsOnTestFieldForNewBeanIntegrationTests {

  @MockitoBean
  ExampleGenericService<String> stringService;

  @MockitoBean
  ExampleGenericService<Integer> integerService;

  @Autowired
  ExampleGenericServiceCaller caller;

  @Test
  void mocking() {
    given(stringService.greeting()).willReturn("Hello");
    given(integerService.greeting()).willReturn(42);
    assertThat(caller.sayGreeting()).isEqualTo("I say Hello 42");
  }

  @Configuration(proxyBeanMethods = false)
  @Import(ExampleGenericServiceCaller.class)
  static class Config {
  }

}
