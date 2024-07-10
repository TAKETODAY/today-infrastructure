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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.annotation.Gh23206Tests.ConditionalConfiguration.NestedConfiguration;
import cn.taketoday.context.annotation.componentscan.simple.SimpleComponent;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for gh-23206.
 *
 * @author Stephane Nicoll
 */
public class Gh23206Tests {

  @Test
  void componentScanShouldFailWithRegisterBeanCondition() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(ConditionalComponentScanConfiguration.class);
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(context::refresh)
            .withMessageContaining(ConditionalComponentScanConfiguration.class.getName())
            .havingCause().isInstanceOf(ApplicationContextException.class)
            .withMessageStartingWith("Component scan for configuration class [")
            .withMessageContaining(ConditionalComponentScanConfiguration.class.getName())
            .withMessageContaining("could not be used with conditions in REGISTER_BEAN phase");
  }

  @Test
  void componentScanShouldFailWithRegisterBeanConditionOnClasThatImportedIt() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(ConditionalConfiguration.class);
    assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(context::refresh)
            .withMessageContaining(ConditionalConfiguration.class.getName())
            .havingCause().isInstanceOf(ApplicationContextException.class)
            .withMessageStartingWith("Component scan for configuration class [")
            .withMessageContaining(NestedConfiguration.class.getName())
            .withMessageContaining("could not be used with conditions in REGISTER_BEAN phase");
  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(NeverRegisterBeanCondition.class)
  @ComponentScan(basePackageClasses = SimpleComponent.class)
  static class ConditionalComponentScanConfiguration {

  }

  @Configuration(proxyBeanMethods = false)
  @Conditional(NeverRegisterBeanCondition.class)
  static class ConditionalConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(basePackageClasses = SimpleComponent.class)
    static class NestedConfiguration {
    }

  }

  static class NeverRegisterBeanCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }

  }
}
