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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0 2024/6/30 12:35
 */
class BeanSupplierTests {

  @Test
  void singleton() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", BeanDefinitionBuilder.rootBeanDefinition(BeanSupplierTests.class).getBeanDefinition());
    BeanSupplier<Object> supplier = BeanSupplier.from(beanFactory, "test");

    assertThat(supplier).isNotNull();
    assertThat(supplier.get()).isInstanceOf(BeanSupplierTests.class).isSameAs(supplier.get());

    assertThat(supplier.getBeanName()).isEqualTo("test");
    assertThat(supplier.getBeanType()).isSameAs(BeanSupplierTests.class);
    assertThat(supplier.getBeanFactory()).isSameAs(beanFactory);

    assertThat(supplier.isSingleton()).isTrue();
  }

  @Test
  void prototype() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("test", BeanDefinitionBuilder.rootBeanDefinition(BeanSupplierTests.class)
            .setScope("prototype").getBeanDefinition());
    BeanSupplier<Object> supplier = BeanSupplier.from(beanFactory, "test");

    assertThat(supplier).isNotNull();
    assertThat(supplier.get()).isInstanceOf(BeanSupplierTests.class).isNotSameAs(supplier.get());
    assertThat(supplier.isSingleton()).isFalse();
  }

}