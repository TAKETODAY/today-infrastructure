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

package cn.taketoday.core;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.ReflectionUtils.MethodFilter;

/**
 * Helper for resolving synthetic {@link Method#isBridge bridge Methods} to the
 * {@link Method} being bridged.
 *
 * <p>Given a synthetic {@link Method#isBridge bridge Method} returns the {@link Method}
 * being bridged. A bridge method may be created by the compiler when extending a
 * parameterized type whose methods have parameterized arguments. During runtime
 * invocation the bridge {@link Method} may be invoked and/or used via reflection.
 * When attempting to locate annotations on {@link Method Methods}, it is wise to check
 * for bridge {@link Method Methods} as appropriate and find the bridged {@link Method}.
 *
 * <p>See <a href="https://java.sun.com/docs/books/jls/third_edition/html/expressions.html#15.12.4.5">
 * The Java Language Specification</a> for more details on the use of bridge methods.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author TODAY 2021/9/6 21:51
 * @since 4.0
 */
public abstract class BridgeMethodResolver {

  private static final ConcurrentReferenceHashMap<Method, Method> cache = new ConcurrentReferenceHashMap<>();

  /**
   * Find the original method for the supplied {@link Method bridge Method}.
   * <p>It is safe to call this method passing in a non-bridge {@link Method} instance.
   * In such a case, the supplied {@link Method} instance is returned directly to the caller.
   * Callers are <strong>not</strong> required to check for bridging before calling this method.
   *
   * @param bridgeMethod the method to introspect
   * @return the original method (either the bridged method or the passed-in method
   * if no more specific one could be found)
   */
  public static Method findBridgedMethod(Method bridgeMethod) {
    if (!bridgeMethod.isBridge()) {
      return bridgeMethod;
    }
    Method bridgedMethod = cache.get(bridgeMethod);
    if (bridgedMethod == null) {
      // Gather all methods with matching name and parameter size.
      ArrayList<Method> candidateMethods = new ArrayList<>();
      MethodFilter filter = candidateMethod -> isBridgedCandidateFor(candidateMethod, bridgeMethod);
      ReflectionUtils.doWithMethods(bridgeMethod.getDeclaringClass(), candidateMethods::add, filter);
      if (!candidateMethods.isEmpty()) {
        bridgedMethod = candidateMethods.size() == 1 ?
                        candidateMethods.get(0) :
                        searchCandidates(candidateMethods, bridgeMethod);
      }
      if (bridgedMethod == null) {
        // A bridge method was passed in but we couldn't find the bridged method.
        // Let's proceed with the passed-in method and hope for the best...
        bridgedMethod = bridgeMethod;
      }
      cache.put(bridgeMethod, bridgedMethod);
    }
    return bridgedMethod;
  }

  /**
   * Returns {@code true} if the supplied '{@code candidateMethod}' can be
   * consider a validate candidate for the {@link Method} that is {@link Method#isBridge() bridged}
   * by the supplied {@link Method bridge Method}. This method performs inexpensive
   * checks and can be used quickly filter for a set of possible matches.
   */
  private static boolean isBridgedCandidateFor(Method candidateMethod, Method bridgeMethod) {
    return !candidateMethod.isBridge()
            && candidateMethod.getName().equals(bridgeMethod.getName())
            && candidateMethod.getParameterCount() == bridgeMethod.getParameterCount();
  }

  /**
   * Searches for the bridged method in the given candidates.
   *
   * @param candidateMethods the List of candidate Methods
   * @param bridgeMethod the bridge method
   * @return the bridged method, or {@code null} if none found
   */
  @Nullable
  private static Method searchCandidates(ArrayList<Method> candidateMethods, Method bridgeMethod) {
    if (candidateMethods.isEmpty()) {
      return null;
    }
    Method previousMethod = null;
    boolean sameSig = true;
    for (Method candidateMethod : candidateMethods) {
      if (isBridgeMethodFor(bridgeMethod, candidateMethod, bridgeMethod.getDeclaringClass())) {
        return candidateMethod;
      }
      else if (previousMethod != null) {
        sameSig = sameSig && Arrays.equals(
                candidateMethod.getGenericParameterTypes(), previousMethod.getGenericParameterTypes());
      }
      previousMethod = candidateMethod;
    }
    return (sameSig ? candidateMethods.get(0) : null);
  }

  /**
   * Determines whether or not the bridge {@link Method} is the bridge for the
   * supplied candidate {@link Method}.
   */
  static boolean isBridgeMethodFor(Method bridgeMethod, Method candidateMethod, Class<?> declaringClass) {
    if (isResolvedTypeMatch(candidateMethod, bridgeMethod, declaringClass)) {
      return true;
    }
    Method method = findGenericDeclaration(bridgeMethod);
    return (method != null && isResolvedTypeMatch(method, candidateMethod, declaringClass));
  }

  /**
   * Returns {@code true} if the {@link Type} signature of both the supplied
   * {@link Method#getGenericParameterTypes() generic Method} and concrete {@link Method}
   * are equal after resolving all types against the declaringType, otherwise
   * returns {@code false}.
   */
  private static boolean isResolvedTypeMatch(Method genericMethod, Method candidateMethod, Class<?> declaringClass) {
    Type[] genericParameters = genericMethod.getGenericParameterTypes();
    if (genericParameters.length != candidateMethod.getParameterCount()) {
      return false;
    }
    Class<?>[] candidateParameters = candidateMethod.getParameterTypes();
    for (int i = 0; i < candidateParameters.length; i++) {
      ResolvableType genericParameter = ResolvableType.forParameter(genericMethod, i, declaringClass);
      Class<?> candidateParameter = candidateParameters[i];
      if (candidateParameter.isArray()) {
        // An array type: compare the component type.
        if (!candidateParameter.getComponentType().equals(genericParameter.getComponentType().toClass())) {
          return false;
        }
      }
      // A non-array type: compare the type itself.
      if (!ClassUtils.resolvePrimitiveIfNecessary(candidateParameter).equals(
              ClassUtils.resolvePrimitiveIfNecessary(genericParameter.toClass()))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Searches for the generic {@link Method} declaration whose erased signature
   * matches that of the supplied bridge method.
   *
   * @throws IllegalStateException if the generic declaration cannot be found
   */
  @Nullable
  private static Method findGenericDeclaration(Method bridgeMethod) {
    // Search parent types for method that has same signature as bridge.
    Class<?> superclass = bridgeMethod.getDeclaringClass().getSuperclass();
    while (superclass != null && Object.class != superclass) {
      Method method = searchForMatch(superclass, bridgeMethod);
      if (method != null && !method.isBridge()) {
        return method;
      }
      superclass = superclass.getSuperclass();
    }

    Class<?>[] interfaces = ClassUtils.getAllInterfacesForClass(bridgeMethod.getDeclaringClass());
    return searchInterfaces(interfaces, bridgeMethod);
  }

  @Nullable
  private static Method searchInterfaces(Class<?>[] interfaces, Method bridgeMethod) {
    for (Class<?> ifc : interfaces) {
      Method method = searchForMatch(ifc, bridgeMethod);
      if (method != null && !method.isBridge()) {
        return method;
      }
      else {
        method = searchInterfaces(ifc.getInterfaces(), bridgeMethod);
        if (method != null) {
          return method;
        }
      }
    }
    return null;
  }

  /**
   * If the supplied {@link Class} has a declared {@link Method} whose signature matches
   * that of the supplied {@link Method}, then this matching {@link Method} is returned,
   * otherwise {@code null} is returned.
   */
  @Nullable
  private static Method searchForMatch(Class<?> type, Method bridgeMethod) {
    try {
      return type.getDeclaredMethod(bridgeMethod.getName(), bridgeMethod.getParameterTypes());
    }
    catch (NoSuchMethodException ex) {
      return null;
    }
  }

  /**
   * Compare the signatures of the bridge method and the method which it bridges. If
   * the parameter and return types are the same, it is a 'visibility' bridge method
   * introduced in Java 6 to fix https://bugs.java.com/view_bug.do?bug_id=6342411.
   * See also https://stas-blogspot.blogspot.com/2010/03/java-bridge-methods-explained.html
   *
   * @return whether signatures match as described
   */
  public static boolean isVisibilityBridgeMethodPair(Method bridgeMethod, Method bridgedMethod) {
    if (bridgeMethod == bridgedMethod) {
      return true;
    }
    return (bridgeMethod.getReturnType().equals(bridgedMethod.getReturnType()) &&
            bridgeMethod.getParameterCount() == bridgedMethod.getParameterCount() &&
            Arrays.equals(bridgeMethod.getParameterTypes(), bridgedMethod.getParameterTypes()));
  }

}
