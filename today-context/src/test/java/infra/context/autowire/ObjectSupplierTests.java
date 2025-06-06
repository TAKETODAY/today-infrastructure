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

package infra.context.autowire;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import infra.beans.factory.ObjectProvider;
import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/3/6 13:24
 */
class ObjectProviderTests {

  static class Bean {

  }

  static class TEST {

    @Autowired
    ObjectProvider<Bean> beanObjectProvider;

    TEST(ObjectProvider<Bean> beanObjectProvider, Supplier<Bean> supplier) {

    }

  }

  static class ResolvableTypeTEST {

    @Autowired
    ObjectProvider<List<Bean>> beanObjectProvider;

    ResolvableTypeTEST(
            ObjectProvider<List<Bean>> beanObjectProvider, Supplier<List<Bean>> supplier) {

    }

  }

  @Test
  public void testProperty() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    context.register(Bean.class, TEST.class);
    context.refresh();

    TEST test = context.getBean(TEST.class);
    Bean bean = context.getBean(Bean.class);

    assertThat(test.beanObjectProvider.get())
            .isEqualTo(bean);

  }

}
