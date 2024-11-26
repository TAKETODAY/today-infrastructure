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

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import infra.aop.scope.ScopedProxyFactoryBean;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.GenericBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.BootstrapContext;
import infra.context.annotation.Primary;
import infra.context.annotation.Scope;
import infra.context.annotation.ScopedProxyMode;
import infra.context.properties.bind.BindMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConfigurationPropertiesBeanRegistrar}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class ConfigurationPropertiesBeanRegistrarTests {

  private StandardBeanFactory registry = new StandardBeanFactory();

  private ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(
          new BootstrapContext(null, registry));

  @Test
  void registerWhenNotAlreadyRegisteredAddBeanDefinition() {
    String beanName = "beancp-" + BeanConfigurationProperties.class.getName();
    this.registrar.register(BeanConfigurationProperties.class);
    BeanDefinition definition = this.registry.getBeanDefinition(beanName);
    assertThat(definition).isNotNull();
    assertThat(definition.getBeanClassName()).isEqualTo(BeanConfigurationProperties.class.getName());
  }

  @Test
  void registerWhenAlreadyContainsNameDoesNotReplace() {
    String beanName = "beancp-" + BeanConfigurationProperties.class.getName();
    this.registry.registerBeanDefinition(beanName, new GenericBeanDefinition());
    this.registrar.register(BeanConfigurationProperties.class);
    BeanDefinition definition = this.registry.getBeanDefinition(beanName);
    assertThat(definition).isNotNull();
    assertThat(definition.getBeanClassName()).isNull();
  }

  @Test
  void registerWhenNoAnnotationThrowsException() {
    assertThatIllegalStateException()
            .isThrownBy(() -> this.registrar.register(NoAnnotationConfigurationProperties.class))
            .withMessageContaining("No ConfigurationProperties annotation found");
  }

  @Test
  void registerWhenValueObjectRegistersValueObjectBeanDefinition() {
    String beanName = "valuecp-" + ValueObjectConfigurationProperties.class.getName();
    this.registrar.register(ValueObjectConfigurationProperties.class);
    BeanDefinition definition = this.registry.getBeanDefinition(beanName);
    assertThat(definition).satisfies(hasBindMethodAttribute(BindMethod.VALUE_OBJECT));
  }

  @Test
  void registerWhenNotValueObjectRegistersRootBeanDefinitionWithJavaBeanBindMethod() {
    String beanName = MultiConstructorBeanConfigurationProperties.class.getName();
    this.registrar.register(MultiConstructorBeanConfigurationProperties.class);
    BeanDefinition definition = this.registry.getBeanDefinition(beanName);
    assertThat(definition).satisfies(hasBindMethodAttribute(BindMethod.JAVA_BEAN));
  }

  @Test
  void registerWhenNoScopeUsesSingleton() {
    String beanName = "beancp-" + BeanConfigurationProperties.class.getName();
    this.registrar.register(BeanConfigurationProperties.class);
    BeanDefinition definition = this.registry.getBeanDefinition(beanName);
    assertThat(definition).isNotNull();
    assertThat(definition.getScope()).isEqualTo(BeanDefinition.SCOPE_SINGLETON);
  }

  @Test
  void registerScopedBeanDefinition() {
    String beanName = "beancp-" + ScopedBeanConfigurationProperties.class.getName();
    this.registrar.register(ScopedBeanConfigurationProperties.class);
    BeanDefinition beanDefinition = this.registry.getBeanDefinition(beanName);
    assertThat(beanDefinition).isNotNull();
    assertThat(beanDefinition.getBeanClassName()).isEqualTo(ScopedBeanConfigurationProperties.class.getName());
    assertThat(beanDefinition.getScope()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
  }

  @Test
  void registerScopedBeanDefinitionWithProxyMode() {
    String beanName = "beancp-" + ProxyScopedBeanConfigurationProperties.class.getName();
    this.registrar.register(ProxyScopedBeanConfigurationProperties.class);
    BeanDefinition proxiedBeanDefinition = this.registry.getBeanDefinition(beanName);
    assertThat(proxiedBeanDefinition).isNotNull();
    assertThat(proxiedBeanDefinition.getBeanClassName()).isEqualTo(ScopedProxyFactoryBean.class.getName());
    String targetBeanName = (String) proxiedBeanDefinition.getPropertyValues().getPropertyValue("targetBeanName");
    assertThat(targetBeanName).isNotNull();
    BeanDefinition beanDefinition = this.registry.getBeanDefinition(targetBeanName);
    assertThat(beanDefinition).isNotNull();
    assertThat(beanDefinition.getBeanClassName()).isEqualTo(ProxyScopedBeanConfigurationProperties.class.getName());
    assertThat(beanDefinition.getScope()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
  }

  @Test
  void registerBeanDefinitionWithCommonDefinitionAnnotations() {
    String beanName = "beancp-" + PrimaryConfigurationProperties.class.getName();
    this.registrar.register(PrimaryConfigurationProperties.class);
    BeanDefinition beanDefinition = this.registry.getBeanDefinition(beanName);
    assertThat(beanDefinition).isNotNull();
    assertThat(beanDefinition.isPrimary()).isEqualTo(true);
  }

  private Consumer<BeanDefinition> hasBindMethodAttribute(BindMethod bindMethod) {
    return (definition) -> {
      assertThat(definition.hasAttribute(BindMethod.class.getName())).isTrue();
      assertThat(definition.getAttribute(BindMethod.class.getName())).isEqualTo(bindMethod);
    };
  }

  @ConfigurationProperties(prefix = "beancp")
  static class BeanConfigurationProperties {

  }

  @ConfigurationProperties(prefix = "beancp")
  @Scope(BeanDefinition.SCOPE_PROTOTYPE)
  static class ScopedBeanConfigurationProperties {

  }

  @ConfigurationProperties(prefix = "beancp")
  @Scope(scopeName = BeanDefinition.SCOPE_PROTOTYPE, proxyMode = ScopedProxyMode.TARGET_CLASS)
  static class ProxyScopedBeanConfigurationProperties {

  }

  @ConfigurationProperties(prefix = "beancp")
  @Primary
  static class PrimaryConfigurationProperties {

  }

  static class NoAnnotationConfigurationProperties {

  }

  @ConfigurationProperties("valuecp")
  static class ValueObjectConfigurationProperties {

    ValueObjectConfigurationProperties(String name) {
    }

  }

  @ConfigurationProperties
  static class MultiConstructorBeanConfigurationProperties {

    MultiConstructorBeanConfigurationProperties() {
    }

    MultiConstructorBeanConfigurationProperties(String name) {
    }

  }

}
