/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.lang.Nullable;
import lombok.Data;

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

  @Data
  static class ParameterHandlerBean {

    int age;
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