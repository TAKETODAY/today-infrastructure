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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import infra.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:46
 */
class ReadOnlyPropertyAccessorTests {

  @Test
  void shouldThrowExceptionWhenSettingValue() {
    ReadOnlyPropertyAccessor accessor = new ReadOnlyPropertyAccessor() {
      @Nullable
      @Override
      public Object get(Object obj) {
        return null;
      }
    };

    Object target = new Object();
    String value = "testValue";

    assertThatThrownBy(() -> accessor.set(target, value))
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Can't set value '%s' to '%s' read only property".formatted(ObjectUtils.toHexString(value), target.getClass()));
  }

  @Test
  void shouldThrowExceptionWhenGettingWriteMethod() {
    ReadOnlyPropertyAccessor accessor = new ReadOnlyPropertyAccessor() {
      @Nullable
      @Override
      public Object get(Object obj) {
        return null;
      }
    };

    assertThatThrownBy(() -> accessor.getWriteMethod())
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Readonly property");
  }

  @Test
  void shouldReturnFalseForIsWriteable() {
    ReadOnlyPropertyAccessor accessor = new ReadOnlyPropertyAccessor() {
      @Nullable
      @Override
      public Object get(Object obj) {
        return null;
      }
    };

    assertThat(accessor.isWriteable()).isFalse();
  }

  @Test
  void shouldAllowGettingValue() {
    String expectedValue = "readonlyValue";
    ReadOnlyPropertyAccessor accessor = new ReadOnlyPropertyAccessor() {
      @Nullable
      @Override
      public Object get(Object obj) {
        return expectedValue;
      }
    };

    Object target = new Object();
    Object value = accessor.get(target);

    assertThat(value).isEqualTo(expectedValue);
  }

  @Test
  void shouldHandleNullValueInExceptionMessage() {
    ReadOnlyPropertyAccessor accessor = new ReadOnlyPropertyAccessor() {
      @Nullable
      @Override
      public Object get(Object obj) {
        return null;
      }
    };

    Object target = new Object();

    assertThatThrownBy(() -> accessor.set(target, null))
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Can't set value 'null' to '%s' read only property".formatted(target.getClass()));
  }

  @Test
  void shouldHandleClassToStringWithNullObject() {
    Class<?> result = ReadOnlyPropertyAccessor.classToString(null);

    assertThat(result).isNull();
  }

  @Test
  void shouldHandleClassToStringWithValidObject() {
    Object obj = new String("test");
    Class<?> result = ReadOnlyPropertyAccessor.classToString(obj);

    assertThat(result).isEqualTo(String.class);
  }

}