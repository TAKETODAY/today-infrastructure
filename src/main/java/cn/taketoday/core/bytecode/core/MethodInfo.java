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

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.bytecode.Type;
import cn.taketoday.core.bytecode.commons.MethodSignature;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ReflectionUtils;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.STATIC;

/**
 * @author TODAY <br>
 * 2019-09-03 19:01
 */
public abstract class MethodInfo {

  private static final Method[] OBJECT_METHODS;

  static {
    Method[] declaredMethods = Object.class.getDeclaredMethods();
    ArrayList<Method> objectMethods = new ArrayList<>(declaredMethods.length);
    for (Method method : declaredMethods) {
      if ("finalize".equals(method.getName()) || (method.getModifiers() & (FINAL | STATIC)) > 0) {
        continue;
      }
      objectMethods.add(method);
    }
    // @since 4.0
    OBJECT_METHODS = ReflectionUtils.toMethodArray(objectMethods);
  }

  protected MethodInfo() { }

  abstract public ClassInfo getClassInfo();

  abstract public int getModifiers();

  abstract public MethodSignature getSignature();

  abstract public Type[] getExceptionTypes();

  @Override
  public boolean equals(Object o) {
    return (o == this) || ((o instanceof MethodInfo) && getSignature().equals(((MethodInfo) o).getSignature()));
  }

  @Override
  public int hashCode() {
    return getSignature().hashCode();
  }

  @Override
  public String toString() {
    // TODO: include modifiers, exceptions
    return getSignature().toString();
  }

  /**
   * @since 4.0
   */
  public boolean isConstructor() {
    return MethodSignature.CONSTRUCTOR_NAME.equals(getSignature().getName());
  }

  // static factory

  public static MethodInfo from(final Member member) {
    return from(member, member.getModifiers());
  }

  public static MethodInfo from(final Member member, final int modifiers) {
    return new MethodInfo() {
      private ClassInfo ci;

      public ClassInfo getClassInfo() {
        if (ci == null)
          ci = ClassInfo.from(member.getDeclaringClass());
        return ci;
      }

      public int getModifiers() {
        return modifiers;
      }

      MethodSignature sig;

      public MethodSignature getSignature() {
        if (sig == null) {
          sig = MethodSignature.from(member);
        }
        return sig;
      }

      public Type[] getExceptionTypes() {
        return Type.getExceptionTypes(member);
      }

    };
  }

  //

  public static List<Method> addAllMethods(final Class<?> type, final List<Method> list) {
    if (type == Object.class) {
      CollectionUtils.addAll(list, OBJECT_METHODS);
    }
    else {
      Collections.addAll(list, type.getDeclaredMethods());
    }

    final Class<?> superclass = type.getSuperclass();
    if (superclass != null) {
      addAllMethods(superclass, list);
    }
    for (final Class<?> interface_ : type.getInterfaces()) {
      addAllMethods(interface_, list);
    }
    return list;
  }

}
