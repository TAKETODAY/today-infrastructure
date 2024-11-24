/*
 * Copyright 2017 - 2024 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.context.properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Consumer;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;
import infra.context.properties.ConfigurationProperties;
import infra.context.properties.EnableConfigurationProperties;
import infra.context.properties.EnableConfigurationPropertiesRegistrar;
import infra.context.properties.bind.BindMethod;
import infra.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link EnableConfigurationPropertiesRegistrar}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
class EnableConfigurationPropertiesRegistrarTests {

  private StandardBeanFactory beanFactory;

  private EnableConfigurationPropertiesRegistrar registrar;

  @BeforeEach
  void setup() {
    this.beanFactory = spy(new StandardBeanFactory());
    this.registrar = new EnableConfigurationPropertiesRegistrar();
  }

  @Test
  void typeWithDefaultConstructorShouldRegisterRootBeanDefinition() {
    register(TestConfiguration.class);
    BeanDefinition definition = this.beanFactory
            .getBeanDefinition("foo-" + getClass().getName() + "$FooProperties");
    assertThat(definition).satisfies(hasBindMethod(BindMethod.JAVA_BEAN));
  }

  @Test
  void constructorBoundPropertiesShouldRegisterConfigurationPropertiesBeanDefinition() {
    register(TestConfiguration.class);
    BeanDefinition definition = this.beanFactory
            .getBeanDefinition("bar-" + getClass().getName() + "$BarProperties");
    assertThat(definition).satisfies(hasBindMethod(BindMethod.VALUE_OBJECT));
  }

  @Test
  void typeWithMultipleConstructorsShouldRegisterGenericBeanDefinition() {
    register(TestConfiguration.class);
    BeanDefinition definition = this.beanFactory
            .getBeanDefinition("bing-" + getClass().getName() + "$BingProperties");
    assertThat(definition).satisfies(hasBindMethod(BindMethod.JAVA_BEAN));
  }

  @Test
  void typeWithNoAnnotationShouldFail() {
    assertThatIllegalStateException().isThrownBy(() -> register(InvalidConfiguration.class))
            .withMessageContaining("No ConfigurationProperties annotation found")
            .withMessageContaining(EnableConfigurationPropertiesRegistrar.class.getName());
  }

  @Test
  void registrationWithDuplicatedTypeShouldRegisterSingleBeanDefinition() {
    register(DuplicateConfiguration.class);
    String name = "foo-" + getClass().getName() + "$FooProperties";
    then(this.beanFactory).should().registerBeanDefinition(eq(name), any());
  }

  @Test
  void registrationWithNoTypeShouldNotRegisterAnything() {
    register(EmptyConfiguration.class);
    Set<String> names = this.beanFactory.getBeanNamesForType(Object.class);
    for (String name : names) {
      assertThat(name).doesNotContain("-");
    }
  }

  private Consumer<BeanDefinition> hasBindMethod(BindMethod bindMethod) {
    return (definition) -> {
      assertThat(definition.hasAttribute(BindMethod.class.getName())).isTrue();
      assertThat(definition.getAttribute(BindMethod.class.getName())).isEqualTo(bindMethod);
    };
  }

  private void register(Class<?> configuration) {
    AnnotationMetadata metadata = AnnotationMetadata.introspect(configuration);
    this.registrar.registerBeanDefinitions(metadata, new BootstrapContext(this.beanFactory, null));
  }

  @EnableConfigurationProperties({ FooProperties.class, BarProperties.class, BingProperties.class })
  static class TestConfiguration {

  }

  @EnableConfigurationProperties(EnableConfigurationPropertiesRegistrarTests.class)
  static class InvalidConfiguration {

  }

  @EnableConfigurationProperties({ FooProperties.class, FooProperties.class })
  static class DuplicateConfiguration {

  }

  @EnableConfigurationProperties
  static class EmptyConfiguration {

  }

  @ConfigurationProperties(prefix = "foo")
  static class FooProperties {

  }

  @ConfigurationProperties(prefix = "bar")
  static class BarProperties {

    BarProperties(String foo) {
    }

  }

  @ConfigurationProperties(prefix = "bing")
  static class BingProperties {

    BingProperties() {
    }

    BingProperties(String foo) {
    }

  }

}
