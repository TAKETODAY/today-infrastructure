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

package infra.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
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
            .isInstanceOf(InvocationTargetException.class)
            .cause()
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