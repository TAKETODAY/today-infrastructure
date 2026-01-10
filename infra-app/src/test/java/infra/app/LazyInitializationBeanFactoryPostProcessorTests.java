/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.SmartInitializingSingleton;
import infra.beans.factory.config.BeanDefinition;
import infra.context.annotation.AnnotationConfigApplicationContext;

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

  @Test
  void whenLazyInitializationIsEnabledThenInfrastructureRoleBeansAreInitializedDuringRefresh() {
    try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
      context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
      context.register(BeanState.class);
      context.registerBean(ExampleBean.class,
              (definition) -> definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE));
      context.refresh();
      BeanState beanState = context.getBean(BeanState.class);
      assertThat(beanState.initializedBeans).containsExactly(ExampleBean.class);
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