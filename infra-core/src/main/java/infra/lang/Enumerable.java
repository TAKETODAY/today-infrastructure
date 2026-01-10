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

import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Enumerable for {@link Enum}
 *
 * @param <V> Value type
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface Enumerable<V> extends Descriptive {

  @SuppressWarnings("unchecked")
  default V getValue() {
    return (V) name();
  }

  @Override
  default String getDescription() {
    return name();
  }

  /**
   * The default name of the enumeration, this method does not need
   * to be implemented, the enumeration class is automatically inherited
   */
  String name();

  /**
   * Returns Enumerable by {@link Enumerable#getValue() enum value}
   *
   * @param enumerable enum
   * @param value enumeration value
   * @param <T> enumeration type
   * @param <V> enumeration value type
   * @return enumeration instance
   * @throws NullPointerException if enumerable is {@code null}
   * @see Enumerable#getValue()
   */
  @Nullable
  static <T extends Enumerable<V>, V> T of(Class<T> enumerable, @Nullable V value) {
    if (value != null) {
      T[] enumConstants = enumerable.getEnumConstants();
      if (enumConstants != null) {
        for (T constant : enumConstants) {
          if (Objects.equals(value, constant.getValue())) {
            return constant;
          }
        }
      }
    }
    return null;
  }

  /**
   * Get the value corresponding to the name
   *
   * @param enumerable enumeration class
   * @param name enumeration name
   * @param <T> enum type
   * @param <V> enumeration value type
   * @return enumeration value
   * @see Enumerable#getValue()
   */
  @Nullable
  static <T extends Enumerable<V>, V> V getValue(Class<T> enumerable, String name) {
    T[] enumConstants = enumerable.getEnumConstants();
    if (enumConstants != null) {
      for (T constant : enumConstants) {
        if (Objects.equals(name, constant.name())) {
          return constant.getValue();
        }
      }
    }
    return null;
  }

  /**
   * @param <T> enum type
   * @param <V> enumeration value type
   * @param defaultValue default value
   * @see #of(Class, Object)
   */
  static <T extends Enumerable<V>, V> T of(Class<T> enumerable, V value, Supplier<T> defaultValue) {
    return find(enumerable, value).orElseGet(defaultValue);
  }

  /**
   * @param defaultValue default value
   * @param <T> enum type
   * @param <V> enumeration value type
   * @see #of(Class, Object)
   */
  static <T extends Enumerable<V>, V> T of(Class<T> enumerable, V value, T defaultValue) {
    return find(enumerable, value).orElse(defaultValue);
  }

  /**
   * @return Optional of T
   */
  static <T extends Enumerable<V>, V> Optional<T> find(Class<T> enumerable, V value) {
    return Optional.ofNullable(of(enumerable, value));
  }

}
