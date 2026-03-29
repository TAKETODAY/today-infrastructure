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

package infra.test.context.bean.override.mockito.hierarchies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.ExampleServiceCaller;
import infra.test.context.bean.override.example.RealExampleService;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.InfraExtension;

import static infra.test.mockito.MockitoAssertions.assertIsSpy;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link MockitoSpyBean @MockitoSpyBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy} with named context levels, when
 * a bean is only spied on "by name" in the parent.
 *
 * @author Sam Brannen
 * @since 5.0
 */
@ExtendWith(InfraExtension.class)
@ContextHierarchy({
        @ContextConfiguration(classes = MockitoSpyBeanByNameInParentContextHierarchyTests.Config1.class, name = "parent"),
        @ContextConfiguration(classes = MockitoSpyBeanByNameInParentContextHierarchyTests.Config2.class)
})
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
class MockitoSpyBeanByNameInParentContextHierarchyTests {

  @MockitoSpyBean(name = "service", contextName = "parent")
  ExampleService service;

  @Autowired
  ExampleServiceCaller serviceCaller1;

  @Autowired
  ExampleServiceCaller serviceCaller2;

  @Test
  void test() {
    assertIsSpy(service);

    assertThat(service.greeting()).isEqualTo("Service 1");
    assertThat(serviceCaller1.getService()).isSameAs(service);
    assertThat(serviceCaller2.getService()).isSameAs(service);
    assertThat(serviceCaller1.sayGreeting()).isEqualTo("I say Service 1");
    assertThat(serviceCaller2.sayGreeting()).isEqualTo("I say Service 1");
  }

  @Configuration
  static class Config1 {

    @Bean
    ExampleService service() {
      return new RealExampleService("Service 1");
    }

    @Bean
    ExampleServiceCaller serviceCaller1(ExampleService service) {
      return new ExampleServiceCaller(service);
    }
  }

  @Configuration
  static class Config2 {

    @Bean
    ExampleServiceCaller serviceCaller2(ExampleService service) {
      return new ExampleServiceCaller(service);
    }
  }

}
