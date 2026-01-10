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

package infra.beans.factory.support;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import infra.beans.factory.config.DependencyDescriptor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/10/29 16:42
 */
class DependencyInjectorTests {

  public void test(ParameterHandlerBean bean) {
    System.out.println(bean);
  }

  public ParameterHandlerBean inject(ParameterHandlerBean bean) {
    return bean;
  }

  @Test
  void resolveArguments() throws NoSuchMethodException {

    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("parameterHandlerBean",
            BeanDefinitionBuilder.rootBeanDefinition(ParameterHandlerBean.class).getBeanDefinition());

    DependencyInjector injector = new DependencyInjector(beanFactory);
    Method test = DependencyInjectorTests.class.getDeclaredMethod("test", ParameterHandlerBean.class);
    Method inject = DependencyInjectorTests.class.getDeclaredMethod("inject", ParameterHandlerBean.class);

    Object[] args = injector.resolveArguments(test);
    assertThat(args).hasSize(1);
    assertThat(args[0]).isInstanceOf(ParameterHandlerBean.class);

    Object inject1 = injector.inject(inject, new DependencyInjectorTests());
    assertThat(inject1).isSameAs(args[0]);
  }

  @Test
  void resolveValue() {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    beanFactory.registerBeanDefinition("testBean", BeanDefinitionBuilder.rootBeanDefinition(TestBean.class).getBeanDefinition());
    AutowiredAnnotationBeanPostProcessor processor = new AutowiredAnnotationBeanPostProcessor();
    processor.setBeanFactory(beanFactory);
    beanFactory.addBeanPostProcessor(processor);

    TestBean testBean = beanFactory.getBean(TestBean.class);
    assertThat(testBean.intProperty.value).isEqualTo(1);
  }

  static class ParameterHandlerBean {

    public int age;
  }

  static class TestBean {

    @Autowired
    IntProperty intProperty;

  }

  static class Property<T> {
    public T value;

    void update(T value) {
      this.value = value;
    }

  }

  static class IntProperty extends Property<Integer> {
    public IntProperty(int value) {
      this.value = value;
    }
  }

  static class TestDependencyResolvingStrategy implements DependencyResolvingStrategy {

    @Nullable
    @Override
    public Object resolveDependency(DependencyDescriptor descriptor, Context context) {
      if (descriptor.getDependencyType() == IntProperty.class) {
        return new IntProperty(1);
      }
      return null;
    }
  }

}