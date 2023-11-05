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

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.lang.NullValue;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/11/5 15:21
 */
class InstantiationStrategyTests {

  private final StandardBeanFactory beanFactory = new StandardBeanFactory();

  private final InstantiationStrategy strategy = new InstantiationStrategy();

  @Test
  void instantiateWithNoArg() {
    RootBeanDefinition bd = new RootBeanDefinition(String.class);
    Object simpleBean = instantiate(bd, new SampleFactory(),
            method(SampleFactory.class, "simpleBean"));
    assertThat(simpleBean).isEqualTo("Hello");
  }

  @Test
  void instantiateWitArgs() {
    RootBeanDefinition bd = new RootBeanDefinition(String.class);
    Object simpleBean = instantiate(bd, new SampleFactory(),
            method(SampleFactory.class, "beanWithTwoArgs"), "Test", 42);
    assertThat(simpleBean).isEqualTo("Test42");
  }

  @Test
  void instantiateWitSubClassFactoryArgs() {
    RootBeanDefinition bd = new RootBeanDefinition(String.class);
    Object simpleBean = instantiate(bd, new ExtendedSampleFactory(),
            method(SampleFactory.class, "beanWithTwoArgs"), "Test", 42);
    assertThat(simpleBean).isEqualTo("42Test");
  }

  @Test
  void instantiateWithNullValueReturnsNullBean() {
    RootBeanDefinition bd = new RootBeanDefinition(String.class);
    Object simpleBean = instantiate(bd, new SampleFactory(),
            method(SampleFactory.class, "cloneBean"), new Object[] { null });
    assertThat(simpleBean).isNotNull().isInstanceOf(NullValue.class);
  }

  @Test
  void instantiateWithArgumentTypeMismatch() {
    RootBeanDefinition bd = new RootBeanDefinition(String.class);
    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() -> instantiate(
                    bd, new SampleFactory(),
                    method(SampleFactory.class, "beanWithTwoArgs"), 42, "Test"))
            .withMessageContaining("Illegal arguments to factory method 'beanWithTwoArgs'")
            .withMessageContaining("args: 42,Test");
  }

  @Test
  void instantiateWithTargetTypeMismatch() {
    RootBeanDefinition bd = new RootBeanDefinition(String.class);
    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() -> instantiate(
                    bd, new AnotherFactory(),
                    method(SampleFactory.class, "beanWithTwoArgs"), "Test", 42))
            .withMessageContaining("Illegal factory instance for factory method 'beanWithTwoArgs'")
            .withMessageContaining("instance: " + AnotherFactory.class.getName())
            .withMessageNotContaining("args: Test,42");
  }

  @Test
  void instantiateWithTargetTypeNotAssignable() {
    RootBeanDefinition bd = new RootBeanDefinition(String.class);
    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() -> instantiate(
                    bd, new SampleFactory(),
                    method(ExtendedSampleFactory.class, "beanWithTwoArgs"), "Test", 42))
            .withMessageContaining("Illegal factory instance for factory method 'beanWithTwoArgs'")
            .withMessageContaining("instance: " + SampleFactory.class.getName())
            .withMessageNotContaining("args: Test,42");
  }

  @Test
  void instantiateWithException() {
    RootBeanDefinition bd = new RootBeanDefinition(String.class);
    assertThatExceptionOfType(BeanInstantiationException.class).isThrownBy(() -> instantiate(
                    bd, new SampleFactory(),
                    method(SampleFactory.class, "errorBean"), "This a test message"))
            .withMessageContaining("Factory method 'errorBean' threw exception")
            .withMessageContaining("This a test message")
            .havingCause().isInstanceOf(IllegalStateException.class).withMessage("This a test message");
  }

  private Object instantiate(RootBeanDefinition bd, Object factory, Method method, Object... args) {
    return this.strategy.instantiate(bd, "simpleBean", this.beanFactory,
            factory, method, args);
  }

  private static Method method(Class<?> target, String methodName) {
    Method[] methods = ReflectionUtils.getUniqueDeclaredMethods(
            target, method -> methodName.equals(method.getName()));
    assertThat(methods).as("No unique method named " + methodName + " found of " + target.getName())
            .hasSize(1);
    return methods[0];
  }

  static class SampleFactory {

    String simpleBean() {
      return "Hello";
    }

    String beanWithTwoArgs(String first, Integer second) {
      return first + second;
    }

    String cloneBean(String arg) {
      return arg;
    }

    String errorBean(String msg) {
      throw new IllegalStateException(msg);
    }

  }

  static class ExtendedSampleFactory extends SampleFactory {

    @Override
    String beanWithTwoArgs(String first, Integer second) {
      return second + first;
    }
  }

  static class AnotherFactory {

    String beanWithTwoArgs(String first, Integer second) {
      return second + first;
    }

  }
}