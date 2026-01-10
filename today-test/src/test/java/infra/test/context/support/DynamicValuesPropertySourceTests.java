/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.test.context.support;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DynamicValuesPropertySource}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 */
class DynamicValuesPropertySourceTests {

  @SuppressWarnings("serial")
  private final DynamicValuesPropertySource source = new DynamicValuesPropertySource("test",
          new HashMap<String, Supplier<Object>>() {{
            put("a", () -> "A");
            put("b", () -> "B");
          }});

  @Test
  void getPropertyReturnsSuppliedProperty() throws Exception {
    assertThat(this.source.getProperty("a")).isEqualTo("A");
    assertThat(this.source.getProperty("b")).isEqualTo("B");
  }

  @Test
  void getPropertyWhenMissingReturnsNull() throws Exception {
    assertThat(this.source.getProperty("c")).isNull();
  }

  @Test
  void containsPropertyWhenPresentReturnsTrue() {
    assertThat(this.source.containsProperty("a")).isTrue();
    assertThat(this.source.containsProperty("b")).isTrue();
  }

  @Test
  void containsPropertyWhenMissingReturnsFalse() {
    assertThat(this.source.containsProperty("c")).isFalse();
  }

  @Test
  void getPropertyNamesReturnsNames() {
    assertThat(this.source.getPropertyNames()).containsExactly("a", "b");
  }

}
