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

import infra.beans.factory.FactoryBean;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.BootstrapContext;
import infra.context.annotation.Configuration;
import infra.context.annotation.Import;
import infra.context.annotation.ImportBeanDefinitionRegistrar;
import infra.core.ResolvableType;
import infra.core.type.AnnotationMetadata;
import infra.test.context.bean.override.example.ExampleGenericService;
import infra.test.context.bean.override.example.StringExampleGenericService;
import infra.test.context.bean.override.mockito.MockitoSpyBean;
import infra.test.context.junit.jupiter.JUnitConfig;

import static infra.test.mockito.MockitoAssertions.assertIsSpy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockingDetails;

/**
 * Tests that {@link MockitoSpyBean @MockitoSpyBean} on a field with generics can
 * be used to replace an existing bean with matching generics that's produced by a
 * {@link FactoryBean} that's programmatically registered via an
 * {@link ImportBeanDefinitionRegistrar}.
 *
 * @author Andy Wilkinson
 * @author Sam Brannen
 * @see MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanIntegrationTests
 * @since 5.0
 */
@JUnitConfig
class MockitoSpyBeanWithGenericsOnTestFieldForExistingGenericBeanProducedByFactoryBeanIntegrationTests {

  @MockitoSpyBean("exampleService")
  ExampleGenericService<String> exampleService;

  @Test
  void spying() {
    assertIsSpy(exampleService);

    Object spiedInstance = mockingDetails(exampleService).getMockCreationSettings().getSpiedInstance();
    assertThat(spiedInstance).isInstanceOf(StringExampleGenericService.class);
  }

  @Configuration(proxyBeanMethods = false)
  @Import(FactoryBeanRegistrar.class)
  static class Config {
  }

  static class FactoryBeanRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importMetadata, BootstrapContext context) {
      RootBeanDefinition definition = new RootBeanDefinition(ExampleGenericServiceFactoryBean.class);
      ResolvableType targetType = ResolvableType.forClassWithGenerics(
              ExampleGenericServiceFactoryBean.class, Object.class, ExampleGenericService.class);
      definition.setTargetType(targetType);
      context.registerBeanDefinition("exampleService", definition);
    }
  }

  static class ExampleGenericServiceFactoryBean<T, U extends ExampleGenericService<T>> implements FactoryBean<U> {

    @Override
    @SuppressWarnings("unchecked")
    public U getObject() throws Exception {
      return (U) new StringExampleGenericService("Enigma");
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class<ExampleGenericService> getObjectType() {
      return ExampleGenericService.class;
    }
  }

}
