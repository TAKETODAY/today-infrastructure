/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/5/2 21:20
 */
class LazyInitializationBeanFactoryPostProcessorTests {

  @Test
  void whenLazyInitializationIsEnabledThenNormalBeansAreNotInitializedUntilRequired() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
      context.register(BeanState.class, ExampleBean.class);
      context.refresh();
      BeanState beanState = context.getBean(BeanState.class);
      assertThat(beanState.initializedBeans).isEmpty();
      context.getBean(ExampleBean.class);
      assertThat(beanState.initializedBeans).containsExactly(ExampleBean.class);
    }
  }

  @Test
  void whenLazyInitializationIsEnabledThenSmartInitializingSingletonsAreInitializedDuringRefresh() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
      context.register(BeanState.class, ExampleSmartInitializingSingleton.class);
      context.refresh();
      BeanState beanState = context.getBean(BeanState.class);
      assertThat(beanState.initializedBeans).containsExactly(ExampleSmartInitializingSingleton.class);
      assertThat(context.getBean(ExampleSmartInitializingSingleton.class).callbackInvoked).isTrue();
    }
  }

  static class ExampleBean {

    ExampleBean(BeanState beanState) {
      beanState.initializedBeans.add(getClass());
    }

  }

  static class ExampleSmartInitializingSingleton implements SmartInitializingSingleton {

    private boolean callbackInvoked;

    ExampleSmartInitializingSingleton(BeanState beanState) {
      beanState.initializedBeans.add(getClass());
    }

    @Override
    public void afterSingletonsInstantiated() {
      this.callbackInvoked = true;
    }

  }

  static class BeanState {

    private final List<Class<?>> initializedBeans = new ArrayList<>();

  }

}