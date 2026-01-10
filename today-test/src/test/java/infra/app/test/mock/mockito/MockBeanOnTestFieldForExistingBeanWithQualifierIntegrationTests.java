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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.app.test.mock.mockito.example.CustomQualifier;
import infra.app.test.mock.mockito.example.CustomQualifierExampleService;
import infra.app.test.mock.mockito.example.ExampleService;
import infra.app.test.mock.mockito.example.ExampleServiceCaller;
import infra.app.test.mock.mockito.example.RealExampleService;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Test {@link MockBean @MockBean} on a test class field can be used to replace existing
 * bean while preserving qualifiers.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
@ExtendWith(InfraExtension.class)
class MockBeanOnTestFieldForExistingBeanWithQualifierIntegrationTests {

  @MockBean
  @CustomQualifier
  private ExampleService service;

  @Autowired
  private ExampleServiceCaller caller;

  @Autowired
  private ApplicationContext applicationContext;

  @Test
  void testMocking() {
    this.caller.sayGreeting();
    then(this.service).should().greeting();
  }

  @Test
  void onlyQualifiedBeanIsReplaced() {
    assertThat(this.applicationContext.getBean("service")).isSameAs(this.service);
    ExampleService anotherService = this.applicationContext.getBean("anotherService", ExampleService.class);
    assertThat(anotherService.greeting()).isEqualTo("Another");
  }

  @Configuration(proxyBeanMethods = false)
  static class TestConfig {

    @Bean
    CustomQualifierExampleService service() {
      return new CustomQualifierExampleService();
    }

    @Bean
    ExampleService anotherService() {
      return new RealExampleService("Another");
    }

    @Bean
    ExampleServiceCaller controller(@CustomQualifier ExampleService service) {
      return new ExampleServiceCaller(service);
    }

  }

}
