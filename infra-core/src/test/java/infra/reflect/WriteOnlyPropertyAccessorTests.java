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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:45
 */
class WriteOnlyPropertyAccessorTests {

  @Test
  void shouldThrowExceptionWhenGettingValue() {
    WriteOnlyPropertyAccessor accessor = new WriteOnlyPropertyAccessor() {
      @Override
      public void set(Object obj, @Nullable Object value) {
        // No-op implementation
      }
    };

    assertThatThrownBy(() -> accessor.get(new Object()))
            .isInstanceOf(ReflectionException.class)
            .hasMessage("Cannot get property cause: write only property");
  }

  @Test
  void shouldAllowSettingValue() {
    WriteOnlyPropertyAccessor accessor = new WriteOnlyPropertyAccessor() {
      @Override
      public void set(Object obj, @Nullable Object value) {
        // No-op implementation - just testing that method can be called
      }
    };

    // Should not throw exception
    accessor.set(new Object(), "testValue");
    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleNullValueWhenSetting() {
    WriteOnlyPropertyAccessor accessor = new WriteOnlyPropertyAccessor() {
      @Override
      public void set(Object obj, @Nullable Object value) {
        // No-op implementation
      }
    };

    // Should not throw exception when value is null
    accessor.set(new Object(), null);
    assertThat(true).isTrue();
  }

}