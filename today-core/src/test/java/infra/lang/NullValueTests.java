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

package infra.lang;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 17:38
 */
class NullValueTests {

  @Test
  void shouldReturnCorrectHashCode() {
    NullValue nullValue = NullValue.INSTANCE;

    int hashCode = nullValue.hashCode();

    assertThat(hashCode).isEqualTo(NullValue.class.hashCode());
  }

  @Test
  void shouldEqualsReturnTrueForSameInstance() {
    NullValue nullValue1 = NullValue.INSTANCE;
    NullValue nullValue2 = NullValue.INSTANCE;

    boolean result = nullValue1.equals(nullValue2);

    assertThat(result).isTrue();
  }

  @Test
  void shouldEqualsReturnFalseForDifferentObject() {
    NullValue nullValue = NullValue.INSTANCE;
    Object otherObject = new Object();

    boolean result = nullValue.equals(otherObject);

    assertThat(result).isFalse();
  }

  @Test
  void shouldEqualsReturnFalseForNull() {
    NullValue nullValue = NullValue.INSTANCE;

    boolean result = nullValue.equals(null);

    assertThat(result).isFalse();
  }

  @Test
  void shouldToStringReturnCorrectValue() {
    NullValue nullValue = NullValue.INSTANCE;

    String result = nullValue.toString();

    assertThat(result).isEqualTo("NullValue");
  }

  @Test
  void shouldHaveCorrectSerialVersionUID() throws NoSuchFieldException, IllegalAccessException {
    Field serialVersionUID1 = NullValue.class.getDeclaredField("serialVersionUID");
    ReflectionUtils.makeAccessible(serialVersionUID1);
    long serialVersionUID = serialVersionUID1.getLong(null);
    assertThat(serialVersionUID).isEqualTo(1L);
  }

}