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
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.Primary;
import infra.test.context.bean.override.example.ExampleGenericServiceCaller;
import infra.test.context.bean.override.example.IntegerExampleGenericService;
import infra.test.context.bean.override.example.StringExampleGenericService;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsSpy;
import static infra.test.mockito.MockitoAssertions.assertMockName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} can be used to spy on a bean
 * when there are multiple candidates and one is {@link Primary @Primary}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @see MockitoSpyBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests
 * @see MockitoSpyBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
class MockitoSpyBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests {

  @MockitoSpyBean
  StringExampleGenericService spy;

  @Autowired
  ExampleGenericServiceCaller caller;

  @Test
  void spying() {
    assertIsSpy(spy);
    assertMockName(spy, "two");

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
    @Primary
    StringExampleGenericService two() {
      return new StringExampleGenericService("two");
    }
  }

}
