/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.properties;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.properties.ConfigurationPropertiesBean.BindMethod;

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

  private BeanDefinitionRegistry registry = new StandardBeanFactory();

  private ConfigurationPropertiesBeanRegistrar registrar = new ConfigurationPropertiesBeanRegistrar(
          new BootstrapContext(registry, null));

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
    assertThat(definition).satisfies(configurationPropertiesBeanDefinition(BindMethod.VALUE_OBJECT));
  }

  @Test
  void registerWhenNotValueObjectRegistersRootBeanDefinitionWithJavaBeanBindMethod() {
    String beanName = MultiConstructorBeanConfigurationProperties.class.getName();
    this.registrar.register(MultiConstructorBeanConfigurationProperties.class);
    BeanDefinition definition = this.registry.getBeanDefinition(beanName);
    assertThat(definition).satisfies(configurationPropertiesBeanDefinition(BindMethod.JAVA_BEAN));
  }

  private Consumer<BeanDefinition> configurationPropertiesBeanDefinition(BindMethod bindMethod) {
    return (definition) -> {
      assertThat(definition).isExactlyInstanceOf(RootBeanDefinition.class);
      assertThat(definition.hasAttribute(BindMethod.class.getName())).isTrue();
      assertThat(definition.getAttribute(BindMethod.class.getName())).isEqualTo(bindMethod);
    };
  }

  @ConfigurationProperties(prefix = "beancp")
  static class BeanConfigurationProperties {

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
