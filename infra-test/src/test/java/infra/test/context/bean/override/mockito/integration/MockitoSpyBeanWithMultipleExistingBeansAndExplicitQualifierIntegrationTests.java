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
import infra.beans.factory.annotation.Qualifier;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.context.bean.override.example.ExampleGenericServiceCaller;
import infra.test.context.bean.override.example.IntegerExampleGenericService;
import infra.test.context.bean.override.example.StringExampleGenericService;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.mockito.MockitoAssertions.assertIsSpy;
import static infra.test.mockito.MockitoAssertions.assertMockName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} can be used to spy on a bean
 * when there are multiple candidates and a {@link Qualifier @Qualifier} is supplied
 * to select one of the candidates.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see MockitoSpyBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests
 * @see MockitoSpyBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests
 * @since 5.0
 */
@JUnitConfig
class MockitoSpyBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests {

  @Qualifier("stringService")
  @MockitoSpyBean
  StringExampleGenericService spy;

  @Autowired
  ExampleGenericServiceCaller caller;

  @Test
  void test() {
    assertIsSpy(spy);
    assertMockName(spy, "stringService");

    assertThat(caller.sayGreeting()).isEqualTo("I say two 123");
    then(spy).should().greeting();
  }

  @Configuration(proxyBeanMethods = false)
  @Import({ ExampleGenericServiceCaller.class, IntegerExampleGenericService.class })
  static class Config {

    @Bean
    StringExampleGenericService one() {
      return new StringExampleGenericService("one");
    }

    @Bean
      // "stringService" matches the constructor argument name in ExampleGenericServiceCaller
    StringExampleGenericService stringService() {
      return new StringExampleGenericService("two");
    }
  }

}
