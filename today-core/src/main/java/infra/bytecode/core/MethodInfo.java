/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.bytecode.core;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import infra.bytecode.Type;
import infra.bytecode.commons.MethodSignature;
import infra.util.CollectionUtils;
import infra.util.ReflectionUtils;

import static java.lang.reflect.Modifier.FINAL;
import static java.lang.reflect.Modifier.STATIC;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2019-09-03 19:01
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

  protected MethodInfo() {

  }

  public abstract ClassInfo getClassInfo();

  public abstract int getModifiers();

  public abstract MethodSignature getSignature();

  public abstract Type @Nullable [] getExceptionTypes();

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

      private @Nullable ClassInfo ci;

      public ClassInfo getClassInfo() {
        if (ci == null)
          ci = ClassInfo.from(member.getDeclaringClass());
        return ci;
      }

      public int getModifiers() {
        return modifiers;
      }

      @Nullable MethodSignature sig;

      public MethodSignature getSignature() {
        if (sig == null) {
          sig = MethodSignature.from(member);
        }
        return sig;
      }

      public Type @Nullable [] getExceptionTypes() {
        return Type.forExceptionTypes(member);
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
