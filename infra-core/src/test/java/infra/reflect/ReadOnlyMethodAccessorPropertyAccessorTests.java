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