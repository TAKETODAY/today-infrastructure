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

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.support.BeanFactoryAwareInstantiator;
import cn.taketoday.context.support.StandardApplicationContext;
import lombok.ToString;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/9/5 18:22
 */
class BeanUtilsTests {

  static class TestNewInstanceBean { }

  @ToString
  static class TestNewInstanceBeanProvidedArgs {
    Integer integer;

    TestNewInstanceBeanProvidedArgs(Integer integer) {
      this.integer = integer;
    }
  }

  @Test
  void newInstance() {
    TestNewInstanceBean testNewInstanceBean = BeanUtils.newInstance(TestNewInstanceBean.class);
    assertThat(testNewInstanceBean).isNotNull();

    try (StandardApplicationContext context = new StandardApplicationContext()) {
      context.refresh();
      TestNewInstanceBeanProvidedArgs providedArgs =
              BeanFactoryAwareInstantiator.instantiate(
                      TestNewInstanceBeanProvidedArgs.class, context, new Object[] { 1, "TODAY" });

      assertThat(providedArgs).isNotNull();
      assertThat(providedArgs.integer).isNotNull().isEqualTo(1);
    }
  }
}
