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
package cn.taketoday.util;

import java.beans.Introspector;
import java.io.Closeable;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import cn.taketoday.core.GenericTypeResolver;
import cn.taketoday.bytecode.ClassReader;
import cn.taketoday.core.io.Resource;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;

/**
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @author TODAY
 * @see TypeUtils
 * @see ReflectionUtils
 * @since 2018-06-0? ?
 */
public abstract class ClassUtils {

  /** The CGLIB class separator: {@code "$$"}. */
  public static final String CGLIB_CLASS_SEPARATOR = "$$";
  public static final char INNER_CLASS_SEPARATOR = '$';
  /** Suffix for array class names: {@code "[]"}. */
  public static final String ARRAY_SUFFIX = "[]";
  /** Prefix for internal array class names: {@code "["}. */
  public static final String INTERNAL_ARRAY_PREFIX = "[";
  /** Prefix for internal non-primitive array class names: {@code "[L"}. */
  public static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";
  public static final String CLASS_FILE_SUFFIX = ".class";

  /** @since 3.0 */
  public static HashSet<Class<?>> primitiveTypes;

  /**
   * Map with primitive wrapper type as key and corresponding primitive
   * type as value, for example: Integer.class -> int.class.
   */
  private static final IdentityHashMap<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

  /**
   * Map with primitive type as key and corresponding wrapper
   * type as value, for example: int.class -> Integer.class.
   */
  private static final IdentityHashMap<Class<?>, Class<?>> primitiveTypeToWrapperMap = new IdentityHashMap<>(9);
  /**
   * Map with primitive type name as key and corresponding primitive
   * type as value, for example: "int" -> "int.class".
   */
  private static final HashMap<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

  /**
   * Map with common Java language class name as key and corresponding Class as value.
   * Primarily for efficient deserialization of remote invocations.
   */
  private static final HashMap<String, Class<?>> commonClassCache = new HashMap<>(64);

  /**
   * Common Java language interfaces which are supposed to be ignored
   * when searching for 'primary' user-level interfaces.
   */
  private static final Set<Class<?>> javaLanguageInterfaces;

  static {
    primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
    primitiveWrapperTypeMap.put(Byte.class, byte.class);
    primitiveWrapperTypeMap.put(Character.class, char.class);
    primitiveWrapperTypeMap.put(Double.class, double.class);
    primitiveWrapperTypeMap.put(Float.class, float.class);
    primitiveWrapperTypeMap.put(Integer.class, int.class);
    primitiveWrapperTypeMap.put(Long.class, long.class);
    primitiveWrapperTypeMap.put(Short.class, short.class);
    primitiveWrapperTypeMap.put(Void.class, void.class);

    // Map entry iteration is less expensive to initialize than forEach with lambdas
    for (Map.Entry<Class<?>, Class<?>> entry : primitiveWrapperTypeMap.entrySet()) {
      primitiveTypeToWrapperMap.put(entry.getValue(), entry.getKey());
      registerCommonClasses(entry.getKey());
    }

    Set<Class<?>> primitiveTypes = new HashSet<>(32);
    primitiveTypes.addAll(primitiveWrapperTypeMap.values());
    Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class,
            double[].class, float[].class, int[].class, long[].class, short[].class);
    for (Class<?> primitiveType : primitiveTypes) {
      primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
    }

    registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class,
            Float[].class, Integer[].class, Long[].class, Short[].class);
    registerCommonClasses(Number.class, Number[].class, String.class, String[].class,
            Class.class, Class[].class, Object.class, Object[].class);
    registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class,
            Error.class, StackTraceElement.class, StackTraceElement[].class);
    registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class,
            Collection.class, List.class, Set.class, Map.class, Map.Entry.class, Optional.class);

    Class<?>[] javaLanguageInterfaceArray = {
            Serializable.class, Externalizable.class, Closeable.class,
            AutoCloseable.class, Cloneable.class, Comparable.class
    };
    registerCommonClasses(javaLanguageInterfaceArray);
    javaLanguageInterfaces = Set.of(javaLanguageInterfaceArray);

    // Map primitive types
    // -------------------------------------------

    primitiveTypes.add(void.class);
    primitiveTypes.add(String.class);
    primitiveTypes.add(Byte.class);
    primitiveTypes.add(Short.class);
    primitiveTypes.add(Character.class);
    primitiveTypes.add(Integer.class);
    primitiveTypes.add(Long.class);
    primitiveTypes.add(Float.class);
    primitiveTypes.add(Double.class);
    primitiveTypes.add(Boolean.class);
    primitiveTypes.add(Date.class);
    primitiveTypes.add(Class.class);
    primitiveTypes.add(BigInteger.class);
    primitiveTypes.add(BigDecimal.class);

    primitiveTypes.add(URI.class);
    primitiveTypes.add(URL.class);
    primitiveTypes.add(Enum.class);
    primitiveTypes.add(Locale.class);
    primitiveTypes.add(Number.class);
    primitiveTypes.add(Temporal.class);
    primitiveTypes.add(CharSequence.class);

    ClassUtils.primitiveTypes = new HashSet<>(primitiveTypes);
  }

  /**
   * Register the given common classes with the ClassUtils cache.
   */
  private static void registerCommonClasses(Class<?>... commonClasses) {
    for (Class<?> clazz : commonClasses) {
      commonClassCache.put(clazz.getName(), clazz);
    }
  }

  /**
   * Return the default ClassLoader to use: typically the thread context
   * ClassLoader, if available; the ClassLoader that loaded the ClassUtils
   * class will be used as fallback.
   * <p>Call this method if you intend to use the thread context ClassLoader
   * in a scenario where you clearly prefer a non-null ClassLoader reference:
   * for example, for class path resource loading (but not necessarily for
   * {@code Class.forName}, which accepts a {@code null} ClassLoader
   * reference as well).
   *
   * @return the default ClassLoader (only {@code null} if even the system
   * ClassLoader isn't accessible)
   * @see Thread#getContextClassLoader()
   * @see ClassLoader#getSystemClassLoader()
   * @since 4.0
   */
  @Nullable
  public static ClassLoader getDefaultClassLoader() {
    ClassLoader cl = null;
    try {
      cl = Thread.currentThread().getContextClassLoader();
    }
    catch (Throwable ex) {
      // Cannot access thread context ClassLoader - falling back...
    }
    if (cl == null) {
      // No thread context class loader -> use class loader of this class.
      cl = ClassUtils.class.getClassLoader();
      if (cl == null) {
        // getClassLoader() returning null indicates the bootstrap ClassLoader
        try {
          cl = ClassLoader.getSystemClassLoader();
        }
        catch (Throwable ex) {
          // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
        }
      }
    }
    return cl;
  }

  /**
   * Override the thread context ClassLoader with the environment's bean ClassLoader
   * if necessary, i.e. if the bean ClassLoader is not equivalent to the thread
   * context ClassLoader already.
   *
   * @param classLoaderToUse the actual ClassLoader to use for the thread context
   * @return the original thread context ClassLoader, or {@code null} if not overridden
   * @since 4.0
   */
  @Nullable
  public static ClassLoader overrideThreadContextClassLoader(@Nullable ClassLoader classLoaderToUse) {
    Thread currentThread = Thread.currentThread();
    ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
    if (classLoaderToUse != null && !classLoaderToUse.equals(threadContextClassLoader)) {
      currentThread.setContextClassLoader(classLoaderToUse);
      return threadContextClassLoader;
    }
    else {
      return null;
    }
  }

  /**
   * Determine whether the {@link Class} identified by the supplied name is present
   * and can be loaded. Will return {@code false} if either the class or
   * one of its dependencies is not present or cannot be loaded.
   * <p> use default class loader
   *
   * @param className the name of the class to check
   * @return whether the specified class is present (including all of its
   * superclasses and interfaces)
   * @throws IllegalStateException if the corresponding class is resolvable but
   * there was a readability mismatch in the inheritance hierarchy of the class
   * (typically a missing dependency declaration in a Jigsaw module definition
   * for a superclass or interface implemented by the class to be checked here)
   */
  public static boolean isPresent(String className) {
    return isPresent(className, null);
  }

  /**
   * Determine whether the {@link Class} identified by the supplied name is present
   * and can be loaded. Will return {@code false} if either the class or
   * one of its dependencies is not present or cannot be loaded.
   *
   * @param className the name of the class to check
   * @param classLoader the class loader to use
   * (may be {@code null} which indicates the default class loader)
   * @return whether the specified class is present (including all of its
   * superclasses and interfaces)
   * @throws IllegalStateException if the corresponding class is resolvable but
   * there was a readability mismatch in the inheritance hierarchy of the class
   * (typically a missing dependency declaration in a Jigsaw module definition
   * for a superclass or interface implemented by the class to be checked here)
   */
  public static boolean isPresent(String className, @Nullable ClassLoader classLoader) {
    try {
      forName(className, classLoader);
      return true;
    }
    catch (IllegalAccessError err) {
      throw new IllegalStateException(
              "Readability mismatch in inheritance hierarchy of class [" + className + "]: " + err.getMessage(), err);
    }
    catch (Throwable ex) {
      // Typically, ClassNotFoundException or NoClassDefFoundError...
      return false;
    }
  }

  public static Class<?> resolvePrimitiveClassName(String name) {
    // Most class names will be quite long, considering that they
    // SHOULD sit in a package, so a length check is worthwhile.
    if (name != null && name.length() <= 8) {
      // Could be a primitive - likely.
      return primitiveTypeNameMap.get(name);
    }
    return null;
  }

  /**
   * Replacement for {@code Class.forName()} that also returns Class instances
   * for primitives (e.g. "int") and array class names (e.g. "String[]").
   * Furthermore, it is also capable of resolving nested class names in Java source
   * style (e.g. "java.lang.Thread.State" instead of "java.lang.Thread$State").
   *
   * @param name the name of the Class
   * @param classLoader the class loader to use (may be {@code null},
   * which indicates the default class loader)
   * @return a class instance for the supplied name
   * @throws ClassNotFoundException if the class was not found
   * @throws LinkageError if the class file could not be loaded
   * @see Class#forName(String, boolean, ClassLoader)
   * @since 2.1.7
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> forName(
          String name, @Nullable ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
    Assert.notNull(name, "Name must not be null");
    return (Class<T>) doForName(name, classLoader);
  }

  private static Class<?> doForName(
          String name, @Nullable ClassLoader classLoader) throws ClassNotFoundException, LinkageError {
    Class<?> clazz = resolvePrimitiveClassName(name);
    if (clazz == null) {
      clazz = commonClassCache.get(name);
    }

    if (clazz != null) {
      return clazz;
    }

    // "java.lang.String[]" style arrays
    if (name.endsWith(ARRAY_SUFFIX)) {
      Class<?> elementClass = doForName(
              name.substring(0, name.length() - ARRAY_SUFFIX.length()), classLoader);
      return Array.newInstance(elementClass, 0).getClass();
    }

    // "[Ljava.lang.String;" style arrays
    if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
      Class<?> elementClass = doForName(
              name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1), classLoader);
      return Array.newInstance(elementClass, 0).getClass();
    }

    // "[[I" or "[[Ljava.lang.String;" style arrays
    if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
      Class<?> elementClass = doForName(
              name.substring(INTERNAL_ARRAY_PREFIX.length()), classLoader);
      return Array.newInstance(elementClass, 0).getClass();
    }

    if (classLoader == null) {
      classLoader = getDefaultClassLoader();
    }
    try {
      return Class.forName(name, false, classLoader);
    }
    catch (ClassNotFoundException ex) {
      int lastDotIndex = name.lastIndexOf(Constant.PACKAGE_SEPARATOR);
      if (lastDotIndex != -1) {
        String innerClassName = name.substring(0, lastDotIndex) + INNER_CLASS_SEPARATOR + name.substring(lastDotIndex + 1);
        try {
          return Class.forName(innerClassName, false, classLoader);
        }
        catch (ClassNotFoundException ex2) {
          // Swallow - let original exception get through
        }
      }
      throw ex;
    }
  }

  /**
   * Replacement for {@code Class.forName()} that also returns Class instances
   * for primitives (e.g. "int") and array class names (e.g. "String[]").
   * Furthermore, it is also capable of resolving nested class names in Java source
   * style (e.g. "java.lang.Thread.State" instead of "java.lang.Thread$State").
   * <p>
   * use default class loader, from spring
   *
   * @param name the name of the Class
   * @return a class instance for the supplied name
   * @throws ClassNotFoundException when class could not be found
   * @since 2.1.6
   */
  public static <T> Class<T> forName(String name) throws ClassNotFoundException {
    return forName(name, getDefaultClassLoader());
  }

  /**
   * Resolve the given class name into a Class instance. Supports
   * primitives (like "int") and array class names (like "String[]").
   * <p>This is effectively equivalent to the {@code forName}
   * method with the same arguments, with the only difference being
   * the exceptions thrown in case of class loading failure.
   *
   * @param className the name of the Class
   * @param classLoader the class loader to use
   * (may be {@code null}, which indicates the default class loader)
   * @return a class instance for the supplied name
   * @throws IllegalArgumentException if the class name was not resolvable
   * (that is, the class could not be found or the class file could not be loaded)
   * @throws IllegalStateException if the corresponding class is resolvable but
   * there was a readability mismatch in the inheritance hierarchy of the class
   * (typically a missing dependency declaration in a Jigsaw module definition
   * for a superclass or interface implemented by the class to be loaded here)
   * @see #forName(String, ClassLoader)
   * @since 4.0
   */
  public static <T> Class<T> resolveClassName(
          String className, @Nullable ClassLoader classLoader) throws IllegalArgumentException {
    try {
      return forName(className, classLoader);
    }
    catch (IllegalAccessError err) {
      throw new IllegalStateException(
              "Readability mismatch in inheritance hierarchy of class ["
                      + className + "]: " + err.getMessage(), err);
    }
    catch (LinkageError err) {
      throw new IllegalArgumentException(
              "Unresolvable class definition for class [" + className + "]", err);
    }
    catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
    }
  }

  /**
   * Load class
   *
   * @param <T> return class type
   * @param name class full name
   * @return class if not found will returns null
   */
  @Nullable
  public static <T> Class<T> load(String name) {
    return load(name, getDefaultClassLoader());
  }

  /**
   * Load class with given class name and {@link ClassLoader}
   *
   * @param <T> return class type
   * @param name class full name
   * @param classLoader use this {@link ClassLoader} load the class
   * @return null if cannot load
   */
  @Nullable
  public static <T> Class<T> load(String name, ClassLoader classLoader) {
    try {
      return forName(name, classLoader);
    }
    catch (ClassNotFoundException | LinkageError e) {
      return null;
    }
  }

  public static String getClassName(ClassReader r) {
    return r.getClassName().replace(Constant.PATH_SEPARATOR, Constant.PACKAGE_SEPARATOR);
  }

  public static String getClassName(byte[] classFile) {
    return getClassName(new ClassReader(classFile));
  }

  public static String getClassName(Resource resource) throws IOException {
    try (InputStream inputStream = resource.getInputStream()) {
      return getClassName(inputStream);
    }
  }

  public static String getClassName(InputStream inputStream) throws IOException {
    return getClassName(new ClassReader(inputStream));
  }

  /**
   * Determine whether the given interface is a common Java language interface:
   * {@link Serializable}, {@link Externalizable}, {@link Closeable}, {@link AutoCloseable},
   * {@link Cloneable}, {@link Comparable} - all of which can be ignored when looking
   * for 'primary' user-level interfaces. Common characteristics: no service-level
   * operations, no bean property methods, no default methods.
   *
   * @param ifc the interface to check
   * @since 4.0
   */
  public static boolean isJavaLanguageInterface(Class<?> ifc) {
    return javaLanguageInterfaces.contains(ifc);
  }

  /**
   * Determine if the supplied class is an <em>inner class</em>,
   * i.e. a non-static member of an enclosing class.
   *
   * @return {@code true} if the supplied class is an inner class
   * @see Class#isMemberClass()
   * @since 4.0
   */
  public static boolean isInnerClass(Class<?> clazz) {
    return (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers()));
  }

  /**
   * Determine if the supplied {@link Class} is a JVM-generated implementation
   * class for a lambda expression or method reference.
   * <p>This method makes a best-effort attempt at determining this, based on
   * checks that work on modern, main stream JVMs.
   *
   * @param clazz the class to check
   * @return {@code true} if the class is a lambda implementation class
   * @since 4.0
   */
  public static boolean isLambdaClass(Class<?> clazz) {
    return clazz.isSynthetic() && (clazz.getSuperclass() == Object.class)
            && (clazz.getInterfaces().length > 0) && clazz.getName().contains("$$Lambda");
  }

  /**
   * If the class is dynamically generated then the user class will be extracted
   * in a specific format.
   *
   * @param synthetic Input object
   * @return The user class
   * @since 2.1.7
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getUserClass(T synthetic) {
    Assert.notNull(synthetic, "synthetic must not be null");
    return (Class<T>) getUserClass(synthetic.getClass());
  }

  /**
   * If the class is dynamically generated then the user class will be extracted
   * in a specific format.
   *
   * @param syntheticClass input test class
   * @return The user class
   * @since 2.1.7
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getUserClass(Class<T> syntheticClass) {
    Assert.notNull(syntheticClass, "syntheticClass must not be null");
    if (syntheticClass.getName().lastIndexOf(CGLIB_CLASS_SEPARATOR) > -1) {
      Class<?> superclass = syntheticClass.getSuperclass();
      if (superclass != null && superclass != Object.class) {
        return (Class<T>) superclass;
      }
    }
    return syntheticClass;
  }

  /**
   * If the class is dynamically generated then the user class will be extracted
   * in a specific format.
   *
   * @param name Class name
   * @return The user class
   * @since 2.1.7
   */
  public static <T> Class<T> getUserClass(String name) {
    Assert.notNull(name, "synthetic-name must not be null");
    int i = name.indexOf(CGLIB_CLASS_SEPARATOR);
    return i > 0 ? load(name.substring(0, i)) : load(name);
  }

  /**
   * Return a descriptive name for the given object's type: usually simply
   * the class name, but component type class name + "[]" for arrays,
   * and an appended list of implemented interfaces for JDK proxies.
   *
   * @param value the value to introspect
   * @return the qualified name of the class
   */
  @Nullable
  public static String getDescriptiveType(@Nullable Object value) {
    if (value == null) {
      return null;
    }
    Class<?> clazz = value.getClass();
    if (Proxy.isProxyClass(clazz)) {
      String prefix = clazz.getName() + " implementing ";
      StringJoiner result = new StringJoiner(",", prefix, Constant.BLANK);
      for (Class<?> ifc : clazz.getInterfaces()) {
        result.add(ifc.getName());
      }
      return result.toString();
    }
    else {
      return clazz.getTypeName();
    }
  }

  // --------------------------------- Field

  // Generics

  /**
   * @param type source type
   * @since 2.1.7
   */
  public static java.lang.reflect.Type[] getGenerics(Class<?> type) {
    if (type != null) {
      java.lang.reflect.Type genericSuperclass = type.getGenericSuperclass();

      Class<?> superclass = type.getSuperclass();
      if (genericSuperclass == superclass && genericSuperclass != Object.class) {
        return getGenerics(superclass);
      }
      if (genericSuperclass instanceof ParameterizedType) {
        return getActualTypeArguments(genericSuperclass);
      }
      java.lang.reflect.Type[] genericInterfaces = type.getGenericInterfaces();
      if (ObjectUtils.isNotEmpty(genericInterfaces)) {
        return getActualTypeArguments(genericInterfaces[0]);
      }
    }
    return null;
  }

  @Nullable
  public static java.lang.reflect.Type[] getGenericTypes(Field property) {
    return property != null ? getActualTypeArguments(property.getGenericType()) : null;
  }

  @Nullable
  public static java.lang.reflect.Type[] getGenericTypes(Parameter parameter) {
    return parameter != null ? getActualTypeArguments(parameter.getParameterizedType()) : null;
  }

  @Nullable
  static java.lang.reflect.Type[] getActualTypeArguments(java.lang.reflect.Type pType) {
    if (pType instanceof ParameterizedType) {
      return ((ParameterizedType) pType).getActualTypeArguments();
    }
    return null;
  }

  /**
   * Find generics in target class
   *
   * @param type find generics in target class
   * @param superClass A interface class or super class
   * @return Target generics {@link Class}s
   * @since 3.0
   */
  @Nullable
  public static Class<?>[] getGenerics(Class<?> type, Class<?> superClass) {
    return GenericTypeResolver.resolveTypeArguments(type, superClass);
  }

  //
  // ---------------------------------

  /**
   * Return the qualified name of the given method, consisting of fully qualified
   * interface/class name + "." + method name.
   *
   * @param method the method
   * @return the qualified name of the method
   */
  public static String getQualifiedMethodName(Method method) {
    return getQualifiedMethodName(method, null);
  }

  /**
   * Return the qualified name of the given method, consisting of fully qualified
   * interface/class name + "." + method name.
   *
   * @param method the method
   * @param clazz the clazz that the method is being invoked on (may be {@code null}
   * to indicate the method's declaring class)
   * @return the qualified name of the method
   */
  public static String getQualifiedMethodName(Method method, @Nullable Class<?> clazz) {
    Assert.notNull(method, "Method must not be null");
    return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
  }

  //

  /**
   * Tells us if the class passed in is a known common type
   *
   * @param clazz The class to check
   * @return True if the class is known
   */
  public static boolean isSimpleType(Class<?> clazz) {
    return primitiveTypes.contains(clazz)
            || (clazz.isArray() && isSimpleType(clazz.getComponentType()));
  }

  // Interfaces from spring

  /**
   * Return all interfaces that the given instance implements as an array,
   * including ones implemented by superclasses.
   *
   * @param instance the instance to analyze for interfaces
   * @return all interfaces that the given instance implements as an array
   * @since 3.0
   */
  public static Class<?>[] getAllInterfaces(Object instance) {
    Assert.notNull(instance, "Instance must not be null");
    return getAllInterfacesForClass(instance.getClass());
  }

  /**
   * Return all interfaces that the given class implements as an array,
   * including ones implemented by superclasses.
   * <p>If the class itself is an interface, it gets returned as sole interface.
   *
   * @param clazz the class to analyze for interfaces
   * @return all interfaces that the given object implements as an array
   * @since 3.0
   */
  public static Class<?>[] getAllInterfacesForClass(Class<?> clazz) {
    return getAllInterfacesForClass(clazz, null);
  }

  /**
   * Return all interfaces that the given class implements as an array,
   * including ones implemented by superclasses.
   * <p>If the class itself is an interface, it gets returned as sole interface.
   *
   * @param clazz the class to analyze for interfaces
   * @param classLoader the ClassLoader that the interfaces need to be visible in
   * (may be {@code null} when accepting all declared interfaces)
   * @return all interfaces that the given object implements as an array
   * @since 3.0
   */
  public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, ClassLoader classLoader) {
    return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
  }

  /**
   * Copy the given {@code Collection} into a {@code Class} array.
   * <p>The {@code Collection} must contain {@code Class} elements only.
   *
   * @param collection the {@code Collection} to copy
   * @return the {@code Class} array
   * @see StringUtils#toStringArray(Collection)
   * @since 3.0
   */
  public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
    return CollectionUtils.isEmpty(collection)
           ? Constant.EMPTY_CLASSES
           : collection.toArray(Constant.EMPTY_CLASSES);
  }

  /**
   * Return all interfaces that the given instance implements as a Set,
   * including ones implemented by superclasses.
   *
   * @param instance the instance to analyze for interfaces
   * @return all interfaces that the given instance implements as a Set
   * @since 3.0
   */
  public static Set<Class<?>> getAllInterfacesAsSet(Object instance) {
    Assert.notNull(instance, "Instance must not be null");
    return getAllInterfacesForClassAsSet(instance.getClass());
  }

  /**
   * Return all interfaces that the given class implements as a Set,
   * including ones implemented by superclasses.
   * <p>If the class itself is an interface, it gets returned as sole interface.
   *
   * @param clazz the class to analyze for interfaces
   * @return all interfaces that the given object implements as a Set
   * @since 3.0
   */
  public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz) {
    return getAllInterfacesForClassAsSet(clazz, null);
  }

  /**
   * Return all interfaces that the given class implements as a Set,
   * including ones implemented by superclasses.
   * <p>If the class itself is an interface, it gets returned as sole interface.
   *
   * @param clazz the class to analyze for interfaces
   * @param classLoader the ClassLoader that the interfaces need to be visible in
   * (may be {@code null} when accepting all declared interfaces)
   * @return all interfaces that the given object implements as a Set
   * @since 3.0
   */
  public static Set<Class<?>> getAllInterfacesForClassAsSet(Class<?> clazz, ClassLoader classLoader) {
    Assert.notNull(clazz, "Class must not be null");
    if (clazz.isInterface() && isVisible(clazz, classLoader)) {
      return Collections.singleton(clazz);
    }
    LinkedHashSet<Class<?>> interfaces = new LinkedHashSet<>();
    Class<?> current = clazz;
    while (current != null) {
      Class<?>[] ifcs = current.getInterfaces();
      for (Class<?> ifc : ifcs) {
        if (isVisible(ifc, classLoader)) {
          interfaces.add(ifc);
        }
      }
      current = current.getSuperclass();
    }
    return interfaces;
  }

  /**
   * Create a composite interface Class for the given interfaces,
   * implementing the given interfaces in one single Class.
   * <p>This implementation builds a JDK proxy class for the given interfaces.
   *
   * @param interfaces the interfaces to merge
   * @param classLoader the ClassLoader to create the composite Class in
   * @return the merged interface as Class
   * @throws IllegalArgumentException if the specified interfaces expose
   * conflicting method signatures (or a similar constraint is violated)
   * @see java.lang.reflect.Proxy#getProxyClass
   * @since 4.0
   */
  @SuppressWarnings("deprecation")
  public static Class<?> createCompositeInterface(Class<?>[] interfaces, @Nullable ClassLoader classLoader) {
    Assert.notEmpty(interfaces, "Interface array must not be empty");
    return Proxy.getProxyClass(classLoader, interfaces);
  }

  /**
   * Determine the common ancestor of the given classes, if any.
   *
   * @param clazz1 the class to introspect
   * @param clazz2 the other class to introspect
   * @return the common ancestor (i.e. common superclass, one interface
   * extending the other), or {@code null} if none found. If any of the
   * given classes is {@code null}, the other class will be returned.
   * @since 4.0
   */
  @Nullable
  public static Class<?> determineCommonAncestor(@Nullable Class<?> clazz1, @Nullable Class<?> clazz2) {
    if (clazz1 == null) {
      return clazz2;
    }
    if (clazz2 == null) {
      return clazz1;
    }
    if (clazz1.isAssignableFrom(clazz2)) {
      return clazz1;
    }
    if (clazz2.isAssignableFrom(clazz1)) {
      return clazz2;
    }
    Class<?> ancestor = clazz1;
    do {
      ancestor = ancestor.getSuperclass();
      if (ancestor == null || Object.class == ancestor) {
        return null;
      }
    }
    while (!ancestor.isAssignableFrom(clazz2));
    return ancestor;
  }

  /**
   * Check whether the given class is cache-safe in the given context,
   * i.e. whether it is loaded by the given ClassLoader or a parent of it.
   *
   * @param clazz the class to analyze
   * @param classLoader the ClassLoader to potentially cache metadata in
   * (may be {@code null} which indicates the system class loader)
   * @since 4.0
   */
  public static boolean isCacheSafe(Class<?> clazz, @Nullable ClassLoader classLoader) {
    Assert.notNull(clazz, "Class must not be null");
    try {
      ClassLoader target = clazz.getClassLoader();
      // Common cases
      if (target == classLoader || target == null) {
        return true;
      }
      if (classLoader == null) {
        return false;
      }
      // Check for match in ancestors -> positive
      ClassLoader current = classLoader;
      while (current != null) {
        current = current.getParent();
        if (current == target) {
          return true;
        }
      }
      // Check for match in children -> negative
      while (target != null) {
        target = target.getParent();
        if (target == classLoader) {
          return false;
        }
      }
    }
    catch (SecurityException ex) {
      // Fall through to loadable check below
    }

    // Fallback for ClassLoaders without parent/child relationship:
    // safe if same Class can be loaded from given ClassLoader
    return classLoader != null && isLoadable(clazz, classLoader);
  }

  /**
   * Check whether the given class is visible in the given ClassLoader.
   *
   * @param clazz the class to check (typically an interface)
   * @param classLoader the ClassLoader to check against
   * (may be {@code null} in which case this method will always return {@code true})
   * @since 3.0
   */
  public static boolean isVisible(Class<?> clazz, @Nullable ClassLoader classLoader) {
    if (classLoader == null) {
      return true;
    }
    try {
      if (clazz.getClassLoader() == classLoader) {
        return true;
      }
    }
    catch (SecurityException ex) {
      // Fall through to loadable check below
    }

    // Visible if same Class can be loaded from given ClassLoader
    return isLoadable(clazz, classLoader);
  }

  /**
   * Check whether the given class is loadable in the given ClassLoader.
   *
   * @param clazz the class to check (typically an interface)
   * @param classLoader the ClassLoader to check against
   * @since 3.0
   */
  private static boolean isLoadable(Class<?> clazz, ClassLoader classLoader) {
    try {
      return (clazz == classLoader.loadClass(clazz.getName()));
      // Else: different class with same name found
    }
    catch (ClassNotFoundException ex) {
      // No corresponding class found at all
      return false;
    }
  }

  /**
   * Build a String that consists of the names of the classes/interfaces
   * in the given array.
   * <p>Basically like {@code AbstractCollection.toString()}, but stripping
   * the "class "/"interface " prefix before every class name.
   *
   * @param classes an array of Class objects
   * @return a String of form "[com.foo.Bar, com.foo.Baz]"
   * @see java.util.AbstractCollection#toString()
   * @since 3.0
   */
  public static String classNamesToString(Class<?>... classes) {
    return classNamesToString(Arrays.asList(classes));
  }

  /**
   * Build a String that consists of the names of the classes/interfaces
   * in the given collection.
   * <p>Basically like {@code AbstractCollection.toString()}, but stripping
   * the "class "/"interface " prefix before every class name.
   *
   * @param classes a Collection of Class objects (may be {@code null})
   * @return a String of form "[com.foo.Bar, com.foo.Baz]"
   * @see java.util.AbstractCollection#toString()
   * @since 3.0
   */
  public static String classNamesToString(Collection<Class<?>> classes) {
    if (CollectionUtils.isEmpty(classes)) {
      return "[]";
    }
    StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");
    for (Class<?> clazz : classes) {
      stringJoiner.add(clazz.getName());
    }
    return stringJoiner.toString();
  }

  //

  /**
   * Determine the name of the package of the given class,
   * e.g. "java.lang" for the {@code java.lang.String} class.
   *
   * @param clazz the class
   * @return the package name, or the empty String if the class
   * is defined in the default package
   * @since 3.0
   */
  public static String getPackageName(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    return getPackageName(clazz.getName());
  }

  /**
   * Determine the name of the package of the given fully-qualified class name,
   * e.g. "java.lang" for the {@code java.lang.String} class name.
   *
   * @param fqClassName the fully-qualified class name
   * @return the package name, or the empty String if the class
   * is defined in the default package
   * @since 3.0
   */
  public static String getPackageName(String fqClassName) {
    Assert.notNull(fqClassName, "Class name must not be null");
    int lastDotIndex = fqClassName.lastIndexOf(Constant.PACKAGE_SEPARATOR);
    return lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : Constant.BLANK;
  }

  /**
   * Adapt the given arguments to the target signature in the given method,
   * if necessary: in particular, if a given vararg argument array does not
   * match the array type of the declared vararg parameter in the method.
   *
   * @param method the target method
   * @param arguments the given arguments
   * @return a cloned argument array, or the original if no adaptation is needed
   */
  public static Object[] adaptArgumentsIfNecessary(Method method, @Nullable Object[] arguments) {
    if (ObjectUtils.isEmpty(arguments)) {
      return Constant.EMPTY_OBJECTS;
    }
    if (method.isVarArgs() && method.getParameterCount() == arguments.length) {

      Class<?>[] paramTypes = method.getParameterTypes();
      int varargIndex = paramTypes.length - 1;
      Class<?> varargType = paramTypes[varargIndex];
      if (varargType.isArray()) {
        Object varargArray = arguments[varargIndex];
        if (varargArray instanceof Object[] && !varargType.isInstance(varargArray)) {
          Object[] newArguments = new Object[arguments.length];
          System.arraycopy(arguments, 0, newArguments, 0, varargIndex);
          Class<?> targetElementType = varargType.getComponentType();
          int varargLength = Array.getLength(varargArray);
          Object newVarargArray = Array.newInstance(targetElementType, varargLength);
          System.arraycopy(varargArray, 0, newVarargArray, 0, varargLength);
          newArguments[varargIndex] = newVarargArray;
          return newArguments;
        }
      }
    }
    return arguments;
  }

  /**
   * Return the qualified name of the given class: usually simply
   * the class name, but component type class name + "[]" for arrays.
   *
   * @param clazz the class
   * @return the qualified name of the class
   * @since 3.0
   */
  public static String getQualifiedName(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    return clazz.getTypeName();
  }

  /**
   * Get the class name without the qualified package name.
   *
   * @param className the className to get the short name for
   * @return the class name of the class without the package name
   * @throws IllegalArgumentException if the className is empty
   * @since 3.0
   */
  public static String getShortName(String className) {
    Assert.hasLength(className, "Class name must not be empty");
    int lastDotIndex = className.lastIndexOf(Constant.PACKAGE_SEPARATOR);
    int nameEndIndex = className.indexOf(CGLIB_CLASS_SEPARATOR);
    if (nameEndIndex == -1) {
      nameEndIndex = className.length();
    }
    String shortName = className.substring(lastDotIndex + 1, nameEndIndex);
    shortName = shortName.replace(INNER_CLASS_SEPARATOR, Constant.PACKAGE_SEPARATOR);
    return shortName;
  }

  public static String getSimpleName(String className) {
    String simpleName = getShortName(className);
    int innerIndex = simpleName.indexOf(Constant.PACKAGE_SEPARATOR);
    if (innerIndex > -1) {
      simpleName = simpleName.substring(innerIndex + 1);
    }
    return simpleName;
  }

  /**
   * Check whether the given class matches the user-specified type name.
   *
   * @param clazz the class to check
   * @param typeName the type name to match
   * @since 4.0
   */
  public static boolean matchesTypeName(Class<?> clazz, @Nullable String typeName) {
    return (typeName != null
            && (typeName.equals(clazz.getTypeName()) || typeName.equals(clazz.getSimpleName())));
  }

  /**
   * Get the class name without the qualified package name.
   *
   * @param clazz the class to get the short name for
   * @return the class name of the class without the package name
   * @since 3.0
   */
  public static String getShortName(Class<?> clazz) {
    return getShortName(getQualifiedName(clazz));
  }

  /**
   * Return the short string name of a Java class in uncapitalized JavaBeans
   * property format. Strips the outer class name in case of a nested class.
   *
   * @param clazz the class
   * @return the short name rendered in a standard JavaBeans property format
   * @see java.beans.Introspector#decapitalize(String)
   * @since 4.0
   */
  public static String getShortNameAsProperty(Class<?> clazz) {
    String shortName = getShortName(clazz);
    int dotIndex = shortName.lastIndexOf(Constant.PACKAGE_SEPARATOR);
    shortName = (dotIndex != -1 ? shortName.substring(dotIndex + 1) : shortName);
    return Introspector.decapitalize(shortName);
  }

  /**
   * Determine the name of the class file, relative to the containing
   * package: e.g. "String.class"
   *
   * @param clazz the class
   * @return the file name of the ".class" file
   * @since 4.0
   */
  public static String getClassFileName(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    String className = clazz.getName();
    int lastDotIndex = className.lastIndexOf(Constant.PACKAGE_SEPARATOR);
    return className.substring(lastDotIndex + 1) + CLASS_FILE_SUFFIX;
  }

  /**
   * Determine the name of the class file, relative to the containing
   * package: e.g. "java/lang/String.class"
   *
   * @param clazz the class
   * @return the file name of the ".class" file
   * @since 4.0
   */
  public static String getFullyClassFileName(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    String className = clazz.getName();
    return className.replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR) + CLASS_FILE_SUFFIX;
  }

  /**
   * Given an input class object, return a string which consists of the
   * class's package name as a pathname, i.e., all dots ('.') are replaced by
   * slashes ('/'). Neither a leading nor trailing slash is added. The result
   * could be concatenated with a slash and the name of a resource and fed
   * directly to {@code ClassLoader.getResource()}. For it to be fed to
   * {@code Class.getResource} instead, a leading slash would also have
   * to be prepended to the returned value.
   *
   * @param clazz the input class. A {@code null} value or the default
   * (empty) package will result in an empty string ("") being returned.
   * @return a path which represents the package name
   * @see ClassLoader#getResource
   * @see Class#getResource
   * @since 3.0
   */
  public static String classPackageAsResourcePath(@Nullable Class<?> clazz) {
    if (clazz == null) {
      return Constant.BLANK;
    }
    String className = clazz.getName();
    int packageEndIndex = className.lastIndexOf(Constant.PACKAGE_SEPARATOR);
    if (packageEndIndex == -1) {
      return Constant.BLANK;
    }
    String packageName = className.substring(0, packageEndIndex);
    return packageName.replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR);
  }

  /**
   * Convert a "/"-based resource path to a "."-based fully qualified class name.
   *
   * @param resourcePath the resource path pointing to a class
   * @return the corresponding fully qualified class name
   * @since 4.0
   */
  public static String convertResourcePathToClassName(String resourcePath) {
    Assert.notNull(resourcePath, "Resource path must not be null");
    return resourcePath.replace(Constant.PATH_SEPARATOR, Constant.PACKAGE_SEPARATOR);
  }

  /**
   * Convert a "."-based fully qualified class name to a "/"-based resource path.
   *
   * @param className the fully qualified class name
   * @return the corresponding resource path, pointing to the class
   * @since 4.0
   */
  public static String convertClassNameToResourcePath(String className) {
    Assert.notNull(className, "Class name must not be null");
    return className.replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR);
  }

  /**
   * Return a path suitable for use with {@code ClassLoader.getResource}
   * (also suitable for use with {@code Class.getResource} by prepending a
   * slash ('/') to the return value). Built by taking the package of the specified
   * class file, converting all dots ('.') to slashes ('/'), adding a trailing slash
   * if necessary, and concatenating the specified resource name to this.
   * <br/>As such, this function may be used to build a path suitable for
   * loading a resource file that is in the same package as a class file,
   * although {@link cn.taketoday.core.io.ClassPathResource} is usually
   * even more convenient.
   *
   * @param clazz the Class whose package will be used as the base
   * @param resourceName the resource name to append. A leading slash is optional.
   * @return the built-up resource path
   * @see ClassLoader#getResource
   * @see Class#getResource
   * @since 4.0
   */
  public static String addResourcePathToPackagePath(Class<?> clazz, String resourceName) {
    Assert.notNull(resourceName, "Resource name must not be null");
    if (!resourceName.startsWith("/")) {
      return classPackageAsResourcePath(clazz) + '/' + resourceName;
    }
    return classPackageAsResourcePath(clazz) + resourceName;
  }

  /**
   * @throws IllegalArgumentException target is not a enum
   * @since 3.0
   */
  public static Class<?> getEnumType(Class<?> targetType) {
    Class<?> enumType = targetType;
    while (enumType != null && !enumType.isEnum()) {
      enumType = enumType.getSuperclass();
    }
    if (enumType == null) {
      throw new IllegalArgumentException(
              "The target " + targetType + " does not refer to an enum");
    }
    return enumType;
  }

  /**
   * Check if the given class represents a primitive wrapper,
   * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, Double, or
   * Void.
   *
   * @param clazz the class to check
   * @return whether the given class is a primitive wrapper class
   * @since 4.0
   */
  public static boolean isPrimitiveWrapper(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    return primitiveWrapperTypeMap.containsKey(clazz);
  }

  /**
   * Check if the given class represents a primitive (i.e. boolean, byte,
   * char, short, int, long, float, or double), {@code void}, or a wrapper for
   * those types (i.e. Boolean, Byte, Character, Short, Integer, Long, Float,
   * Double, or Void).
   *
   * @param clazz the class to check
   * @return {@code true} if the given class represents a primitive, void, or
   * a wrapper class
   * @since 4.0
   */
  public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    return clazz.isPrimitive() || isPrimitiveWrapper(clazz);
  }

  /**
   * Check if the given class represents an array of primitives,
   * i.e. boolean, byte, char, short, int, long, float, or double.
   *
   * @param clazz the class to check
   * @return whether the given class is a primitive array class
   * @since 4.0
   */
  public static boolean isPrimitiveArray(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    return clazz.isArray() && clazz.getComponentType().isPrimitive();
  }

  /**
   * Check if the given class represents an array of primitive wrappers,
   * i.e. Boolean, Byte, Character, Short, Integer, Long, Float, or Double.
   *
   * @param clazz the class to check
   * @return whether the given class is a primitive wrapper array class
   * @since 4.0
   */
  public static boolean isPrimitiveWrapperArray(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    return clazz.isArray() && isPrimitiveWrapper(clazz.getComponentType());
  }

  /**
   * Resolve the given class if it is a primitive class,
   * returning the corresponding primitive wrapper type instead.
   *
   * @param clazz the class to check
   * @return the original class, or a primitive wrapper for the original primitive type
   * @since 4.0
   */
  public static Class<?> resolvePrimitiveIfNecessary(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    return clazz.isPrimitive() && clazz != void.class
           ? primitiveTypeToWrapperMap.get(clazz)
           : clazz;
  }

  /**
   * Check if the right-hand side type may be assigned to the left-hand side
   * type, assuming setting by reflection. Considers primitive wrapper
   * classes as assignable to the corresponding primitive types.
   *
   * @param lhsType the target type
   * @param rhsType the value type that should be assigned to the target type
   * @return if the target type is assignable from the value type
   * @see TypeUtils#isAssignable(java.lang.reflect.Type, java.lang.reflect.Type)
   * @since 4.0
   */
  public static boolean isAssignable(Class<?> lhsType, Class<?> rhsType) {
    Assert.notNull(lhsType, "Left-hand side type must not be null");
    Assert.notNull(rhsType, "Right-hand side type must not be null");
    if (lhsType.isAssignableFrom(rhsType)) {
      return true;
    }
    if (lhsType.isPrimitive()) {
      Class<?> resolvedPrimitive = primitiveWrapperTypeMap.get(rhsType);
      return lhsType == resolvedPrimitive;
    }
    else {
      Class<?> resolvedWrapper = primitiveTypeToWrapperMap.get(rhsType);
      return resolvedWrapper != null && lhsType.isAssignableFrom(resolvedWrapper);
    }
  }

  /**
   * Determine if the given type is assignable from the given value,
   * assuming setting by reflection. Considers primitive wrapper classes
   * as assignable to the corresponding primitive types.
   *
   * @param type the target type
   * @param value the value that should be assigned to the type
   * @return if the type is assignable from the value
   * @since 4.0
   */
  public static boolean isAssignableValue(Class<?> type, @Nullable Object value) {
    Assert.notNull(type, "Type must not be null");
    return value != null ? isAssignable(type, value.getClass()) : !type.isPrimitive();
  }

  /**
   * Determine whether the given method is declared by the user or at least pointing to
   * a user-declared method.
   * <p>Checks {@link Method#isSynthetic()} (for implementation methods) as well as the
   * {@code GroovyObject} interface (for interface methods; on an implementation class,
   * implementations of the {@code GroovyObject} methods will be marked as synthetic anyway).
   * Note that, despite being synthetic, bridge methods ({@link Method#isBridge()}) are considered
   * as user-level methods since they are eventually pointing to a user-declared generic method.
   *
   * @param method the method to check
   * @return {@code true} if the method can be considered as user-declared; {@code false} otherwise
   * @since 4.0
   */
  public static boolean isUserLevelMethod(Method method) {
    Assert.notNull(method, "Method must not be null");
    return method.isBridge() || (!method.isSynthetic() && !isGroovyObjectMethod(method));
  }

  private static boolean isGroovyObjectMethod(Method method) {
    return method.getDeclaringClass().getName().equals("groovy.lang.GroovyObject");
  }

}
