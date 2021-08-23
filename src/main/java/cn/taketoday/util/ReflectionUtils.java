/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.reflect.FieldGetterMethod;
import cn.taketoday.core.reflect.FieldPropertyAccessor;
import cn.taketoday.core.reflect.FieldSetterMethod;
import cn.taketoday.core.reflect.GetterMethod;
import cn.taketoday.core.reflect.MethodAccessorGetterMethod;
import cn.taketoday.core.reflect.MethodAccessorPropertyAccessor;
import cn.taketoday.core.reflect.MethodAccessorSetterMethod;
import cn.taketoday.core.reflect.MethodInvoker;
import cn.taketoday.core.reflect.PropertyAccessor;
import cn.taketoday.core.reflect.ReadOnlyFieldPropertyAccessor;
import cn.taketoday.core.reflect.ReadOnlyMethodAccessorPropertyAccessor;
import cn.taketoday.core.reflect.ReflectionException;
import cn.taketoday.core.reflect.SetterMethod;

/**
 * Fast reflection operation
 *
 * @author TODAY 2020-08-13 18:45
 * @since 2.1.7
 */
public abstract class ReflectionUtils {

  /**
   * Pre-built MethodFilter that matches all non-bridge non-synthetic methods
   * which are not declared on {@code java.lang.Object}.
   */
  public static final MethodFilter USER_DECLARED_METHODS
          = (method -> !method.isBridge() && !method.isSynthetic());

  /**
   * Pre-built FieldFilter that matches all non-static, non-final fields.
   */
  public static final FieldFilter COPYABLE_FIELDS = //
          (field -> !(Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())));

  /**
   * Naming prefix for CGLIB-renamed methods.
   *
   * @see #isCglibRenamedMethod
   */
  public static final String CGLIB_RENAMED_METHOD_PREFIX = "today$";
  private static final Field[] EMPTY_FIELD_ARRAY = Constant.EMPTY_FIELD_ARRAY;
  private static final Method[] EMPTY_METHOD_ARRAY = Constant.EMPTY_METHOD_ARRAY;
  private static final Object[] EMPTY_OBJECT_ARRAY = Constant.EMPTY_OBJECT_ARRAY;

  /**
   * Cache for {@link Class#getDeclaredFields()}, allowing for fast iteration.
   */
  private static final ConcurrentCache<Class<?>, Field[]>
          DECLARED_FIELDS_CACHE = ConcurrentCache.create(256);
  /**
   * Cache for {@link Class#getDeclaredMethods()} plus equivalent default methods
   * from Java 8 based interfaces, allowing for fast iteration.
   */
  private static final ConcurrentCache<Class<?>, Method[]>
          DECLARED_METHODS_CACHE = ConcurrentCache.create(256);

  // Exception handling

  /**
   * Handle the given reflection exception.
   * <p>
   * Should only be called if no checked exception is expected to be thrown by a
   * target method, or if an error occurs while accessing a method or field.
   * <p>
   * Throws the underlying RuntimeException or Error in case of an
   * InvocationTargetException with such a root cause. Throws an
   * IllegalStateException with an appropriate message or
   * UndeclaredThrowableException otherwise.
   *
   * @param ex
   *         the reflection exception to handle
   */
  public static void handleReflectionException(Exception ex) {
    if (ex instanceof NoSuchMethodException) {
      throw new IllegalStateException("Method not found: " + ex.getMessage());
    }
    if (ex instanceof IllegalAccessException) {
      throw new IllegalStateException("Could not access method or field: " + ex.getMessage());
    }
    if (ex instanceof InvocationTargetException) {
      handleInvocationTargetException((InvocationTargetException) ex);
    }
    if (ex instanceof RuntimeException) {
      throw (RuntimeException) ex;
    }
    throw new UndeclaredThrowableException(ex);
  }

  /**
   * Handle the given invocation target exception. Should only be called if no
   * checked exception is expected to be thrown by the target method.
   * <p>
   * Throws the underlying RuntimeException or Error in case of such a root cause.
   * Throws an UndeclaredThrowableException otherwise.
   *
   * @param ex
   *         the invocation target exception to handle
   */
  public static void handleInvocationTargetException(InvocationTargetException ex) {
    rethrowRuntimeException(ex.getTargetException());
  }

  /**
   * Rethrow the given {@link Throwable exception}, which is presumably the
   * <em>target exception</em> of an {@link InvocationTargetException}. Should
   * only be called if no checked exception is expected to be thrown by the target
   * method.
   * <p>
   * Rethrows the underlying exception cast to a {@link RuntimeException} or
   * {@link Error} if appropriate; otherwise, throws an
   * {@link UndeclaredThrowableException}.
   *
   * @param ex
   *         the exception to rethrow
   *
   * @throws RuntimeException
   *         the rethrown exception
   */
  public static void rethrowRuntimeException(Throwable ex) {
    if (ex instanceof RuntimeException) {
      throw (RuntimeException) ex;
    }
    if (ex instanceof Error) {
      throw (Error) ex;
    }
    throw new UndeclaredThrowableException(ex);
  }

  /**
   * Rethrow the given {@link Throwable exception}, which is presumably the
   * <em>target exception</em> of an {@link InvocationTargetException}. Should
   * only be called if no checked exception is expected to be thrown by the target
   * method.
   * <p>
   * Rethrows the underlying exception cast to an {@link Exception} or
   * {@link Error} if appropriate; otherwise, throws an
   * {@link UndeclaredThrowableException}.
   *
   * @param ex
   *         the exception to rethrow
   *
   * @throws Exception
   *         the rethrown exception (in case of a checked exception)
   */
  public static void rethrowException(Throwable ex) throws Exception {
    if (ex instanceof Exception) {
      throw (Exception) ex;
    }
    if (ex instanceof Error) {
      throw (Error) ex;
    }
    throw new UndeclaredThrowableException(ex);
  }

  // Method handling

  /**
   * Attempt to find a {@link Method} on the supplied class with the supplied name
   * and no parameters. Searches all superclasses up to {@code Object}.
   * <p>
   * Returns {@code null} if no {@link Method} can be found.
   *
   * @param clazz
   *         the class to introspect
   * @param name
   *         the name of the method
   *
   * @return the Method object, or {@code null} if none found
   */
  public static Method findMethod(Class<?> clazz, String name) {
    return findMethod(clazz, name, (Class<?>[]) null);
  }

  /**
   * Attempt to find a {@link Method} on the supplied class with the supplied name
   * and parameter types. Searches all superclasses up to {@code Object}.
   * <p>
   * Returns {@code null} if no {@link Method} can be found.
   *
   * @param clazz
   *         the class to introspect
   * @param name
   *         the name of the method
   * @param paramTypes
   *         the parameter types of the method (may be {@code null} to indicate
   *         any signature)
   *
   * @return the Method object, or {@code null} if none found
   */
  public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(name, "Method name must not be null");
    Class<?> searchType = clazz;
    while (searchType != null) {
      Method[] methods = (searchType.isInterface() ? searchType.getMethods() : getDeclaredMethods(searchType, false));
      for (Method method : methods) {
        if (name.equals(method.getName())
                && (paramTypes == null || hasSameParams(method, paramTypes))) {
          return method;
        }
      }
      searchType = searchType.getSuperclass();
    }
    return null;
  }

  private static boolean hasSameParams(Method method, Class<?>[] paramTypes) {
    return paramTypes.length == method.getParameterCount()
            && Arrays.equals(paramTypes, method.getParameterTypes());
  }

  /**
   * Invoke the specified {@link Method} against the supplied target object with
   * no arguments. The target object can be {@code null} when invoking a static
   * {@link Method}.
   * <p>
   * Thrown exceptions are handled via a call to
   * {@link #handleReflectionException}.
   *
   * @param method
   *         the method to invoke
   * @param target
   *         the target object to invoke the method on
   *
   * @return the invocation result, if any
   *
   * @see #invokeMethod(java.lang.reflect.Method, Object, Object[])
   */
  public static Object invokeMethod(Method method, Object target) {
    return invokeMethod(method, target, EMPTY_OBJECT_ARRAY);
  }

  /**
   * Invoke the specified {@link Method} against the supplied target object with
   * the supplied arguments. The target object can be {@code null} when invoking a
   * static {@link Method}.
   * <p>
   * Thrown exceptions are handled via a call to
   * {@link #handleReflectionException}.
   *
   * @param method
   *         the method to invoke
   * @param target
   *         the target object to invoke the method on
   * @param args
   *         the invocation arguments (may be {@code null})
   *
   * @return the invocation result, if any
   */
  public static Object invokeMethod(Method method, Object target, Object... args) {
    try {
      return method.invoke(target, args);
    }
    catch (Exception ex) {
      handleReflectionException(ex);
    }
    throw new IllegalStateException("Should never get here");
  }

  public static Object accessInvokeMethod(Method method, Object target, Object... args) {
    return invokeMethod(makeAccessible(method), target, args);
  }

  public static Object invokeConstructor(Constructor<?> constructor, Object[] args) {
    try {
      return constructor.newInstance(args);
    }
    catch (Exception ex) {
      handleReflectionException(ex);
    }
    throw new IllegalStateException("Should never get here");
  }

  /**
   * Determine whether the given method explicitly declares the given exception or
   * one of its superclasses, which means that an exception of that type can be
   * propagated as-is within a reflective invocation.
   *
   * @param method
   *         the declaring method
   * @param exceptionType
   *         the exception to throw
   *
   * @return {@code true} if the exception can be thrown as-is; {@code false} if
   * it needs to be wrapped
   */
  public static boolean declaresException(Method method, Class<?> exceptionType) {
    Assert.notNull(method, "Method must not be null");
    Class<?>[] declaredExceptions = method.getExceptionTypes();
    for (Class<?> declaredException : declaredExceptions) {
      if (declaredException.isAssignableFrom(exceptionType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Perform the given callback operation on all matching methods of the given
   * class, as locally declared or equivalent thereof (such as default methods on
   * Java 8 based interfaces that the given class implements).
   *
   * @param clazz
   *         the class to introspect
   * @param mc
   *         the callback to invoke for each method
   *
   * @throws IllegalStateException
   *         if introspection fails
   * @see #doWithMethods
   */
  public static void doWithLocalMethods(Class<?> clazz, MethodCallback mc) {
    Method[] methods = getDeclaredMethods(clazz, false);
    for (Method method : methods) {
      try {
        mc.doWith(method);
      }
      catch (IllegalAccessException ex) {
        throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
      }
    }
  }

  /**
   * Perform the given callback operation on all matching methods of the given
   * class and superclasses.
   * <p>
   * The same named method occurring on subclass and superclass will appear twice,
   * unless excluded by a {@link MethodFilter}.
   *
   * @param clazz
   *         the class to introspect
   * @param mc
   *         the callback to invoke for each method
   *
   * @throws IllegalStateException
   *         if introspection fails
   * @see #doWithMethods(Class, MethodCallback, MethodFilter)
   */
  public static void doWithMethods(Class<?> clazz, MethodCallback mc) {
    doWithMethods(clazz, mc, null);
  }

  /**
   * Perform the given callback operation on all matching methods of the given
   * class and superclasses (or given interface and super-interfaces).
   * <p>
   * The same named method occurring on subclass and superclass will appear twice,
   * unless excluded by the specified {@link MethodFilter}.
   *
   * @param clazz
   *         the class to introspect
   * @param mc
   *         the callback to invoke for each method
   * @param mf
   *         the filter that determines the methods to apply the callback to
   *
   * @throws IllegalStateException
   *         if introspection fails
   */
  public static void doWithMethods(Class<?> clazz, MethodCallback mc, MethodFilter mf) {
    // Keep backing up the inheritance hierarchy.
    Method[] methods = getDeclaredMethods(clazz, false);
    for (Method method : methods) {
      if (mf != null && !mf.matches(method)) {
        continue;
      }
      try {
        mc.doWith(method);
      }
      catch (IllegalAccessException ex) {
        throw new IllegalStateException("Not allowed to access method '" + method.getName() + "': " + ex);
      }
    }
    if (clazz.getSuperclass() != null
            && (mf != USER_DECLARED_METHODS || clazz.getSuperclass() != Object.class)) {
      doWithMethods(clazz.getSuperclass(), mc, mf);
    }
    else if (clazz.isInterface()) {
      for (Class<?> superIfc : clazz.getInterfaces()) {
        doWithMethods(superIfc, mc, mf);
      }
    }
  }

  /**
   * Get all declared methods on the leaf class and all superclasses. Leaf class
   * methods are included first.
   *
   * @param leafClass
   *         the class to introspect
   *
   * @throws IllegalStateException
   *         if introspection fails
   */
  public static Method[] getAllDeclaredMethods(Class<?> leafClass) {
    final ArrayList<Method> methods = new ArrayList<>(32);
    doWithMethods(leafClass, methods::add);
    return methods.toArray(new Method[methods.size()]);
  }

  /**
   * Variant of {@link Class#getDeclaredMethods()} that uses a local cache in
   * order to avoid the JVM's SecurityManager check and new Method instances. In
   * addition, it also includes Java 8 default methods from locally implemented
   * interfaces, since those are effectively to be treated just like declared
   * methods.
   *
   * @param targetClass
   *         the class to introspect
   *
   * @return the cached array of methods
   *
   * @throws IllegalStateException
   *         if introspection fails
   * @see Class#getDeclaredMethods()
   */
  public static Method[] getDeclaredMethods(Class<?> targetClass) {
    return getDeclaredMethods(targetClass, true);
  }

  private static Method[] getDeclaredMethods(Class<?> targetClass, boolean defensive) {
    Assert.notNull(targetClass, "targetClass must not be null");
    Method[] result = DECLARED_METHODS_CACHE.get(targetClass);
    if (result == null) {
      try {
        Method[] declaredMethods = targetClass.getDeclaredMethods();
        List<Method> defaultMethods = findConcreteMethodsOnInterfaces(targetClass);
        if (defaultMethods != null) {
          result = new Method[declaredMethods.length + defaultMethods.size()];
          System.arraycopy(declaredMethods, 0, result, 0, declaredMethods.length);
          int index = declaredMethods.length;
          for (Method defaultMethod : defaultMethods) {
            result[index] = defaultMethod;
            index++;
          }
        }
        else {
          result = declaredMethods;
        }
        DECLARED_METHODS_CACHE.put(targetClass, (result.length == 0 ? EMPTY_METHOD_ARRAY : result));
      }
      catch (Throwable ex) {
        throw new IllegalStateException(
                "Failed to introspect Class [" + targetClass.getName() +
                        "] from ClassLoader [" + targetClass.getClassLoader() + "]", ex);
      }
    }
    return (result.length == 0 || !defensive) ? result : result.clone();
  }

  @Nullable
  private static List<Method> findConcreteMethodsOnInterfaces(Class<?> clazz) {
    ArrayList<Method> result = null;
    for (Class<?> ifc : clazz.getInterfaces()) {
      for (Method ifcMethod : ifc.getMethods()) {
        if (!Modifier.isAbstract(ifcMethod.getModifiers())) {
          if (result == null) {
            result = new ArrayList<>();
          }
          result.add(ifcMethod);
        }
      }
    }
    return result;
  }

  /**
   * Get the unique set of declared methods on the leaf class and all
   * superclasses. Leaf class methods are included first and while traversing the
   * superclass hierarchy any methods found with signatures matching a method
   * already included are filtered out.
   *
   * @param leafClass
   *         the class to introspect
   *
   * @throws IllegalStateException
   *         if introspection fails
   */
  public static Method[] getUniqueDeclaredMethods(Class<?> leafClass) {
    return getUniqueDeclaredMethods(leafClass, null);
  }

  /**
   * Get the unique set of declared methods on the leaf class and all
   * superclasses. Leaf class methods are included first and while traversing the
   * superclass hierarchy any methods found with signatures matching a method
   * already included are filtered out.
   *
   * @param leafClass
   *         the class to introspect
   * @param mf
   *         the filter that determines the methods to take into account
   *
   * @throws IllegalStateException
   *         if introspection fails
   */
  public static Method[] getUniqueDeclaredMethods(Class<?> leafClass, MethodFilter mf) {
    final ArrayList<Method> methods = new ArrayList<>(32);
    doWithMethods(leafClass, method -> {
      boolean knownSignature = false;
      Method methodBeingOverriddenWithCovariantReturnType = null;
      for (Method existingMethod : methods) {
        if (method.getName().equals(existingMethod.getName())
                && method.getParameterCount() == existingMethod.getParameterCount()
                && Arrays.equals(method.getParameterTypes(), existingMethod.getParameterTypes())) {
          // Is this a covariant return type situation?
          if (existingMethod.getReturnType() != method.getReturnType()
                  && existingMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
            methodBeingOverriddenWithCovariantReturnType = existingMethod;
          }
          else {
            knownSignature = true;
          }
          break;
        }
      }
      if (methodBeingOverriddenWithCovariantReturnType != null) {
        methods.remove(methodBeingOverriddenWithCovariantReturnType);
      }
      if (!knownSignature && !isCglibRenamedMethod(method)) {
        methods.add(method);
      }
    }, mf);
    return methods.toArray(EMPTY_METHOD_ARRAY);
  }

  /**
   * Determine whether the given method is an "equals" method.
   *
   * @see java.lang.Object#equals(Object)
   */
  public static boolean isEqualsMethod(Method method) {
    if (method == null || !method.getName().equals("equals")) {
      return false;
    }
    if (method.getParameterCount() != 1) {
      return false;
    }
    return method.getParameterTypes()[0] == Object.class;
  }

  /**
   * Determine whether the given method is a "hashCode" method.
   *
   * @see java.lang.Object#hashCode()
   */
  public static boolean isHashCodeMethod(Method method) {
    return (method != null && method.getName().equals("hashCode") && method.getParameterCount() == 0);
  }

  /**
   * Determine whether the given method is a "toString" method.
   *
   * @see java.lang.Object#toString()
   */
  public static boolean isToStringMethod(Method method) {
    return (method != null && method.getName().equals("toString") && method.getParameterCount() == 0);
  }

  /**
   * Determine whether the given method is originally declared by
   * {@link java.lang.Object}.
   */
  public static boolean isObjectMethod(Method method) {
    return (method != null && (method.getDeclaringClass() == Object.class ||
            isEqualsMethod(method) || isHashCodeMethod(method) || isToStringMethod(method)));
  }

  /**
   * Determine whether the given method is a "finalize" method.
   *
   * @see java.lang.Object#finalize()
   */
  public static boolean isFinalizeMethod(Method method) {
    return (method != null && method.getName().equals("finalize") &&
            method.getParameterCount() == 0);
  }

  /**
   * Determine whether the given method is a CGLIB 'renamed' method, following the
   * pattern "CGLIB$methodName$0".
   *
   * @param renamedMethod
   *         the method to check
   */
  public static boolean isCglibRenamedMethod(Method renamedMethod) {
    String name = renamedMethod.getName();
    if (name.startsWith(CGLIB_RENAMED_METHOD_PREFIX)) {
      int i = name.length() - 1;
      while (i >= 0 && Character.isDigit(name.charAt(i))) {
        i--;
      }
      return (i > CGLIB_RENAMED_METHOD_PREFIX.length() && (i < name.length() - 1) && name.charAt(i) == '$');
    }
    return false;
  }

  /**
   * Make the given method accessible, explicitly setting it accessible if
   * necessary. The {@code setAccessible(true)} method is only called when
   * actually necessary, to avoid unnecessary conflicts with a JVM SecurityManager
   * (if active).
   *
   * @param method
   *         the method to make accessible
   *
   * @see java.lang.reflect.Method#setAccessible
   */
  @SuppressWarnings("deprecation") // on JDK 9
  public static Method makeAccessible(Method method) {
    if ((!Modifier.isPublic(method.getModifiers()) ||
            !Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible()) {
      method.setAccessible(true);
    }
    return method;
  }

  // Field handling

  /**
   * Attempt to find a {@link Field field} on the supplied {@link Class} with the
   * supplied {@code name}. Searches all superclasses up to {@link Object}.
   *
   * @param clazz
   *         the class to introspect
   * @param name
   *         the name of the field
   *
   * @return the corresponding Field object, or {@code null} if not found
   */
  public static Field findField(Class<?> clazz, String name) {
    return findField(clazz, name, null);
  }

  /**
   * Attempt to find a {@link Field field} on the supplied {@link Class} with the
   * supplied {@code name} and/or {@link Class type}. Searches all superclasses up
   * to {@link Object}.
   *
   * @param clazz
   *         the class to introspect
   * @param name
   *         the name of the field (may be {@code null} if type is specified)
   * @param type
   *         the type of the field (may be {@code null} if name is specified)
   *
   * @return the corresponding Field object, or {@code null} if not found
   */
  public static Field findField(Class<?> clazz, String name, Class<?> type) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.isTrue(name != null || type != null, "Either name or type of the field must be specified");
    Class<?> searchType = clazz;
    while (Object.class != searchType && searchType != null) {
      Field[] fields = getDeclaredFields(searchType);
      for (Field field : fields) {
        if ((name == null || name.equals(field.getName()))
                && (type == null || type.equals(field.getType()))) {
          return field;
        }
      }
      searchType = searchType.getSuperclass();
    }
    return null;
  }

  /**
   * Set the field represented by the supplied {@linkplain Field field object} on
   * the specified {@linkplain Object target object} to the specified
   * {@code value}.
   * <p>
   * In accordance with {@link Field#set(Object, Object)} semantics, the new value
   * is automatically unwrapped if the underlying field has a primitive type.
   * <p>
   * This method does not support setting {@code static final} fields.
   * <p>
   * Thrown exceptions are handled via a call to
   * {@link #handleReflectionException(Exception)}.
   *
   * @param field
   *         the field to set
   * @param target
   *         the target object on which to set the field
   * @param value
   *         the value to set (may be {@code null})
   */
  public static void setField(Field field, Object target, Object value) {
    try {
      field.set(target, value);
    }
    catch (IllegalAccessException ex) {
      handleReflectionException(ex);
    }
  }

  /**
   * Get the field represented by the supplied {@link Field field object} on the
   * specified {@link Object target object}. In accordance with
   * {@link Field#get(Object)} semantics, the returned value is automatically
   * wrapped if the underlying field has a primitive type.
   * <p>
   * Thrown exceptions are handled via a call to
   * {@link #handleReflectionException(Exception)}.
   *
   * @param field
   *         the field to get
   * @param target
   *         the target object from which to get the field
   *
   * @return the field's current value
   */
  public static Object getField(Field field, Object target) {
    try {
      return field.get(target);
    }
    catch (IllegalAccessException ex) {
      handleReflectionException(ex);
    }
    throw new IllegalStateException("Should never get here");
  }

  /**
   * Invoke the given callback on all locally declared fields in the given class.
   *
   * @param clazz
   *         the target class to analyze
   * @param fc
   *         the callback to invoke for each field
   *
   * @throws IllegalStateException
   *         if introspection fails
   * @see #doWithFields
   */
  public static void doWithLocalFields(Class<?> clazz, FieldCallback fc) {
    for (Field field : getDeclaredFields(clazz)) {
      try {
        fc.doWith(field);
      }
      catch (IllegalAccessException ex) {
        throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
      }
    }
  }

  /**
   * Invoke the given callback on all fields in the target class, going up the
   * class hierarchy to get all declared fields.
   *
   * @param clazz
   *         the target class to analyze
   * @param fc
   *         the callback to invoke for each field
   *
   * @throws IllegalStateException
   *         if introspection fails
   */
  public static void doWithFields(Class<?> clazz, FieldCallback fc) {
    doWithFields(clazz, fc, null);
  }

  /**
   * Invoke the given callback on all fields in the target class, going up the
   * class hierarchy to get all declared fields.
   *
   * @param clazz
   *         the target class to analyze
   * @param fc
   *         the callback to invoke for each field
   * @param ff
   *         the filter that determines the fields to apply the callback to
   *
   * @throws IllegalStateException
   *         if introspection fails
   */
  public static void doWithFields(Class<?> clazz, FieldCallback fc, FieldFilter ff) {
    // Keep backing up the inheritance hierarchy.
    Class<?> targetClass = clazz;
    do {
      Field[] fields = getDeclaredFields(targetClass);
      for (Field field : fields) {
        if (ff != null && !ff.matches(field)) {
          continue;
        }
        try {
          fc.doWith(field);
        }
        catch (IllegalAccessException ex) {
          throw new IllegalStateException("Not allowed to access field '" + field.getName() + "': " + ex);
        }
      }
      targetClass = targetClass.getSuperclass();
    }
    while (targetClass != null && targetClass != Object.class);
  }

  /**
   * This variant retrieves {@link Class#getDeclaredFields()} from a local cache
   * in order to avoid the JVM's SecurityManager check and defensive array
   * copying.
   *
   * @param clazz
   *         the class to introspect
   *
   * @return the cached array of fields
   *
   * @throws IllegalStateException
   *         if introspection fails
   * @see Class#getDeclaredFields()
   */
  private static Field[] getDeclaredFields(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    Field[] result = DECLARED_FIELDS_CACHE.get(clazz);
    if (result == null) {
      try {
        result = clazz.getDeclaredFields();
        DECLARED_FIELDS_CACHE.put(clazz, (result.length == 0 ? EMPTY_FIELD_ARRAY : result));
      }
      catch (Throwable ex) {
        throw new IllegalStateException(
                "Failed to introspect Class [" + clazz.getName() +
                        "] from ClassLoader [" + clazz.getClassLoader() + "]", ex);
      }
    }
    return result;
  }

  /**
   * Given the source object and the destination, which must be the same class or
   * a subclass, copy all fields, including inherited fields. Designed to work on
   * objects with public no-arg constructors.
   *
   * @throws IllegalStateException
   *         if introspection fails
   */
  public static void shallowCopyFieldState(final Object src, final Object dest) {
    Assert.notNull(src, "Source for field copy cannot be null");
    Assert.notNull(dest, "Destination for field copy cannot be null");
    if (!src.getClass().isAssignableFrom(dest.getClass())) {
      throw new IllegalArgumentException(
              "Destination class [" + dest.getClass().getName() +
                      "] must be same or subclass as source class [" + src.getClass().getName() + "]");
    }

    final class FieldCallback0 implements FieldCallback {

      @Override
      public void doWith(final Field field) {
        copyField(field, src, dest);
      }
    }

    doWithFields(src.getClass(), new FieldCallback0(), COPYABLE_FIELDS);
  }

  /**
   * Copy a given field from the source object to the destination
   *
   * @param field
   *         target field property
   */
  public static void copyField(final Field field, final Object src, final Object dest) {
    makeAccessible(field);
    setField(field, dest, getField(field, src));
  }

  /**
   * Make the given field accessible, explicitly setting it accessible if
   * necessary. The {@code setAccessible(true)} method is only called when
   * actually necessary, to avoid unnecessary conflicts with a JVM SecurityManager
   * (if active).
   *
   * @param field
   *         the field to make accessible
   *
   * @see java.lang.reflect.Field#setAccessible
   */
  @SuppressWarnings("deprecation") // on JDK 9
  public static Field makeAccessible(Field field) {
    if ((!Modifier.isPublic(field.getModifiers()) ||
            !Modifier.isPublic(field.getDeclaringClass().getModifiers()) ||
            Modifier.isFinal(field.getModifiers())) && !field.isAccessible()) {
      field.setAccessible(true);
    }
    return field;
  }

  /**
   * Determine whether the given field is a "public static final" constant.
   *
   * @param field
   *         the field to check
   */
  public static boolean isPublicStaticFinal(Field field) {
    int modifiers = field.getModifiers();
    return (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers));
  }

  /**
   * Get bean instance's {@link Field}
   *
   * @param target
   *         target instance
   *
   * @return all {@link Field}
   *
   * @since 2.1.5
   */
  public static Collection<Field> getFields(Object target) {
    return getFields(target.getClass());
  }

  /**
   * Get all {@link Field} list, including superclass's Field
   *
   * @param targetClass
   *         target class
   *
   * @return get all the {@link Field}
   *
   * @since 2.1.2
   */
  public static Collection<Field> getFields(Class<?> targetClass) {

    final ArrayList<Field> list = new ArrayList<>(64);
    do {
      Collections.addAll(list, getDeclaredFields(targetClass));
    }
    while ((targetClass = targetClass.getSuperclass()) != Object.class && targetClass != null);

    return list;
  }

  /**
   * Get all {@link Field} array
   *
   * @param targetClass
   *         target class
   *
   * @return get all the {@link Field} array
   *
   * @since 2.1.2
   */
  public static Field[] getFieldArray(Class<?> targetClass) {
    final Collection<Field> fields = getFields(targetClass);
    return fields.toArray(new Field[fields.size()]);
  }

  public static <T> Constructor<T> accessibleConstructor(final Class<T> targetClass, final Class<?>... parameterTypes)
          throws NoSuchMethodException //
  {
    Assert.notNull(targetClass, "targetClass must not be null");
    return makeAccessible(targetClass.getDeclaredConstructor(parameterTypes));
  }

  public static <T> Constructor<T> makeAccessible(Constructor<T> constructor) {
    Assert.notNull(constructor, "constructor must not be null");

    if ((!Modifier.isPublic(constructor.getModifiers()) //
            || !Modifier.isPublic(constructor.getDeclaringClass().getModifiers())) && !constructor.isAccessible()) {

      constructor.setAccessible(true);
    }
    return constructor;
  }

  // Cache handling

  /**
   * Clear the internal method/field cache.
   */
  public static void clearCache() {
    DECLARED_FIELDS_CACHE.clear();
    DECLARED_METHODS_CACHE.clear();
  }

  public static Field obtainField(Class<?> clazz, String name) {
    final Field field = findField(clazz, name);
    if (field == null) {
      throw new ReflectionException("No such field named: " + name + " in class: " + clazz.getName());
    }
    return field;
  }

  public static Method obtainMethod(final String methodName,
                                    final Class<?> targetClass,
                                    final Class<?>... parameterTypes) {
    final Method declaredMethod = findMethod(targetClass, methodName, parameterTypes);
    if (declaredMethod == null) {
      throw new ReflectionException("No such method named: " + methodName + " in class: " + targetClass.getName());
    }
    return declaredMethod;
  }

  // Accessor
  // --------------------------------

  /**
   * find getter method
   *
   * @since 3.0.2
   */
  public static Method getReadMethod(Field field) {
    final Class<?> type = field.getType();
    final String propertyName = field.getName();
    return getReadMethod(field.getDeclaringClass(), type, propertyName);
  }

  /**
   * find getter method
   *
   * @since 3.0.2
   */
  public static Method getReadMethod(Class<?> declaredClass, Class<?> type, String name) {
    final String getterName = getterPropertyName(StringUtils.capitalize(name), type);
    for (final Method declaredMethod : declaredClass.getDeclaredMethods()) {
      if (declaredMethod.getName().equals(getterName)
              && declaredMethod.getParameterCount() == 0 && declaredMethod.getReturnType() == type) {
        return declaredMethod;
      }
    }
    return null;
  }

  /**
   * find setter method
   *
   * @since 3.0.2
   */
  public static Method getWriteMethod(Field field) {
    final Class<?> type = field.getType();
    final String propertyName = field.getName();
    return getWriteMethod(field.getDeclaringClass(), type, propertyName);
  }

  /**
   * find setter method
   *
   * @since 3.0.2
   */
  public static Method getWriteMethod(Class<?> declaredClass, Class<?> type, String name) {
    final String setterName = "set".concat(StringUtils.capitalize(name));
    for (final Method declaredMethod : declaredClass.getDeclaredMethods()) {
      if (declaredMethod.getName().equals(setterName)) {
        final Class<?>[] parameterTypes = declaredMethod.getParameterTypes();
        if (parameterTypes.length == 1 && parameterTypes[0] == type) {
          return declaredMethod;
        }
      }
    }
    return null;
  }

  public static PropertyAccessor newPropertyAccessor(final Field field) {
    Assert.notNull(field, "field must not be null");
    final Method readMethod = getReadMethod(field);
    final boolean isReadOnly = Modifier.isFinal(field.getModifiers());
    if (isReadOnly && readMethod != null) {
      return new ReadOnlyMethodAccessorPropertyAccessor(MethodInvoker.fromMethod(readMethod));
    }
    final Method writeMethod = getWriteMethod(field);
    if (writeMethod != null && readMethod != null) {
      return new MethodAccessorPropertyAccessor(writeMethod, readMethod);
    }
    if (writeMethod != null) {
      final MethodInvoker accessor = MethodInvoker.fromMethod(writeMethod);
      makeAccessible(field);
      return new PropertyAccessor() {
        @Override
        public Object get(Object obj) {
          return ReflectionUtils.getField(field, obj);
        }

        @Override
        public void set(Object obj, Object value) {
          accessor.invoke(obj, new Object[] { value });
        }

        @Override
        public Method getWriteMethod() {
          return writeMethod;
        }
      };
    }

    if (readMethod != null) {
      makeAccessible(field);
      final MethodInvoker accessor = MethodInvoker.fromMethod(readMethod);
      return new PropertyAccessor() {
        @Override
        public Object get(Object obj) {
          return accessor.invoke(obj, null);
        }

        @Override
        public void set(Object obj, Object value) {
          ReflectionUtils.setField(field, obj, value);
        }

        @Override
        public Method getReadMethod() {
          return readMethod;
        }
      };
    }

    // readMethod == null && setMethod == null
    return isReadOnly
           ? new ReadOnlyFieldPropertyAccessor(field, null)
           : new FieldPropertyAccessor(field, null, null);
  }

  // GetterMethod
  // ---------------------

  public static GetterMethod newGetterMethod(final Field field) {
    Assert.notNull(field, "field must not be null");
    final Method readMethod = getReadMethod(field);
    if (readMethod == null) {
      return new FieldGetterMethod(field);
    }
    return newGetterMethod(readMethod);
  }

  private static String getterPropertyName(final String name, final Class<?> type) {
    return (type == boolean.class ? "is" : "get").concat(name);
  }

  public static GetterMethod newGetterMethod(final Method method) {
    return new MethodAccessorGetterMethod(MethodInvoker.fromMethod(method));
  }

  // SetterMethod
  // ----------------------

  public static SetterMethod newSetterMethod(final Field field) {
    final Method writeMethod = getWriteMethod(field);
    if (writeMethod == null) {
      return new FieldSetterMethod(field);
    }
    return newSetterMethod(writeMethod);
  }

  public static SetterMethod newSetterMethod(final Method method) {
    final MethodInvoker accessor = MethodInvoker.fromMethod(method);
    return new MethodAccessorSetterMethod(accessor);
  }

  /**
   * Action to take on each method.
   */
  @FunctionalInterface
  public interface MethodCallback {

    /**
     * Perform an operation using the given method.
     *
     * @param method
     *         the method to operate on
     */
    void doWith(Method method) throws IllegalArgumentException, IllegalAccessException;
  }

  /**
   * Callback optionally used to filter methods to be operated on by a method
   * callback.
   */
  @FunctionalInterface
  public interface MethodFilter {

    /**
     * Determine whether the given method matches.
     *
     * @param method
     *         the method to check
     */
    boolean matches(Method method);
  }

  /**
   * Callback interface invoked on each field in the hierarchy.
   */
  @FunctionalInterface
  public interface FieldCallback {

    /**
     * Perform an operation using the given field.
     *
     * @param field
     *         the field to operate on
     */
    void doWith(Field field) throws IllegalArgumentException, IllegalAccessException;
  }

  /**
   * Callback optionally used to filter fields to be operated on by a field
   * callback.
   */
  @FunctionalInterface
  public interface FieldFilter {

    /**
     * Determine whether the given field matches.
     *
     * @param field
     *         the field to check
     */
    boolean matches(Field field);
  }

  //

}
