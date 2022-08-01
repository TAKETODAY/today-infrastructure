/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.lang;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Enumerable for {@link Enum}
 *
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
   * @see #of(Class, V)
   */
  static <T extends Enumerable<V>, V> T of(Class<T> enumerable, V value, Supplier<T> defaultValue) {
    return find(enumerable, value).orElseGet(defaultValue);
  }

  /**
   * @param defaultValue default value
   * @param <T> enum type
   * @param <V> enumeration value type
   * @see #of(Class, V)
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
