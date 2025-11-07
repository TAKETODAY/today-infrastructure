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

package infra.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:52
 */
class ReadOnlyMethodAccessorPropertyAccessorTests {

  @Test
  void shouldCreateReadOnlyMethodAccessorPropertyAccessor() throws NoSuchMethodException {
    Method method = TestBean.class.getDeclaredMethod("getValue");
    MethodInvoker invoker = MethodInvoker.forMethod(method);

    ReadOnlyMethodAccessorPropertyAccessor accessor = new ReadOnlyMethodAccessorPropertyAccessor(invoker);

    assertThat(accessor).isNotNull();
  }

  @Test
  void shouldGetPropertyValue() throws NoSuchMethodException {
    Method method = TestBean.class.getDeclaredMethod("getValue");
    MethodInvoker invoker = MethodInvoker.forMethod(method);
    ReadOnlyMethodAccessorPropertyAccessor accessor = new ReadOnlyMethodAccessorPropertyAccessor(invoker);

    TestBean bean = new TestBean("testValue");
    Object result = accessor.get(bean);

    assertThat(result).isEqualTo("testValue");
  }

  @Test
  void shouldGetReadMethod() throws NoSuchMethodException {
    Method method = TestBean.class.getDeclaredMethod("getValue");
    MethodInvoker invoker = MethodInvoker.forMethod(method);
    ReadOnlyMethodAccessorPropertyAccessor accessor = new ReadOnlyMethodAccessorPropertyAccessor(invoker);

    Method readMethod = accessor.getReadMethod();

    assertThat(readMethod).isEqualTo(method);
  }

  @Test
  void shouldThrowExceptionWhenSettingValue() throws NoSuchMethodException {
    Method method = TestBean.class.getDeclaredMethod("getValue");
    MethodInvoker invoker = MethodInvoker.forMethod(method);
    ReadOnlyMethodAccessorPropertyAccessor accessor = new ReadOnlyMethodAccessorPropertyAccessor(invoker);

    TestBean bean = new TestBean("testValue");

    assertThatThrownBy(() -> accessor.set(bean, "newValue"))
            .isInstanceOf(ReflectionException.class)
            .hasMessageContaining("read only property");
  }

  @Test
  void shouldReturnFalseForIsWriteable() throws NoSuchMethodException {
    Method method = TestBean.class.getDeclaredMethod("getValue");
    MethodInvoker invoker = MethodInvoker.forMethod(method);
    ReadOnlyMethodAccessorPropertyAccessor accessor = new ReadOnlyMethodAccessorPropertyAccessor(invoker);

    boolean writeable = accessor.isWriteable();

    assertThat(writeable).isFalse();
  }

  @Test
  void shouldThrowExceptionWhenGettingWriteMethod() throws NoSuchMethodException {
    Method method = TestBean.class.getDeclaredMethod("getValue");
    MethodInvoker invoker = MethodInvoker.forMethod(method);
    ReadOnlyMethodAccessorPropertyAccessor accessor = new ReadOnlyMethodAccessorPropertyAccessor(invoker);

    assertThatThrownBy(() -> accessor.getWriteMethod())
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Readonly property");
  }

  static class TestBean {
    private String value;

    public TestBean(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

}