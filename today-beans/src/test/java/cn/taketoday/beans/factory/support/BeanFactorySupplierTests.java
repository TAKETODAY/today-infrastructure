/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.util.function.ThrowingSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link AbstractAutowireCapableBeanFactory} instance supplier
 * support.
 *
 * @author Phillip Webb
 */
public class BeanFactorySupplierTests {

  @Test
  void getBeanWhenUsingRegularSupplier() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setInstanceSupplier(() -> "I am supplied");
    beanFactory.registerBeanDefinition("test", beanDefinition);
    assertThat(beanFactory.getBean("test")).isEqualTo("I am supplied");
  }

  @Test
  void getBeanWithInnerBeanUsingRegularSupplier() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setInstanceSupplier(() -> "I am supplied");
    RootBeanDefinition outerBean = new RootBeanDefinition(String.class);
    outerBean.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition);
    beanFactory.registerBeanDefinition("test", outerBean);
    assertThat(beanFactory.getBean("test")).asString().startsWith("I am supplied");
  }

  @Test
  void getBeanWhenUsingInstanceSupplier() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    beanDefinition.setInstanceSupplier(InstanceSupplier.of(registeredBean ->
            "I am bean " + registeredBean.getBeanName() + " of " + registeredBean.getBeanClass()));
    beanFactory.registerBeanDefinition("test", beanDefinition);
    assertThat(beanFactory.getBean("test")).isEqualTo("I am bean test of class java.lang.String");
  }

  @Test
  void getBeanWithInnerBeanUsingInstanceSupplier() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition(String.class);
    beanDefinition.setInstanceSupplier(InstanceSupplier.of(registeredBean ->
            "I am bean " + registeredBean.getBeanName() + " of " + registeredBean.getBeanClass()));
    RootBeanDefinition outerBean = new RootBeanDefinition(String.class);
    outerBean.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition);
    beanFactory.registerBeanDefinition("test", outerBean);
    assertThat(beanFactory.getBean("test")).asString()
            .startsWith("I am bean (inner bean)")
            .endsWith(" of class java.lang.String");
  }

  @Test
  void getBeanWhenUsingThrowableSupplier() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setInstanceSupplier(ThrowingSupplier.of(() -> "I am supplied"));
    beanFactory.registerBeanDefinition("test", beanDefinition);
    assertThat(beanFactory.getBean("test")).isEqualTo("I am supplied");
  }

  @Test
  void getBeanWithInnerBeanUsingThrowableSupplier() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setInstanceSupplier(ThrowingSupplier.of(() -> "I am supplied"));
    RootBeanDefinition outerBean = new RootBeanDefinition(String.class);
    outerBean.getConstructorArgumentValues().addGenericArgumentValue(beanDefinition);
    beanFactory.registerBeanDefinition("test", outerBean);
    assertThat(beanFactory.getBean("test")).asString().startsWith("I am supplied");
  }

  @Test
  void getBeanWhenUsingThrowableSupplierThatThrowsCheckedException() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setInstanceSupplier(ThrowingSupplier.of(() -> {
      throw new IOException("fail");
    }));
    beanFactory.registerBeanDefinition("test", beanDefinition);
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> beanFactory.getBean("test"))
            .withCauseInstanceOf(IOException.class);
  }

  @Test
  void getBeanWhenUsingThrowableSupplierThatThrowsRuntimeException() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setInstanceSupplier(ThrowingSupplier.of(() -> {
      throw new IllegalStateException("fail");
    }));
    beanFactory.registerBeanDefinition("test", beanDefinition);
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> beanFactory.getBean("test"))
            .withCauseInstanceOf(IllegalStateException.class);
  }

}
