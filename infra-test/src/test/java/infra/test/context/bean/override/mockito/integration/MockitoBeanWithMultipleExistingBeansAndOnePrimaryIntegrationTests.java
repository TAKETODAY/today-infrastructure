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
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsMock;
import static infra.test.mockito.MockitoAssertions.assertMockName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Tests that {@link MockitoBean @MockitoBean} can be used to mock a bean when
 * there are multiple candidates and one is primary.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see MockitoBeanWithMultipleExistingBeansAndOnePrimaryAndOneConflictingQualifierIntegrationTests
 * @see MockitoBeanWithMultipleExistingBeansAndExplicitBeanNameIntegrationTests
 * @see MockitoBeanWithMultipleExistingBeansAndExplicitQualifierIntegrationTests
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
class MockitoBeanWithMultipleExistingBeansAndOnePrimaryIntegrationTests {

  @MockitoBean
  StringExampleGenericService mock;

  @Autowired
  ExampleGenericServiceCaller caller;

  @Test
  void test() {
    assertIsMock(mock);
    assertMockName(mock, "two");

    given(mock.greeting()).willReturn("mocked");
    assertThat(caller.sayGreeting()).isEqualTo("I say mocked 123");
    then(mock).should().greeting();
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
