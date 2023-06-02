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

package cn.taketoday.context.autowire;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.support.StandardApplicationContext;
import lombok.ToString;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/6 13:24
 */
class ObjectProviderTests {

  @ToString
  static class Bean {

  }

  @ToString
  static class TEST {

    @Autowired
    ObjectProvider<Bean> beanObjectProvider;

    TEST(ObjectProvider<Bean> beanObjectProvider, Supplier<Bean> supplier) {

    }

  }

  @ToString
  static class ResolvableTypeTEST {

    @Autowired
    ObjectProvider<List<Bean>> beanObjectProvider;

    ResolvableTypeTEST(
            ObjectProvider<List<Bean>> beanObjectProvider, Supplier<List<Bean>> supplier) {

    }

  }

  @Test
  public void testProperty() {
    StandardApplicationContext context = new StandardApplicationContext();
    context.register(Bean.class, TEST.class);
    context.refresh();

    TEST test = context.getBean(TEST.class);
    Bean bean = context.getBean(Bean.class);

    assertThat(test.beanObjectProvider.get())
            .isEqualTo(bean);

  }

}
