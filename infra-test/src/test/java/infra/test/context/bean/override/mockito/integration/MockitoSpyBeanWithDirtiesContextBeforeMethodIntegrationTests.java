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

import org.junit.jupiter.api.RepeatedTest;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.test.annotation.DirtiesContext;
import infra.test.annotation.DirtiesContext.MethodMode;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.ExampleServiceCaller;
import infra.test.context.bean.override.example.SimpleExampleService;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.annotation.DirtiesContext.MethodMode.BEFORE_METHOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

/**
 * Integration tests for using {@link MockitoSpyBean @MockitoSpyBean} with
 * {@link DirtiesContext @DirtiesContext} and {@link MethodMode#BEFORE_METHOD}.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @see MockitoBeanWithDirtiesContextBeforeMethodIntegrationTests
 * @since 5.0
 */
@JUnitConfig
class MockitoSpyBeanWithDirtiesContextBeforeMethodIntegrationTests {

  @Autowired
  ExampleServiceCaller caller;

  @MockitoSpyBean
  SimpleExampleService service;

  @Autowired
  ExampleService autowiredService;

  @RepeatedTest(2)
  @DirtiesContext(methodMode = BEFORE_METHOD)
  void spying() {
    assertThat(service).isSameAs(autowiredService);

    assertThat(caller.sayGreeting()).isEqualTo("I say simple");
    then(service).should().greeting();
  }

  @Configuration(proxyBeanMethods = false)
  @Import(SimpleExampleService.class)
  static class Config {

    @Bean
    ExampleServiceCaller serviceCaller(ExampleService service) {
      return new ExampleServiceCaller(service);
    }
  }

}
