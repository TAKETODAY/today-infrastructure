/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.beans.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.support.DependencyInjector;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.support.StandardApplicationContext;
import lombok.Data;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/8/22 22:03
 */
class ParameterHandlerTests {

  @Data
  static class ParameterHandlerBean {

    int age;
  }

  public void test(ParameterHandlerBean bean) {
    System.out.println(bean);
  }

  @Test
  void resolveParameter() throws NoSuchMethodException {

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.register(ParameterHandlerBean.class);
      context.refresh();

      StandardBeanFactory beanFactory = context.getBeanFactory();

      DependencyInjector argumentsResolver = new DependencyInjector(beanFactory);
      Method test = ParameterHandlerTests.class.getDeclaredMethod("test", ParameterHandlerBean.class);

      Object[] args = argumentsResolver.resolveArguments(test);
      assertThat(args).hasSize(1);
      assertThat(args[0]).isInstanceOf(ParameterHandlerBean.class);
    }
  }

}
