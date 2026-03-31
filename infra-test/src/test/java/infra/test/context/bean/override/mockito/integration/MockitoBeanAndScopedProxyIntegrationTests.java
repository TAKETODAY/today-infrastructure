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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;
import infra.context.annotation.ScopedProxyMode;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.ExampleServiceCaller;
import infra.test.context.bean.override.example.FailingExampleService;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link MockitoBean @MockitoBean} used in combination with scoped-proxy
 * targets.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
public class MockitoBeanAndScopedProxyIntegrationTests {

  @MockitoBean
  // The ExampleService mock should replace the scoped-proxy FailingExampleService
  // created in the @Configuration class.
  ExampleService service;

  @Autowired
  ExampleServiceCaller serviceCaller;

  @BeforeEach
  void configureServiceMock() {
    given(service.greeting()).willReturn("mock");
  }

  @Test
  void mocking() {
    assertThat(serviceCaller.sayGreeting()).isEqualTo("I say mock");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    @Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
    ExampleService exampleService() {
      return new FailingExampleService();
    }

    @Bean
    ExampleServiceCaller serviceCaller(ExampleService service) {
      return new ExampleServiceCaller(service);
    }

  }

}
