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