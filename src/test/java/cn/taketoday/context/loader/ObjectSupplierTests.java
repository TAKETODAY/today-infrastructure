/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.context.loader;

import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.function.Supplier;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.factory.ObjectSupplier;
import cn.taketoday.context.utils.ClassUtils;
import lombok.ToString;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/6 13:24
 */
public class ObjectSupplierTests {

  @ToString
  static class Bean {

  }

  @ToString
  static class TEST {

    @Autowired
    ObjectSupplier<Bean> beanObjectSupplier;

    TEST(ObjectSupplier<Bean> beanObjectSupplier, Supplier<Bean> supplier) {

    }

  }

  @Test
  public void testProperty() throws Throwable {
    try (ConfigurableApplicationContext context = new StandardApplicationContext(new HashSet<>())) {
      context.importBeans(Bean.class, TEST.class);

      final TEST test = context.getBean(TEST.class);
      final Bean bean = context.getBean(Bean.class);

      assertThat(test.beanObjectSupplier.get())
              .isEqualTo(bean);

    }
  }

  @Test
  public void testParameter() throws Throwable {
    final Constructor<TEST> constructor = ClassUtils.getSuitableConstructor(TEST.class);
    final Parameter[] parameters = constructor.getParameters();

    ObjectSupplierParameterResolver parameterResolver = new ObjectSupplierParameterResolver();

    try (ConfigurableApplicationContext context = new StandardApplicationContext(new HashSet<>())) {
      context.importBeans(Bean.class);

      final ObjectSupplier<?> supplier = parameterResolver.resolve(parameters[0], context);

      final ObjectSupplier<?> supplier1 = parameterResolver.resolve(parameters[1], context);
      final Bean bean = context.getBean(Bean.class);

      assertThat(bean)
              .isEqualTo(supplier.get())
              .isNotNull()
      ;

      assertThat(bean)
              .isEqualTo(supplier1.get())
              .isNotNull()
      ;

      assertThat(supplier1).isEqualTo(supplier);

    }
  }

}
