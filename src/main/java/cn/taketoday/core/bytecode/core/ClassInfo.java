/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.core.bytecode.core;

import cn.taketoday.core.bytecode.Type;

/**
 * @author TODAY <br>
 * 2019-09-03 19:33
 */
@SuppressWarnings("rawtypes")
public abstract class ClassInfo {

  protected ClassInfo() { }

  public abstract Type getType();

  public abstract Type getSuperType();

  public abstract Type[] getInterfaces();

  public abstract int getModifiers();

  public boolean equals(Object o) {
    return (o == this) || ((o instanceof ClassInfo) && getType().equals(((ClassInfo) o).getType()));
  }

  public int hashCode() {
    return getType().hashCode();
  }

  public String toString() {
    // TODO: include modifiers, superType, interfaces
    return getType().getClassName();
  }

  // static

  public static ClassInfo from(final Class clazz) {
    final Type type = Type.fromClass(clazz);
    final Type sc = (clazz.getSuperclass() == null) ? null : Type.fromClass(clazz.getSuperclass());
    final class DefaultClassInfo extends ClassInfo {
      public Type getType() {
        return type;
      }

      public Type getSuperType() {
        return sc;
      }

      public Type[] getInterfaces() {
        return Type.getTypes(clazz.getInterfaces());
      }

      public int getModifiers() {
        return clazz.getModifiers();
      }
    }

    return new DefaultClassInfo();
  }
}
