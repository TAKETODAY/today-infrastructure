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

import infra.context.ApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;
import infra.test.context.aot.DisabledInAotMode;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.ExampleServiceCaller;
import infra.test.context.bean.override.mockito.MockitoBean;
import infra.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests which verify that {@link MockitoBean @MockitoBean} and
 * {@link MockitoSpyBean @MockitoSpyBean} can be used within a
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @see MockitoBeanAndContextHierarchyParentIntegrationTests
 * @since 5.0
 */
@ContextHierarchy(@ContextConfiguration)
@DisabledInAotMode("@ContextHierarchy is not supported in AOT")
public class MockitoSpyBeanAndContextHierarchyChildIntegrationTests extends
        MockitoBeanAndContextHierarchyParentIntegrationTests {

  @MockitoSpyBean
  ExampleServiceCaller serviceCaller;

  @Test
  @Override
  void test(ApplicationContext context) {
    ApplicationContext parentContext = context.getParent();
    assertThat(parentContext).as("parent ApplicationContext").isNotNull();
    assertThat(parentContext.getParent()).as("grandparent ApplicationContext").isNull();

    assertThat(parentContext.getBeanNamesForType(ExampleService.class)).hasSize(1);
    assertThat(parentContext.getBeanNamesForType(ExampleServiceCaller.class)).isEmpty();

    assertThat(context.getBeanNamesForType(ExampleService.class)).hasSize(1);
    assertThat(context.getBeanNamesForType(ExampleServiceCaller.class)).hasSize(1);

    assertThat(service.greeting()).isEqualTo("mock");
    assertThat(serviceCaller.sayGreeting()).isEqualTo("I say mock");
  }

  @Configuration(proxyBeanMethods = false)
  static class ChildConfig {

    @Bean
    ExampleServiceCaller serviceCaller(ExampleService service) {
      return new ExampleServiceCaller(service);
    }
  }

}
