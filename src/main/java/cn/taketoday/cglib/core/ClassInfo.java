/*
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.taketoday.cglib.core;

import cn.taketoday.asm.Type;

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
