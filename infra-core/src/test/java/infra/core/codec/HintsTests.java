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

package infra.core.codec;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:45
 */
class HintsTests {

  @Test
  void fromCreatesSingletonMap() {
    String hintName = "test.hint";
    Object value = "testValue";
    Map<String, Object> hints = Hints.from(hintName, value);

    assertThat(hints).isNotNull();
    assertThat(hints).hasSize(1);
    assertThat(hints.get(hintName)).isEqualTo(value);
  }

  @Test
  void noneReturnsEmptyMap() {
    Map<String, Object> hints = Hints.none();

    assertThat(hints).isNotNull();
    assertThat(hints).isEmpty();
  }

  @Test
  void getRequiredHintReturnsValueWhenPresent() {
    String hintName = "test.hint";
    Object value = "testValue";
    Map<String, Object> hints = Collections.singletonMap(hintName, value);

    Object result = Hints.getRequiredHint(hints, hintName);
    assertThat(result).isEqualTo(value);
  }

  @Test
  void getRequiredHintThrowsExceptionWhenHintsIsNull() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Hints.getRequiredHint(null, "test.hint"))
            .withMessageContaining("No hints map for required hint");
  }

  @Test
  void getRequiredHintThrowsExceptionWhenHintNotFound() {
    Map<String, Object> hints = Collections.emptyMap();

    assertThatIllegalArgumentException()
            .isThrownBy(() -> Hints.getRequiredHint(hints, "test.hint"))
            .withMessageContaining("Hints map must contain the hint");
  }

  @Test
  void getLogPrefixReturnsPrefixWhenPresent() {
    String logPrefix = "test-prefix";
    Map<String, Object> hints = Collections.singletonMap(Hints.LOG_PREFIX_HINT, logPrefix);

    String result = Hints.getLogPrefix(hints);
    assertThat(result).isEqualTo(logPrefix);
  }

  @Test
  void getLogPrefixReturnsEmptyStringWhenNotPresent() {
    Map<String, Object> hints = Collections.emptyMap();

    String result = Hints.getLogPrefix(hints);
    assertThat(result).isEqualTo("");
  }

  @Test
  void getLogPrefixReturnsEmptyStringWhenHintsIsNull() {
    String result = Hints.getLogPrefix(null);
    assertThat(result).isEqualTo("");
  }

  @Test
  void isLoggingSuppressedReturnsTrueWhenHintIsSet() {
    Map<String, Object> hints = Collections.singletonMap(Hints.SUPPRESS_LOGGING_HINT, true);

    boolean result = Hints.isLoggingSuppressed(hints);
    assertThat(result).isTrue();
  }

  @Test
  void isLoggingSuppressedReturnsFalseWhenHintIsNotSet() {
    Map<String, Object> hints = Collections.emptyMap();

    boolean result = Hints.isLoggingSuppressed(hints);
    assertThat(result).isFalse();
  }

  @Test
  void isLoggingSuppressedReturnsFalseWhenHintsIsNull() {
    boolean result = Hints.isLoggingSuppressed(null);
    assertThat(result).isFalse();
  }

  @Test
  void mergeTwoEmptyMapsReturnsEmptyMap() {
    Map<String, Object> result = Hints.merge(null, null);
    assertThat(result).isSameAs(Collections.emptyMap());
  }

  @Test
  void mergeEmptyMapWithNonEmptyMapReturnsNonEmptyMap() {
    Map<String, Object> hints = Collections.singletonMap("key", "value");
    Map<String, Object> result = Hints.merge(null, hints);
    assertThat(result).isSameAs(hints);
  }

  @Test
  void mergeNonEmptyMapWithEmptyMapReturnsNonEmptyMap() {
    Map<String, Object> hints = Collections.singletonMap("key", "value");
    Map<String, Object> result = Hints.merge(hints, null);
    assertThat(result).isSameAs(hints);
  }

  @Test
  void mergeTwoNonEmptyMapsReturnsCombinedMap() {
    Map<String, Object> hints1 = Collections.singletonMap("key1", "value1");
    Map<String, Object> hints2 = Collections.singletonMap("key2", "value2");
    Map<String, Object> result = Hints.merge(hints1, hints2);

    assertThat(result).hasSize(2);
    assertThat(result.get("key1")).isEqualTo("value1");
    assertThat(result.get("key2")).isEqualTo("value2");
  }

  @Test
  void mergeHintIntoEmptyMapReturnsSingletonMap() {
    String hintName = "key";
    Object hintValue = "value";
    Map<String, Object> result = Hints.merge(null, hintName, hintValue);

    assertThat(result).hasSize(1);
    assertThat(result.get(hintName)).isEqualTo(hintValue);
  }

  @Test
  void mergeHintIntoNonEmptyMapReturnsCombinedMap() {
    Map<String, Object> hints = Collections.singletonMap("existing", "value");
    String hintName = "newKey";
    Object hintValue = "newValue";
    Map<String, Object> result = Hints.merge(hints, hintName, hintValue);

    assertThat(result).hasSize(2);
    assertThat(result.get("existing")).isEqualTo("value");
    assertThat(result.get(hintName)).isEqualTo(hintValue);
  }

}