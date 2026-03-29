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

package infra.test.context.bean.override.mockito;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.TestInstantiationAwareExtension.ExtensionContextScope;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;
import infra.test.context.bean.override.example.ExampleService;
import infra.test.context.bean.override.example.RealExampleService;
import infra.test.context.junit.jupiter.InfraExtensionConfig;
import infra.test.context.junit.jupiter.JUnitConfig;
import infra.test.mockito.MockitoAssertions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link MockitoSpyBean} that use by-name lookup with test class
 * {@link ExtensionContextScope}.
 *
 * @author Simon Baslé
 * @author Sam Brannen
 * @since 5.0
 */
@JUnitConfig
@InfraExtensionConfig(useTestClassScopedExtensionContext = true)
public class MockitoSpyBeanByNameLookupTestClassScopedExtensionContextIntegrationTests {

  @MockitoSpyBean("field1")
  ExampleService field;

  @MockitoSpyBean("field3")
  ExampleService prototypeScoped;

  @Test
  void fieldHasOverride(ApplicationContext ctx) {
    assertThat(ctx.getBean("field1"))
            .isInstanceOf(ExampleService.class)
            .satisfies(MockitoAssertions::assertIsSpy)
            .isSameAs(field);

    assertThat(field.greeting()).isEqualTo("bean1");
  }

  @Test
  void fieldForPrototypeHasOverride(ConfigurableApplicationContext ctx) {
    assertThat(ctx.getBean("field3"))
            .isInstanceOf(ExampleService.class)
            .satisfies(MockitoAssertions::assertIsSpy)
            .isSameAs(prototypeScoped);
    assertThat(ctx.getBeanFactory().getBeanDefinition("field3").isSingleton()).as("isSingleton").isTrue();

    assertThat(prototypeScoped.greeting()).isEqualTo("bean3");
  }

  @Nested
  @DisplayName("With @MockitoSpyBean in enclosing class and in @Nested class")
  public class MockitoSpyBeanNestedTests {

    @Autowired
    @Qualifier("field1")
    ExampleService localField;

    @MockitoSpyBean("field2")
    ExampleService nestedField;

    @Test
    void fieldHasOverride(ApplicationContext ctx) {
      assertThat(ctx.getBean("field1"))
              .isInstanceOf(ExampleService.class)
              .satisfies(MockitoAssertions::assertIsSpy)
              .isSameAs(localField);

      assertThat(localField.greeting()).isEqualTo("bean1");
    }

    @Test
    void nestedFieldHasOverride(ApplicationContext ctx) {
      assertThat(ctx.getBean("field2"))
              .isInstanceOf(ExampleService.class)
              .satisfies(MockitoAssertions::assertIsSpy)
              .isSameAs(nestedField);

      assertThat(nestedField.greeting()).isEqualTo("bean2");
    }
  }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean("field1")
    ExampleService bean1() {
      return new RealExampleService("bean1");
    }

    @Bean("field2")
    ExampleService bean2() {
      return new RealExampleService("bean2");
    }

    @Bean("field3")
    @Scope("prototype")
    ExampleService bean3() {
      return new RealExampleService("bean3");
    }
  }

}
