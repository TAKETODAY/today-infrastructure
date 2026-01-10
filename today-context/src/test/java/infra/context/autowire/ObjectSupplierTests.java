/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
