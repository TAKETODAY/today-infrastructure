/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.annotation.beanregistrar;

import org.junit.jupiter.api.Test;

import infra.beans.factory.BeanRegistrar;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.testfixture.beans.factory.GenericBeanRegistrar;
import infra.context.testfixture.beans.factory.SampleBeanRegistrar.Bar;
import infra.context.testfixture.beans.factory.SampleBeanRegistrar.Baz;
import infra.context.testfixture.beans.factory.SampleBeanRegistrar.Foo;
import infra.context.testfixture.beans.factory.SampleBeanRegistrar.Init;
import infra.context.testfixture.context.annotation.registrar.BeanRegistrarConfiguration;
import infra.context.testfixture.context.annotation.registrar.GenericBeanRegistrarConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BeanRegistrar} imported by @{@link infra.context.annotation.Configuration}.
 *
 * @author Sebastien Deleuze
 */
public class BeanRegistrarConfigurationTests {

  @Test
  void beanRegistrar() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BeanRegistrarConfiguration.class);
    assertThat(context.getBean(Bar.class).foo()).isEqualTo(context.getBean(Foo.class));
    assertThat(context.getBean("foo", Foo.class)).isEqualTo(context.getBean("fooAlias", Foo.class));
    assertThatThrownBy(() -> context.getBean(Baz.class)).isInstanceOf(NoSuchBeanDefinitionException.class);
    assertThat(context.getBean(Init.class).initialized).isTrue();
    BeanDefinition beanDefinition = context.getBeanDefinition("bar");
    assertThat(beanDefinition.getScope()).isEqualTo(BeanDefinition.SCOPE_PROTOTYPE);
    assertThat(beanDefinition.isLazyInit()).isTrue();
    assertThat(beanDefinition.getDescription()).isEqualTo("Custom description");
  }

  @Test
  void beanRegistrarWithProfile() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(BeanRegistrarConfiguration.class);
    context.getEnvironment().addActiveProfile("baz");
    context.refresh();
    assertThat(context.getBean(Baz.class).message()).isEqualTo("Hello World!");
  }

  @Test
  void scannedFunctionalConfiguration() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.scan("infra.context.testfixture.context.annotation.registrar");
    context.refresh();
    assertThat(context.getBean(Bar.class).foo()).isEqualTo(context.getBean(Foo.class));
    assertThatThrownBy(() -> context.getBean(Baz.class).message()).isInstanceOf(NoSuchBeanDefinitionException.class);
    assertThat(context.getBean(Init.class).initialized).isTrue();
  }

  @Test
  void beanRegistrarWithTargetType() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(GenericBeanRegistrarConfiguration.class);
    context.refresh();
    RootBeanDefinition beanDefinition = (RootBeanDefinition) context.getBeanDefinition("fooSupplier");
    assertThat(beanDefinition.getResolvableType().resolveGeneric(0)).isEqualTo(GenericBeanRegistrar.Foo.class);
  }

}
