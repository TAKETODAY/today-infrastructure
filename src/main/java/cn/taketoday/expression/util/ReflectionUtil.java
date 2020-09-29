/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package cn.taketoday.expression.util;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.context.Constant;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionException;
import cn.taketoday.expression.MethodNotFoundException;
import cn.taketoday.expression.PropertyNotFoundException;
import cn.taketoday.expression.lang.ExpressionUtils;

import static java.beans.Introspector.getBeanInfo;

/**
 * Utilities for Managing Serialization and Reflection
 *
 * @author Jacob Hookom [jacob@hookom.net]
 * @version $Change: 181177 $$DateTime: 2001/06/26 08:45:09 $$Author: kchung $
 */
public abstract class ReflectionUtil {

  /**
   * Converts an array of Class names to Class types
   *
   * @param s
   *
   * @return The array of Classes
   *
   * @throws ClassNotFoundException
   */
  @SuppressWarnings("rawtypes")
  public static Class[] toTypeArray(String[] s) throws ClassNotFoundException {
    if (s == null) return null;
    Class[] c = new Class[s.length];
    for (int i = 0; i < s.length; i++) {
      c[i] = ClassUtils.forName(s[i]);
    }
    return c;
  }

  /**
   * Converts an array of Class types to Class names
   *
   * @param c
   *
   * @return The array of Classes
   */
  @SuppressWarnings("rawtypes")
  public static String[] toClassNameArray(Class[] c) {
    if (c == null) return null;
    String[] s = new String[c.length];
    for (int i = 0; i < c.length; i++) {
      s[i] = c[i].getName();
    }
    return s;
  }

  /**
   * @param base
   *         The base object
   * @param property
   *         The property
   *
   * @return The PropertyDescriptor for the base with the given property
   *
   * @throws ExpressionException
   * @throws PropertyNotFoundException
   */
  public static PropertyDescriptor getPropertyDescriptor(final Object base, final Object property) throws ExpressionException {

    String name = ExpressionUtils.coerceToString(property);
    try {
      for (PropertyDescriptor desc : getBeanInfo(base.getClass()).getPropertyDescriptors()) {
        if (desc.getName().equals(name)) {
          return desc;
        }
      }
    }
    catch (IntrospectionException ie) {
      throw new ExpressionException(ie);
    }
    throw new PropertyNotFoundException("Property '" + name + "' not found on " + base + "");
  }

  /**
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  public static Object invokeMethod(ExpressionContext context, Method m, Object base, Object[] params) {

    Object[] parameters = buildParameters(context, m.getParameterTypes(), m.isVarArgs(), params);
    try {
      return m.invoke(base, parameters);
    }
    catch (IllegalAccessException | IllegalArgumentException iae) {
      throw new ExpressionException(iae);
    }
    catch (InvocationTargetException ite) {
      throw new ExpressionException(ite.getCause());
    }
  }

  public static Object invokeConstructor(ExpressionContext context, Constructor<?> c, Object[] params) {
    try {
      return c.newInstance(buildParameters(context, c.getParameterTypes(), c.isVarArgs(), params));
    }
    catch (IllegalAccessException | IllegalArgumentException iae) {
      throw new ExpressionException(iae);
    }
    catch (InvocationTargetException | InstantiationException ie) {
      throw new ExpressionException(ie.getCause());
    }
  }

  public static Constructor<?> findConstructor(Class<?> klass, Class<?>[] paramTypes, Object[] params) {

    if (klass == null) {
      throw new MethodNotFoundException("Method not found: " + klass
                                                + '.' + Constant.CONSTRUCTOR_NAME + '(' + paramString(paramTypes) + ')');
    }

    if (paramTypes == null) {
      paramTypes = getTypesFromValues(params);
    }

    Constructor<?>[] constructors = klass.getConstructors();

    List<Wrapper> wrappers = Wrapper.wrap(constructors);

    final String methodName = Constant.CONSTRUCTOR_NAME;
    Wrapper result = findWrapper(klass, wrappers, methodName, paramTypes, params);
    return result == null ? null : getConstructor(klass, result.unwrap());
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  public static Method findMethod(Class<?> clazz, String methodName,
                                  Class<?>[] paramTypes, Object[] paramValues) {

    if (clazz == null || methodName == null) {
      throw new MethodNotFoundException("Method not found: " + clazz + "." + methodName + "(" + paramString(paramTypes) + ")");
    }

    if (paramTypes == null) {
      paramTypes = getTypesFromValues(paramValues);
    }

    Method[] methods = clazz.getMethods();

    List<Wrapper> wrappers = Wrapper.wrap(methods, methodName);

    Wrapper result = findWrapper(clazz, wrappers, methodName, paramTypes, paramValues);

    return result == null ? null : getMethod(clazz, result.unwrap());
  }

  public static Method findMethod(Class<?> klass, String method, Class<?>[] paramTypes, Object[] params, boolean staticOnly) {

    Method m = findMethod(klass, method, paramTypes, params);
    if (staticOnly && !Modifier.isStatic(m.getModifiers())) {
      throw new MethodNotFoundException("Method " + method + "for class " + klass + " not found or accessible");
    }

    return m;
  }

  /**
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static Wrapper findWrapper(Class<?> clazz,
                                     List<Wrapper> wrappers,
                                     String name,
                                     Class<?>[] paramTypes,
                                     Object[] paramValues) //
  {
    ArrayList<Wrapper> varArgsCandidates = new ArrayList<>();
    ArrayList<Wrapper> coercibleCandidates = new ArrayList<>();
    ArrayList<Wrapper> assignableCandidates = new ArrayList<>();

    int paramCount;
    if (paramTypes == null) {
      paramCount = 0;
    }
    else {
      paramCount = paramTypes.length;
    }

    for (Wrapper w : wrappers) {
      Class<?>[] mParamTypes = w.getParameterTypes();
      int mParamCount;
      if (mParamTypes == null) {
        mParamCount = 0;
      }
      else {
        mParamCount = mParamTypes.length;
      }

      // Check the number of parameters
      if (!(paramCount == mParamCount || (w.isVarArgs() && paramCount >= mParamCount - 1))) {
        // Method has wrong number of parameters
        continue;
      }

      // Check the parameters match
      boolean assignable = false;
      boolean coercible = false;
      boolean varArgs = false;
      boolean noMatch = false;
      for (int i = 0; i < mParamCount; i++) {
        if (i == (mParamCount - 1) && w.isVarArgs()) {
          varArgs = true;
          // exact var array type match
          if (mParamCount == paramCount && mParamTypes[i] == paramTypes[i]) {
            continue;
          }

          // unwrap the array's component type
          Class<?> varType = mParamTypes[i].getComponentType();
          for (int j = i; j < paramCount; j++) {
            if (!isAssignableFrom(paramTypes[j], varType)
                    && !(paramValues != null && j < paramValues.length && isCoercibleFrom(paramValues[j], varType))) {
              noMatch = true;
              break;
            }
          }
        }
        //				else if (mParamTypes[i].equals(paramTypes[i])) {
        //				}
        else if (isAssignableFrom(paramTypes[i], mParamTypes[i])) {
          assignable = true;
        }
        else {
          if (paramValues == null || i >= paramValues.length) {
            noMatch = true;
            break;
          }
          else {
            if (isCoercibleFrom(paramValues[i], mParamTypes[i])) {
              coercible = true;
            }
            else {
              noMatch = true;
              break;
            }
          }
        }
      }
      if (noMatch) {
        continue;
      }

      if (varArgs) {
        varArgsCandidates.add(w);
      }
      else if (coercible) {
        coercibleCandidates.add(w);
      }
      else if (assignable) {
        assignableCandidates.add(w);
      }
      else {
        // If a method is found where every parameter matches exactly,
        // return it
        return w;
      }
    }

    if (!assignableCandidates.isEmpty()) {
      return findMostSpecificWrapper(assignableCandidates, paramTypes, false, errorMsg(clazz, name, paramTypes));
    }
    else if (!coercibleCandidates.isEmpty()) {
      return findMostSpecificWrapper(coercibleCandidates, paramTypes, true, errorMsg(clazz, name, paramTypes));
    }
    else if (!varArgsCandidates.isEmpty()) {
      return findMostSpecificWrapper(varArgsCandidates, paramTypes, true, errorMsg(clazz, name, paramTypes));
    }
    else {
      throw new MethodNotFoundException("Method not found: " + clazz + "." + name + "(" + paramString(paramTypes) + ")");
    }

  }

  final static Supplier<String> errorMsg(Class<?> clazz, String name, Class<?>[] paramTypes) {
    return () -> "Unable to find unambiguous method: " + clazz + "." + name + "(" + paramString(paramTypes) + ")";
  }

  /**
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static Wrapper findMostSpecificWrapper(List<Wrapper> candidates,
                                                 Class<?>[] matchingTypes,
                                                 boolean elSpecific,
                                                 Supplier<String> errorMsg) //
  {
    ArrayList<Wrapper> ambiguouses = new ArrayList<>();
    for (Wrapper candidate : candidates) {
      boolean lessSpecific = false;

      Iterator<Wrapper> it = ambiguouses.iterator();
      while (it.hasNext()) {
        int result = isMoreSpecific(candidate, it.next(), matchingTypes, elSpecific);
        if (result == 1) {
          it.remove();
        }
        else if (result == -1) {
          lessSpecific = true;
        }
      }

      if (!lessSpecific) {
        ambiguouses.add(candidate);
      }
    }

    if (ambiguouses.size() > 1) {
      throw new MethodNotFoundException(errorMsg.get());
    }

    return ambiguouses.get(0);
  }

  /**
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static int isMoreSpecific(Wrapper wrapper1, Wrapper wrapper2,
                                    Class<?>[] matchingTypes, boolean elSpecific) {
    Class<?>[] paramTypes1 = wrapper1.getParameterTypes();
    Class<?>[] paramTypes2 = wrapper2.getParameterTypes();

    if (wrapper1.isVarArgs()) {
      // JLS8 15.12.2.5 Choosing the Most Specific Method
      int length = Math.max(Math.max(paramTypes1.length, paramTypes2.length), matchingTypes.length);
      paramTypes1 = getComparingParamTypesForVarArgsMethod(paramTypes1, length);
      paramTypes2 = getComparingParamTypesForVarArgsMethod(paramTypes2, length);

      if (length > matchingTypes.length) {
        Class<?>[] matchingTypes2 = new Class<?>[length];
        System.arraycopy(matchingTypes, 0, matchingTypes2, 0, matchingTypes.length);
        matchingTypes = matchingTypes2;
      }
    }

    int result = 0;
    for (int i = 0; i < paramTypes1.length; i++) {
      if (paramTypes1[i] != paramTypes2[i]) {
        int r2 = isMoreSpecific(paramTypes1[i], paramTypes2[i], matchingTypes[i], elSpecific);
        if (r2 == 1) {
          if (result == -1) {
            return 0;
          }
          result = 1;
        }
        else if (r2 == -1) {
          if (result == 1) {
            return 0;
          }
          result = -1;
        }
        else {
          return 0;
        }
      }
    }

    if (result == 0) {
      // The nature of bridge methods is such that it actually
      // doesn't matter which one we pick as long as we pick
      // one. That said, pick the 'right' one (the non-bridge
      // one) anyway.
      result = Boolean.compare(wrapper1.isBridge(), wrapper2.isBridge());
    }

    return result;
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static int isMoreSpecific(Class<?> type1, Class<?> type2, Class<?> matchingType, boolean elSpecific) {
    type1 = getBoxingTypeIfPrimitive(type1);
    type2 = getBoxingTypeIfPrimitive(type2);
    if (type2.isAssignableFrom(type1)) {
      return 1;
    }
    else if (type1.isAssignableFrom(type2)) {
      return -1;
    }
    else {
      if (elSpecific) {
        /**
         * Number will be treated as more specific ASTInteger only return Long or
         * BigInteger, no Byte / Short / Integer. ASTFloatingPoint also.
         */
        if (matchingType != null && Number.class.isAssignableFrom(matchingType)) {
          boolean b1 = Number.class.isAssignableFrom(type1) || type1.isPrimitive();
          boolean b2 = Number.class.isAssignableFrom(type2) || type2.isPrimitive();
          if (b1 && !b2) {
            return 1;
          }
          else if (b2 && !b1) {
            return -1;
          }
          else {
            return 0;
          }
        }

        return 0;
      }
      else {
        return 0;
      }
    }
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static Class<?> getBoxingTypeIfPrimitive(Class<?> clazz) {
    if (clazz.isPrimitive()) {
      if (clazz == boolean.class) {
        return Boolean.class;
      }
      else if (clazz == char.class) {
        return Character.class;
      }
      else if (clazz == byte.class) {
        return Byte.class;
      }
      else if (clazz == short.class) {
        return Short.class;
      }
      else if (clazz == int.class) {
        return Integer.class;
      }
      else if (clazz == long.class) {
        return Long.class;
      }
      else if (clazz == float.class) {
        return Float.class;
      }
      else {
        return Double.class;
      }
    }
    return clazz;
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static Class<?>[] getComparingParamTypesForVarArgsMethod(Class<?>[] paramTypes, int length) {
    Class<?>[] result = new Class<?>[length];
    System.arraycopy(paramTypes, 0, result, 0, paramTypes.length - 1);
    Class<?> type = paramTypes[paramTypes.length - 1].getComponentType();
    for (int i = paramTypes.length - 1; i < length; i++) {
      result[i] = type;
    }
    return result;
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static final String paramString(Class<?>[] types) {
    if (types != null) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < types.length; i++) {
        if (types[i] == null) {
          sb.append("null, ");
        }
        else {
          sb.append(types[i].getName()).append(", ");
        }
      }
      if (sb.length() > 2) {
        sb.setLength(sb.length() - 2);
      }
      return sb.toString();
    }
    return null;
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  static boolean isAssignableFrom(Class<?> src, Class<?> target) {
    // src will always be an object
    // Short-cut. null is always assignable to an object and in EL null
    // can always be coerced to a valid value for a primitive
    if (src == null) {
      return true;
    }

    target = getBoxingTypeIfPrimitive(target);

    return target.isAssignableFrom(src);
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static boolean isCoercibleFrom(Object src, Class<?> target) {
    // TODO: This isn't pretty but it works. Significant refactoring would
    // be required to avoid the exception.
    try {
      ExpressionUtils.coerceToType(src, target);
    }
    catch (Exception e) {
      return false;
    }
    return true;
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync.
   */
  private static Class<?>[] getTypesFromValues(Object[] values) {
    if (values == null) {
      return null;
    }

    Class<?> result[] = new Class<?>[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        result[i] = null;
      }
      else {
        result[i] = values[i].getClass();
      }
    }
    return result;
  }

  /*
   * This method duplicates code in javax.el.ELUtil. When making changes keep the
   * code in sync. Get a public method form a public class or interface of a given
   * method. Note that if a PropertyDescriptor is obtained for a non-public class
   * that implements a public interface, the read/write methods will be for the
   * class, and therefore inaccessible. To correct this, a version of the same
   * method must be found in a superclass or interface.
   */
  static Method getMethod(Class<?> type, Method m) {
    if (m == null || Modifier.isPublic(type.getModifiers())) {
      return m;
    }
    Class<?>[] inf = type.getInterfaces();
    Method mp = null;
    for (int i = 0; i < inf.length; i++) {
      try {
        mp = inf[i].getMethod(m.getName(), m.getParameterTypes());
        mp = getMethod(mp.getDeclaringClass(), mp);
        if (mp != null) {
          return mp;
        }
      }
      catch (NoSuchMethodException e) {
        // Ignore
      }
    }
    Class<?> sup = type.getSuperclass();
    if (sup != null) {
      try {
        mp = sup.getMethod(m.getName(), m.getParameterTypes());
        mp = getMethod(mp.getDeclaringClass(), mp);
        if (mp != null) {
          return mp;
        }
      }
      catch (NoSuchMethodException e) {
        // Ignore
      }
    }
    return null;
  }

  static Constructor<?> getConstructor(Class<?> type, Constructor<?> c) {
    if (c == null || Modifier.isPublic(type.getModifiers())) {
      return c;
    }
    Constructor<?> cp = null;
    Class<?> sup = type.getSuperclass();
    if (sup != null) {
      try {
        cp = sup.getConstructor(c.getParameterTypes());
        cp = getConstructor(cp.getDeclaringClass(), cp);
        if (cp != null) {
          return cp;
        }
      }
      catch (NoSuchMethodException e) {
        // Ignore
      }
    }
    return null;
  }

  static Object[] buildParameters(ExpressionContext context, Class<?>[] parameterTypes,
                                  boolean isVarArgs, Object[] params) {
    Object[] parameters = null;
    if (parameterTypes.length > 0) {
      parameters = new Object[parameterTypes.length];
      int paramCount = params == null ? 0 : params.length;
      if (isVarArgs) {
        int varArgIndex = parameterTypes.length - 1;
        // First argCount-1 parameters are standard
        for (int i = 0; (i < varArgIndex && i < paramCount); i++) {
          parameters[i] = context.convertToType(params[i],
                                                parameterTypes[i]);
        }
        // Last parameter is the varargs
        if (parameterTypes.length == paramCount && parameterTypes[varArgIndex] == params[varArgIndex].getClass()) {
          parameters[varArgIndex] = params[varArgIndex];
        }
        else {
          Class<?> varArgClass = parameterTypes[varArgIndex].getComponentType();
          final Object varargs = Array.newInstance(varArgClass, (paramCount - varArgIndex));
          for (int i = varArgIndex; i < paramCount; i++) {
            Array.set(varargs, i - varArgIndex, context.convertToType(params[i], varArgClass));
          }
          parameters[varArgIndex] = varargs;
        }
      }
      else {
        for (int i = 0; i < parameterTypes.length && i < paramCount; i++) {
          parameters[i] = context.convertToType(params[i], parameterTypes[i]);
        }
      }
    }
    return parameters;
  }

  /**
   * This method duplicates code in ReflectionUtil. When making changes keep the
   * code in sync.
   */
  public abstract static class Wrapper {

    public static List<Wrapper> wrap(Method[] methods, String name) {
      ArrayList<Wrapper> result = new ArrayList<>();
      for (Method method : methods) {
        if (method.getName().equals(name)) {
          result.add(new MethodWrapper(method));
        }
      }
      return result;
    }

    public static List<Wrapper> wrap(Constructor<?>[] constructors) {
      ArrayList<Wrapper> result = new ArrayList<>();
      for (Constructor<?> constructor : constructors) {
        result.add(new ConstructorWrapper(constructor));
      }
      return result;
    }

    public abstract <T> T unwrap();

    public abstract Class<?>[] getParameterTypes();

    public abstract boolean isVarArgs();

    public abstract boolean isBridge();
  }

  private static class MethodWrapper extends Wrapper {

    private final Method m;

    public MethodWrapper(Method m) {
      this.m = m;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Method unwrap() {
      return m;
    }

    @Override
    public Class<?>[] getParameterTypes() {
      return m.getParameterTypes();
    }

    @Override
    public boolean isVarArgs() {
      return m.isVarArgs();
    }

    @Override
    public boolean isBridge() {
      return m.isBridge();
    }
  }

  private static class ConstructorWrapper extends Wrapper {
    private final Constructor<?> c;

    public ConstructorWrapper(Constructor<?> c) {
      this.c = c;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Constructor<?> unwrap() {
      return c;
    }

    @Override
    public Class<?>[] getParameterTypes() {
      return c.getParameterTypes();
    }

    @Override
    public boolean isVarArgs() {
      return c.isVarArgs();
    }

    @Override
    public boolean isBridge() {
      return false;
    }
  }

}
