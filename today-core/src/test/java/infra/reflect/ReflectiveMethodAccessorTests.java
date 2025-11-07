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
 * @since 5.0 2025/11/7 17:39
 */
class ReflectiveMethodAccessorTests {

  @Test
  void shouldCreateReflectiveMethodAccessorWithHandleException() throws NoSuchMethodException {
    Method method = TestClass.class.getDeclaredMethod("publicMethod");
    boolean handleException = true;

    ReflectiveMethodAccessor accessor = new ReflectiveMethodAccessor(method, handleException);

    assertThat(accessor).isNotNull();
  }

  @Test
  void shouldCreateReflectiveMethodAccessorWithoutHandleException() throws NoSuchMethodException {
    Method method = TestClass.class.getDeclaredMethod("publicMethod");
    boolean handleException = false;

    ReflectiveMethodAccessor accessor = new ReflectiveMethodAccessor(method, handleException);

    assertThat(accessor).isNotNull();
  }

  @Test
  void shouldInvokeMethodSuccessfully() throws NoSuchMethodException {
    Method method = TestClass.class.getDeclaredMethod("publicMethod");
    ReflectiveMethodAccessor accessor = new ReflectiveMethodAccessor(method, false);
    TestClass target = new TestClass();

    Object result = accessor.invoke(target, new Object[0]);

    assertThat(result).isEqualTo("success");
  }

  @Test
  void shouldInvokeMethodWithParameters() throws NoSuchMethodException {
    Method method = TestClass.class.getDeclaredMethod("methodWithParams", String.class, int.class);
    ReflectiveMethodAccessor accessor = new ReflectiveMethodAccessor(method, false);
    TestClass target = new TestClass();

    Object result = accessor.invoke(target, new Object[] { "test", 42 });

    assertThat(result).isEqualTo("test-42");
  }

  @Test
  void shouldHandleExceptionWhenHandleExceptionIsTrue() throws NoSuchMethodException {
    Method method = TestClass.class.getDeclaredMethod("throwException");
    ReflectiveMethodAccessor accessor = new ReflectiveMethodAccessor(method, true);
    TestClass target = new TestClass();

    assertThatThrownBy(() -> accessor.invoke(target, new Object[0]))
            .isInstanceOf(RuntimeException.class);
  }

  @Test
  void shouldRethrowExceptionWhenHandleExceptionIsFalse() throws NoSuchMethodException {
    Method method = TestClass.class.getDeclaredMethod("throwException");
    ReflectiveMethodAccessor accessor = new ReflectiveMethodAccessor(method, false);
    TestClass target = new TestClass();

    assertThatThrownBy(() -> accessor.invoke(target, new Object[0]))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Test exception");
  }

  @Test
  void shouldInvokeStaticMethod() throws NoSuchMethodException {
    Method method = TestClass.class.getDeclaredMethod("staticMethod");
    ReflectiveMethodAccessor accessor = new ReflectiveMethodAccessor(method, false);

    Object result = accessor.invoke(null, new Object[0]);

    assertThat(result).isEqualTo("static success");
  }

  @Test
  void shouldGetMethod() throws NoSuchMethodException {
    Method method = TestClass.class.getDeclaredMethod("publicMethod");
    ReflectiveMethodAccessor accessor = new ReflectiveMethodAccessor(method, false);

    Method retrievedMethod = accessor.getMethod();

    assertThat(retrievedMethod).isEqualTo(method);
  }

  static class TestClass {

    public String publicMethod() {
      return "success";
    }

    public String methodWithParams(String param1, int param2) {
      return param1 + "-" + param2;
    }

    public String throwException() {
      throw new RuntimeException("Test exception");
    }

    public static String staticMethod() {
      return "static success";
    }
  }

}