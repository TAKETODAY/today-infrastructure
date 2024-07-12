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
package cn.taketoday.bytecode.tree.analysis;

import cn.taketoday.bytecode.Type;

/**
 * A {@link Value} that is represented with its type in a seven types type system. This type system
 * distinguishes the UNINITIALZED, INT, FLOAT, LONG, DOUBLE, REFERENCE and RETURNADDRESS types.
 *
 * @author Eric Bruneton
 */
public class BasicValue implements Value {

  /** An uninitialized value. */
  public static final BasicValue UNINITIALIZED_VALUE = new BasicValue(null);

  /** A byte, boolean, char, short, or int value. */
  public static final BasicValue INT_VALUE = new BasicValue(Type.INT_TYPE);

  /** A float value. */
  public static final BasicValue FLOAT_VALUE = new BasicValue(Type.FLOAT_TYPE);

  /** A long value. */
  public static final BasicValue LONG_VALUE = new BasicValue(Type.LONG_TYPE);

  /** A double value. */
  public static final BasicValue DOUBLE_VALUE = new BasicValue(Type.DOUBLE_TYPE);

  /** An object or array reference value. */
  public static final BasicValue REFERENCE_VALUE =
          new BasicValue(Type.forInternalName("java/lang/Object"));

  /** A return address value (produced by a jsr instruction). */
  public static final BasicValue RETURNADDRESS_VALUE = new BasicValue(Type.VOID_TYPE);

  /** The {@link Type} of this value, or {@literal null} for uninitialized values. */
  private final Type type;

  /**
   * Constructs a new {@link BasicValue} of the given type.
   *
   * @param type the value type.
   */
  public BasicValue(final Type type) {
    this.type = type;
  }

  /**
   * Returns the {@link Type} of this value.
   *
   * @return the {@link Type} of this value.
   */
  public Type getType() {
    return type;
  }

  @Override
  public int getSize() {
    return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
  }

  /**
   * Returns whether this value corresponds to an object or array reference.
   *
   * @return whether this value corresponds to an object or array reference.
   */
  public boolean isReference() {
    return type != null && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
  }

  @Override
  public boolean equals(final Object value) {
    if (value == this) {
      return true;
    }
    else if (value instanceof BasicValue) {
      if (type == null) {
        return ((BasicValue) value).type == null;
      }
      else {
        return type.equals(((BasicValue) value).type);
      }
    }
    else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return type == null ? 0 : type.hashCode();
  }

  @Override
  public String toString() {
    if (this == UNINITIALIZED_VALUE) {
      return ".";
    }
    else if (this == RETURNADDRESS_VALUE) {
      return "A";
    }
    else if (this == REFERENCE_VALUE) {
      return "R";
    }
    else {
      return type.getDescriptor();
    }
  }
}
