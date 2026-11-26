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
 * Associates an {@link Enum} constant with a value and a textual description.
 * By default, both are the constant's declared name. Override {@link #getValue()}
 * to associate a custom value and {@link #getDescription()} to provide a
 * different description.
 *
 * <p>Use {@link #of(Class, Object)} to look up a constant by its associated
 * value, or {@link #findValue(Class, String)} to look up a value by the
 * constant's declared name.
 *
 * @param <V> the type of the associated value
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface Enumerable<V> extends Descriptive {

  /**
   * Return the value associated with this constant.
   *
   * @return the associated value
   * @implSpec The default implementation returns {@link #name()} cast to
   * {@code V}. Override this method if {@code V} cannot represent a {@link String}.
   */
  @SuppressWarnings("unchecked")
  default V getValue() {
    return (V) name();
  }

  /**
   * {@inheritDoc}
   *
   * @implSpec The default implementation returns {@link #name()}, not the
   * associated value.
   */
  @Override
  default String getDescription() {
    return name();
  }

  /**
   * Return the name of this constant exactly as declared in the enum.
   * Enum implementations inherit this method from {@link Enum#name()}.
   *
   * @return the declared name of the constant
   */
  String name();

  /**
   * Find the first constant whose {@linkplain #getValue() associated value}
   * matches the supplied value according to {@link Objects#equals(Object, Object)}.
   * Constants are checked in declaration order.
   *
   * @param <T> the enum type
   * @param <V> the associated value type
   * @param enumerable the enum class to search
   * @param value the value to match, or {@code null} to skip the lookup
   * @return the first matching constant, or {@code null} if the value is
   * {@code null}, the class is not an enum, or no constant matches
   * @throws NullPointerException if {@code enumerable} is {@code null}
   * and {@code value} is non-null
   */
  static <T extends Enumerable<V>, V> @Nullable T of(Class<T> enumerable, @Nullable V value) {
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
   * Return the associated value of the constant with the given declared name.
   * Names are matched case-sensitively.
   *
   * @param <T> the enum type
   * @param <V> the associated value type
   * @param enumerable the enum class to search
   * @param name the declared name of the constant
   * @return the associated value, or {@code null} if the class is not an enum
   * or no constant has the given name
   * @see #getValue()
   */
  static <T extends Enumerable<V>, V> @Nullable V findValue(Class<T> enumerable, String name) {
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
   * Find a constant by its associated value, or obtain a fallback from a supplier.
   * The supplier is invoked only when the lookup finds no match.
   *
   * @param <T> the enum type
   * @param <V> the associated value type
   * @param enumerable the enum class to search
   * @param value the value to match
   * @param defaultValue the supplier of the fallback constant
   * @return the first matching constant, or the result of the supplier if the
   * value is {@code null} or no constant matches
   * @see #of(Class, Object)
   */
  static <T extends Enumerable<V>, V> T of(Class<T> enumerable, V value, Supplier<T> defaultValue) {
    return find(enumerable, value).orElseGet(defaultValue);
  }

  /**
   * Find a constant by its associated value, or return the given fallback.
   *
   * @param <T> the enum type
   * @param <V> the associated value type
   * @param enumerable the enum class to search
   * @param value the value to match
   * @param defaultValue the fallback constant
   * @return the first matching constant, or {@code defaultValue} if the value
   * is {@code null} or no constant matches
   * @see #of(Class, Object)
   */
  static <T extends Enumerable<V>, V> T of(Class<T> enumerable, V value, T defaultValue) {
    return find(enumerable, value).orElse(defaultValue);
  }

  /**
   * Find a constant by its associated value and return the result as an {@link Optional}.
   *
   * @param <T> the enum type
   * @param <V> the associated value type
   * @param enumerable the enum class to search
   * @param value the value to match
   * @return an optional containing the first matching constant, or an empty
   * optional if the value is {@code null} or no constant matches
   * @see #of(Class, Object)
   */
  static <T extends Enumerable<V>, V> Optional<T> find(Class<T> enumerable, V value) {
    return Optional.ofNullable(of(enumerable, value));
  }

}
