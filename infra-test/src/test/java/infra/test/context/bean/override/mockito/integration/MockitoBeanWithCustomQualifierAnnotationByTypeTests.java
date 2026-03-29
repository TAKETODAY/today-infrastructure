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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.ExampleServiceCaller;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsMock;
import static infra.test.mockito.MockitoAssertions.assertMockName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MockitoBean @MockitoBean} where the mocked bean is associated
 * with a custom {@link Qualifier @Qualifier} annotation and the bean to override
 * is selected by type.
 *
 * @author Sam Brannen
 * @see MockitoBeanWithCustomQualifierAnnotationByNameTests
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
class MockitoBeanWithCustomQualifierAnnotationByTypeTests {

  @MockitoBean(enforceOverride = true)
  @MyQualifier
  ExampleService service;

  @Autowired
  ExampleServiceCaller caller;

  @Test
  void test(ApplicationContext context) {
    assertIsMock(service);
    assertMockName(service, "qualifiedService");
    assertThat(service).isNotInstanceOf(QualifiedService.class);

    // Since the 'service' field's type is ExampleService, the QualifiedService
    // bean in the @Configuration class effectively gets removed from the context,
    // or rather it never gets created because we register an ExampleService as
    // a manual singleton in its place.
    assertThat(context.getBeanNamesForType(QualifiedService.class)).isEmpty();
    assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(1);

    when(service.greeting()).thenReturn("mock!");
    assertThat(caller.sayGreeting()).isEqualTo("I say mock!");
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    QualifiedService qualifiedService() {
      return new QualifiedService();
    }

    @Bean
    ExampleServiceCaller myServiceCaller(@MyQualifier ExampleService service) {
      return new ExampleServiceCaller(service);
    }
  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @interface MyQualifier {
  }

  @MyQualifier
  static class QualifiedService implements ExampleService {

    @Override
    public String greeting() {
      return "Qualified service";
    }
  }

}
