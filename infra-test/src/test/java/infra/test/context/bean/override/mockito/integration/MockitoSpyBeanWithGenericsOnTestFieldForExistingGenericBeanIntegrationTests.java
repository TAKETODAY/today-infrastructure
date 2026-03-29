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
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.context.bean.override.example.ExampleGenericService;
import infra.test.context.bean.override.example.ExampleGenericServiceCaller;
import infra.test.context.bean.override.example.IntegerExampleGenericService;
import infra.test.context.bean.override.example.StringExampleGenericService;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.JUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} on a field with generics can
 * be used to replace an existing bean with matching generics.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see MockitoBeanWithGenericsOnTestFieldForNewBeanIntegrationTests
 * @see MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanProducedByFactoryBeanIntegrationTests
 * @since 5.0
 */
@JUnitConfig
class MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests {

  @MockitoSpyBean
  ExampleGenericService<String> service;

  @Autowired
  ExampleGenericServiceCaller caller;

  @Test
  void spying() {
    assertThat(caller.sayGreeting()).isEqualTo("I say Enigma 123");
    then(service).should().greeting();
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ ExampleGenericServiceCaller.class, IntegerExampleGenericService.class })
  static class Config {

    @Bean
    ExampleGenericService<String> simpleExampleStringGenericService() {
      // In order to trigger the issue, we need a method signature that returns the
      // generic type instead of the actual implementation class.
      return new StringExampleGenericService("Enigma");
    }
  }

}
