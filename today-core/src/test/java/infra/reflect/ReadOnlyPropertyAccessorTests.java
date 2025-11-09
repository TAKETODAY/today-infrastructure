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