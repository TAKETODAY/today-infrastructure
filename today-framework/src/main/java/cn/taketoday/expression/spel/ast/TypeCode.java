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

package cn.taketoday.expression.spel.ast;

/**
 * Captures primitive types and their corresponding class objects, plus one special entry
 * that represents all reference (non-primitive) types.
 *
 * @author Andy Clement
 */
public enum TypeCode {

  /**
   * An {@link Object}.
   */
  OBJECT(Object.class),

  /**
   * A {@code boolean}.
   */
  BOOLEAN(Boolean.TYPE),

  /**
   * A {@code byte}.
   */
  BYTE(Byte.TYPE),

  /**
   * A {@code char}.
   */
  CHAR(Character.TYPE),

  /**
   * A {@code double}.
   */
  DOUBLE(Double.TYPE),

  /**
   * A {@code float}.
   */
  FLOAT(Float.TYPE),

  /**
   * An {@code int}.
   */
  INT(Integer.TYPE),

  /**
   * A {@code long}.
   */
  LONG(Long.TYPE),

  /**
   * An {@link Object}.
   */
  SHORT(Short.TYPE);

  private final Class<?> type;

  TypeCode(Class<?> type) {
    this.type = type;
  }

  public Class<?> getType() {
    return this.type;
  }

  public static TypeCode forName(String name) {
    TypeCode[] tcs = values();
    for (int i = 1; i < tcs.length; i++) {
      if (tcs[i].name().equalsIgnoreCase(name)) {
        return tcs[i];
      }
    }
    return OBJECT;
  }

  public static TypeCode forClass(Class<?> clazz) {
    TypeCode[] allValues = TypeCode.values();
    for (TypeCode typeCode : allValues) {
      if (clazz == typeCode.getType()) {
        return typeCode;
      }
    }
    return OBJECT;
  }

}
