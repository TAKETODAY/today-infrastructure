/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.expression.spel.ast;

/**
 * Captures primitive types and their corresponding class objects, plus one special entry
 * that represents all reference (non-primitive) types.
 *
 * @author Andy Clement
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public enum TypeCode {

  /**
   * An {@link Object}.
   */
  OBJECT(Object.class),

  /**
   * A {@code boolean}.
   */
  BOOLEAN(boolean.class),

  /**
   * A {@code char}.
   */
  CHAR(char.class),

  /**
   * A {@code byte}.
   */
  BYTE(byte.class),

  /**
   * A {@code short}.
   */
  SHORT(short.class),

  /**
   * An {@code int}.
   */
  INT(int.class),

  /**
   * A {@code long}.
   */
  LONG(long.class),

  /**
   * A {@code float}.
   */
  FLOAT(float.class),

  /**
   * A {@code double}.
   */
  DOUBLE(double.class);

  private final Class<?> type;

  TypeCode(Class<?> type) {
    this.type = type;
  }

  public Class<?> getType() {
    return this.type;
  }

  public static TypeCode forName(String name) {
    for (TypeCode typeCode : values()) {
      if (typeCode.name().equalsIgnoreCase(name)) {
        return typeCode;
      }
    }
    return OBJECT;
  }

  public static TypeCode forClass(Class<?> clazz) {
    for (TypeCode typeCode : values()) {
      if (typeCode.getType() == clazz) {
        return typeCode;
      }
    }
    return OBJECT;
  }

}
