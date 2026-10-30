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

package infra.context.properties.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for nesting depth limits in {@link JSONTokener}.
 */
class JSONTokenerTests {

  @ParameterizedTest
  @ValueSource(strings = { "array", "object", "mixed" })
  void acceptsMaximumDepth(String type) throws JSONException {
    assertThat(new JSONTokener(nested(type, 512)).nextValue()).isNotNull();
  }

  @ParameterizedTest
  @ValueSource(strings = { "array", "object", "mixed" })
  void rejectsExcessiveDepth(String type) {
    assertThatExceptionOfType(JSONException.class)
            .isThrownBy(() -> new JSONTokener(nested(type, 513)).nextValue())
            .withMessageStartingWith("Maximum nesting depth of 512 exceeded at character ");
  }

  @ParameterizedTest
  @ValueSource(strings = { "array", "object", "mixed" })
  void restoresDepthAfterReadingValue(String type) throws JSONException {
    JSONTokener tokener = new JSONTokener(nested(type, 512) + " " + nested(type, 512));
    assertThat(tokener.nextValue()).isNotNull();
    assertThat(tokener.nextValue()).isNotNull();
  }

  private String nested(String type, int depth) {
    String value = "0";
    for (int i = 0; i < depth; i++) {
      value = type.equals("object") || (type.equals("mixed") && i % 2 == 0)
              ? "{\"value\":" + value + "}" : "[" + value + "]";
    }
    return value;
  }

}
