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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface Enumerable<V> extends Descriptive {

  @SuppressWarnings("unchecked")
  default V getValue() {
    return (V) name();
  }

  @Override
  String getDescription(); // 描述 对标

  String name(); // 枚举默认的名字 , 该方法无需实现，枚举类自动继承

  @Nullable
  static <T extends Enumerable<V>, V> T introspect(Class<T> enumerable, V value) {
    T[] enumConstants = enumerable.getEnumConstants();
    if (enumConstants != null) {
      for (T constant : enumConstants) {
        if (Objects.equals(value, constant.getValue())) {
          return constant;
        }
      }
    }
    return null;
  }

  /**
   * 通过 枚举名称 返回 Enumerable
   *
   * @param enumerable 枚举类
   * @param name 枚举名称
   * @param <T>枚举类型
   * @param <V> 枚举值类型
   * @return 枚举实例
   */
  @Nullable
  static <T extends Enumerable<V>, V> T fromName(Class<T> enumerable, String name) {
    T[] enumConstants = enumerable.getEnumConstants();
    if (enumConstants != null) {
      for (T constant : enumConstants) {
        if (Objects.equals(name, constant.name())) {
          return constant;
        }
      }
    }
    return null;
  }

  /**
   * 获取 name 对应的 value
   *
   * @param enumerable 枚举类
   * @param name 枚举名称
   * @param <T> 枚举类型
   * @param <V> 枚举值类型
   * @return 枚举值
   * @see Enumerable#getValue()
   */
  @Nullable
  static <T extends Enumerable<V>, V> V introspectValue(Class<T> enumerable, String name) {
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
   * @param defaultValue 默认值
   */
  static <T extends Enumerable<V>, V> T introspect(Class<T> enumerable, V value, Supplier<T> defaultValue) {
    return find(enumerable, value).orElseGet(defaultValue);
  }

  static <T extends Enumerable<V>, V> Optional<T> find(Class<T> enumerable, V value) {
    return Optional.ofNullable(introspect(enumerable, value));
  }

}
