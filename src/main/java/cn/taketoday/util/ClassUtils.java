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

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;

import cn.taketoday.asm.ClassReader;
import cn.taketoday.asm.ClassVisitor;
import cn.taketoday.asm.Label;
import cn.taketoday.asm.MethodVisitor;
import cn.taketoday.asm.Opcodes;
import cn.taketoday.asm.Type;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.loader.CandidateComponentScanner;
import cn.taketoday.core.Assert;
import cn.taketoday.core.Constant;
import cn.taketoday.core.Nullable;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.io.Resource;

/**
 * @author TODAY 2018-06-0? ?
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

//    private static final Logger log = LoggerFactory.getLogger(ClassUtils.class);

  /** class loader **/
  private static ClassLoader classLoader;

  static final HashMap<String, Class<?>> PRIMITIVE_CACHE = new HashMap<>(32);

  static final ParameterFunction PARAMETER_NAMES_FUNCTION = new ParameterFunction();
  static final ConcurrentCache<Class<?>, Map<Method, String[]>> PARAMETER_NAMES_CACHE = ConcurrentCache.create(64);

  /** @since 3.0 */
  public static HashSet<Class<?>> primitiveTypes;

  static {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    if (classLoader == null) {
      classLoader = ClassUtils.class.getClassLoader();
    }
    if (classLoader == null) {
      classLoader = ClassLoader.getSystemClassLoader();
    }
    setClassLoader(classLoader);

    // Map primitive types
    // -------------------------------------------
    final HashSet<Class<?>> primitiveTypes = new HashSet<>();
    Collections.addAll(primitiveTypes,
                       boolean.class, byte.class, char.class, int.class,
                       long.class, double.class, float.class, short.class,
                       boolean[].class, byte[].class, char[].class, double[].class,
                       float[].class, int[].class, long[].class, short[].class
    );
    primitiveTypes.add(void.class);
    for (Class<?> primitiveType : primitiveTypes) {
      PRIMITIVE_CACHE.put(primitiveType.getName(), primitiveType);
    }

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
   * clear cache
   */
  public static void clearCache() {
    PARAMETER_NAMES_CACHE.clear();
  }

  public static void setClassLoader(ClassLoader classLoader) {
    ClassUtils.classLoader = classLoader;
  }

  /**
   * default class loader
   */
  public static ClassLoader getClassLoader() {
    return classLoader;
  }

  // ------------------------------------------ Class Scan

  /**
   * Whether given class name present in class path
   *
   * @param className
   *         class name
   *
   * @return whether given class name present in class path
   */
  public static boolean isPresent(String className) {
    Assert.notNull(className, "class name can't be null");
    try {
      forName(className);
      return true;
    }
    catch (ClassNotFoundException ex) {
      return false;
    }
  }

  public static Class<?> resolvePrimitiveClassName(String name) {
    // Most class names will be quite long, considering that they
    // SHOULD sit in a package, so a length check is worthwhile.
    if (name != null && name.length() <= 8) {
      // Could be a primitive - likely.
      return PRIMITIVE_CACHE.get(name);
    }
    return null;
  }

  /**
   * Load class .from spring
   *
   * @param name
   *         a class full name
   * @param classLoader
   *         The Class file loader
   *
   * @return a class
   *
   * @throws ClassNotFoundException
   *         When class could not be found
   * @since 2.1.7
   */
  public static Class<?> forName(String name, ClassLoader classLoader) throws ClassNotFoundException {
    Class<?> clazz = resolvePrimitiveClassName(name);
    if (clazz != null) {
      return clazz;
    }

    // "java.lang.String[]" style arrays
    if (name.endsWith(ARRAY_SUFFIX)) {
      Class<?> elementClass = //
              forName(name.substring(0, name.length() - ARRAY_SUFFIX.length()));
      return Array.newInstance(elementClass, 0).getClass();
    }

    // "[Ljava.lang.String;" style arrays
    if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
      Class<?> elementClass = //
              forName(name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1));
      return Array.newInstance(elementClass, 0).getClass();
    }

    // "[[I" or "[[Ljava.lang.String;" style arrays
    if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
      Class<?> elementClass = forName(name.substring(INTERNAL_ARRAY_PREFIX.length()));
      return Array.newInstance(elementClass, 0).getClass();
    }

    if (classLoader == null) {
      classLoader = getClassLoader();
    }
    try {
      return Class.forName(name, false, classLoader);
    }
    catch (ClassNotFoundException ex) {
      int lastDotIndex = name.lastIndexOf(Constant.PACKAGE_SEPARATOR);
      if (lastDotIndex != -1) {
        String innerClassName = name.substring(0, lastDotIndex) //
                + INNER_CLASS_SEPARATOR //
                + name.substring(lastDotIndex + 1);
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
   * Load class .from spring
   *
   * @param name
   *         a class full name
   *
   * @return a class
   *
   * @throws ClassNotFoundException
   *         when class could not be found
   * @since 2.1.6
   */
  public static Class<?> forName(String name) throws ClassNotFoundException {
    return forName(name, classLoader);
  }

  /**
   * Load class
   *
   * @param <T>
   *         return class type
   * @param name
   *         class full name
   *
   * @return class if not found will returns null
   */
  public static <T> Class<T> loadClass(String name) {
    return loadClass(name, classLoader);
  }

  /**
   * Load class with given class name and {@link ClassLoader}
   *
   * @param <T>
   *         return class type
   * @param name
   *         class gull name
   * @param classLoader
   *         use this {@link ClassLoader} load the class
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> loadClass(String name, ClassLoader classLoader) {
    Assert.notNull(classLoader, "ClassLoader can't be null");
    try {
      return (Class<T>) classLoader.loadClass(name);
    }
    catch (ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Scan class with given package.
   *
   * @param locations
   *         The packages to scan
   *
   * @return Class set
   */
  public static Set<Class<?>> scan(final String... locations) {
    return new CandidateComponentScanner().scan(locations);
  }

  /**
   * Scan all the classpath classes
   *
   * @param scanClasses
   *         class set
   *
   * @since 2.1.6
   */
  public static void scan(Set<Class<?>> scanClasses) {
    new CandidateComponentScanner()
            .setCandidates(scanClasses)
            .scan();
  }

  public static String getClassName(ClassReader r) {
    return r.getClassName().replace(Constant.PATH_SEPARATOR, Constant.PACKAGE_SEPARATOR);
  }

  public static String getClassName(final byte[] classFile) {
    return getClassName(new ClassReader(classFile));
  }

  public static String getClassName(final Resource resource) throws IOException {
    try (final InputStream inputStream = resource.getInputStream()) {
      return getClassName(inputStream);
    }
  }

  public static String getClassName(final InputStream inputStream) throws IOException {
    return getClassName(new ClassReader(inputStream));
  }

  /**
   * If the class is dynamically generated then the user class will be extracted
   * in a specific format.
   *
   * @param synthetic
   *         Input object
   *
   * @return The user class
   *
   * @since 2.1.7
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getUserClass(T synthetic) {
    return (Class<T>) getUserClass(Objects.requireNonNull(synthetic).getClass());
  }

  /**
   * If the class is dynamically generated then the user class will be extracted
   * in a specific format.
   *
   * @param syntheticClass
   *         input test class
   *
   * @return The user class
   *
   * @since 2.1.7
   */
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getUserClass(Class<T> syntheticClass) {
    if (Objects.requireNonNull(syntheticClass).getName().lastIndexOf(CGLIB_CLASS_SEPARATOR) > -1) {
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
   * @param name
   *         Class name
   *
   * @return The user class
   *
   * @since 2.1.7
   */
  public static <T> Class<T> getUserClass(String name) {
    final int i = Objects.requireNonNull(name).indexOf(CGLIB_CLASS_SEPARATOR);
    return i > 0 ? loadClass(name.substring(0, i)) : loadClass(name);
  }

  // --------------------------------- Field

  // Generics

  /**
   * @param type
   *         source type
   *
   * @since 2.1.7
   */
  public static java.lang.reflect.Type[] getGenerics(final Class<?> type) {
    if (type != null) {
      final java.lang.reflect.Type genericSuperclass = type.getGenericSuperclass();

      final Class<?> superclass = type.getSuperclass();
      if (genericSuperclass == superclass && genericSuperclass != Object.class) {
        return getGenerics(superclass);
      }
      if (genericSuperclass instanceof ParameterizedType) {
        return getActualTypeArguments(genericSuperclass);
      }
      final java.lang.reflect.Type[] genericInterfaces = type.getGenericInterfaces();
      if (ObjectUtils.isNotEmpty(genericInterfaces)) {
        return getActualTypeArguments(genericInterfaces[0]);
      }
    }
    return null;
  }

  @Nullable
  public static java.lang.reflect.Type[] getGenericTypes(final Field property) {
    return property != null ? getActualTypeArguments(property.getGenericType()) : null;
  }

  @Nullable
  public static java.lang.reflect.Type[] getGenericTypes(final Parameter parameter) {
    return parameter != null ? getActualTypeArguments(parameter.getParameterizedType()) : null;
  }

  @Nullable
  static java.lang.reflect.Type[] getActualTypeArguments(final java.lang.reflect.Type pType) {
    if (pType instanceof ParameterizedType) {
      return ((ParameterizedType) pType).getActualTypeArguments();
    }
    return null;
  }

  /**
   * Find generics in target class
   *
   * @param type
   *         find generics in target class
   * @param superClass
   *         A interface class or super class
   *
   * @return Target generics {@link Class}s
   *
   * @since 3.0
   */
  public static Class<?>[] getGenerics(final Class<?> type, Class<?> superClass) {
    return GenericTypeResolver.resolveTypeArguments(type, superClass);
  }

  // --------------------------- parameter names discovering

  /**
   * Find method parameter list. Uses ObjectWeb's ASM library for analyzing class
   * files.
   *
   * @param method
   *         target method
   *
   * @return method parameter list
   *
   * @throws ApplicationContextException
   *         when could not access to the class file
   * @since 2.1.6
   */
  public static String[] getMethodArgsNames(Method method) {
    return PARAMETER_NAMES_CACHE.get(method.getDeclaringClass(), PARAMETER_NAMES_FUNCTION).get(method);
  }

  private static boolean enableParamNameTypeChecking;

  public static void setEnableParamNameTypeChecking(final boolean enableParamNameTypeChecking) {
    ClassUtils.enableParamNameTypeChecking = enableParamNameTypeChecking;
  }

  static {
    enableParamNameTypeChecking = Boolean.parseBoolean(System.getProperty("ClassUtils.enableParamNameTypeChecking", "true"));
  }

  static final class ParameterFunction implements Function<Class<?>, Map<Method, String[]>> {

    @Override
    public Map<Method, String[]> apply(final Class<?> declaringClass) {
      final String classFile = declaringClass.getName()
              .replace(Constant.PACKAGE_SEPARATOR, Constant.PATH_SEPARATOR)
              .concat(CLASS_FILE_SUFFIX);

      try (InputStream resourceAsStream = classLoader.getResourceAsStream(classFile)) {
        final ClassNode classVisitor = new ClassNode();
        new ClassReader(resourceAsStream).accept(classVisitor, 0);

        final ArrayList<MethodNode> methodNodes = classVisitor.methodNodes;
        final HashMap<Method, String[]> map = new HashMap<>(methodNodes.size());
        for (final MethodNode methodNode : methodNodes) {
          final Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
          final Class<?>[] argTypes;
          if (argumentTypes.length == 0) {
            argTypes = Constant.EMPTY_CLASS_ARRAY; // fixed @since 3.0.1
          }
          else {
            argTypes = new Class<?>[argumentTypes.length];
            int idx = 0;
            for (final Type argumentType : argumentTypes) {
              argTypes[idx++] = forName(argumentType.getClassName());
            }
          }
          final Method method = ReflectionUtils.findMethod(declaringClass, methodNode.name, argTypes);
          if (method == null) {
            throw new NoSuchMethodException(
                    "No such method named: '" + methodNode.name + "' argTypes: '"
                            + Arrays.toString(argTypes) + "' in: " + declaringClass);
          }

          final int parameterCount = method.getParameterCount();
          if (parameterCount == 0) {
            map.put(method, Constant.EMPTY_STRING_ARRAY);
            continue;
          }
          final String[] paramNames = new String[parameterCount];
          if (Modifier.isAbstract(method.getModifiers()) || method.isBridge() || method.isSynthetic()) {
            int idx = 0;
            for (final Parameter parameter : method.getParameters()) {
              paramNames[idx++] = parameter.getName();
            }
            map.put(method, paramNames);
            continue;
          }

          final ArrayList<LocalVariable> localVariables = methodNode.localVariables;
          if (localVariables.size() >= parameterCount) {
            int offset = Modifier.isStatic(method.getModifiers()) ? 0 : 1;
            if (enableParamNameTypeChecking) { // enable check params types
              // check params types
              int idx = offset; // localVariable index
              int start = 0; // loop control
              while (start < parameterCount) {
                final Type argument = argumentTypes[start];
                if (!argument.equals(Type.fromDescriptor(localVariables.get(idx++).descriptor))) {
                  idx = ++offset;
                  start = 0; //reset
                }
                else {
                  paramNames[start] = localVariables.get(start + offset).name;
                  start++;
                }
              }
            }
            else {
              for (int idx = 0; idx < parameterCount; idx++) {
                paramNames[idx] = localVariables.get(idx + offset).name;
              }
            }
          }
          // @since 3.0.4 for gc
          localVariables.clear();
          methodNode.localVariables = null;

          map.put(method, paramNames);
        }
        // @since 3.0.4 for gc
        methodNodes.clear();
        classVisitor.methodNodes = null;
        return map;
      }
      catch (IOException | ClassNotFoundException | NoSuchMethodException | IndexOutOfBoundsException e) {
        throw new ApplicationContextException("When visit declaring class: [" + declaringClass.getName() + ']', e);
      }
    }

  }

  static final class ClassNode extends ClassVisitor {
    private ArrayList<MethodNode> methodNodes = new ArrayList<>();

    @Override
    public MethodVisitor visitMethod(int access,
                                     String name,
                                     String descriptor,
                                     String signature,
                                     String[] exceptions) {

      if (isSyntheticOrBridged(access)
              || Constant.CONSTRUCTOR_NAME.equals(name)
              || Constant.STATIC_CLASS_INIT.equals(name)) {
        return null;
      }
      final MethodNode methodNode = new MethodNode(name, descriptor);
      methodNodes.add(methodNode);
      return methodNode;
    }

    private static boolean isSyntheticOrBridged(int access) {
      return (((access & Opcodes.ACC_SYNTHETIC) | (access & Opcodes.ACC_BRIDGE)) > 0);
    }
  }

  static final class MethodNode extends MethodVisitor {
    private final String name;
    private final String desc;
    private ArrayList<LocalVariable> localVariables = new ArrayList<>();

    MethodNode(final String name, final String desc) {
      this.name = name;
      this.desc = desc;
    }

    @Override
    public void visitLocalVariable(String name,
                                   String descriptor,
                                   String signature,
                                   Label start, Label end, int index) {

      localVariables.add(new LocalVariable(name, descriptor));
    }
  }

  static class LocalVariable {
    String name;
    String descriptor;

    LocalVariable(String name, String descriptor) {
      this.name = name;
      this.descriptor = descriptor;
    }
  }

  //
  // ---------------------------------

  /**
   * Return the qualified name of the given method, consisting of fully qualified
   * interface/class name + "." + method name.
   *
   * @param method
   *         the method
   *
   * @return the qualified name of the method
   */
  public static String getQualifiedMethodName(Method method) {
    return getQualifiedMethodName(method, null);
  }

  /**
   * Return the qualified name of the given method, consisting of fully qualified
   * interface/class name + "." + method name.
   *
   * @param method
   *         the method
   * @param clazz
   *         the clazz that the method is being invoked on (may be {@code null}
   *         to indicate the method's declaring class)
   *
   * @return the qualified name of the method
   */
  public static String getQualifiedMethodName(Method method, Class<?> clazz) {
    Assert.notNull(method, "Method must not be null");
    return (clazz != null ? clazz : method.getDeclaringClass()).getName() + '.' + method.getName();
  }

  //

  /**
   * Tells us if the class passed in is a known common type
   *
   * @param clazz
   *         The class to check
   *
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
   * @param instance
   *         the instance to analyze for interfaces
   *
   * @return all interfaces that the given instance implements as an array
   *
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
   * @param clazz
   *         the class to analyze for interfaces
   *
   * @return all interfaces that the given object implements as an array
   *
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
   * @param clazz
   *         the class to analyze for interfaces
   * @param classLoader
   *         the ClassLoader that the interfaces need to be visible in
   *         (may be {@code null} when accepting all declared interfaces)
   *
   * @return all interfaces that the given object implements as an array
   *
   * @since 3.0
   */
  public static Class<?>[] getAllInterfacesForClass(Class<?> clazz, ClassLoader classLoader) {
    return toClassArray(getAllInterfacesForClassAsSet(clazz, classLoader));
  }

  /**
   * Copy the given {@code Collection} into a {@code Class} array.
   * <p>The {@code Collection} must contain {@code Class} elements only.
   *
   * @param collection
   *         the {@code Collection} to copy
   *
   * @return the {@code Class} array
   *
   * @see StringUtils#toStringArray
   * @since 3.0
   */
  public static Class<?>[] toClassArray(Collection<Class<?>> collection) {
    return CollectionUtils.isEmpty(collection)
           ? Constant.EMPTY_CLASS_ARRAY
           : collection.toArray(Constant.EMPTY_CLASS_ARRAY);
  }

  /**
   * Return all interfaces that the given instance implements as a Set,
   * including ones implemented by superclasses.
   *
   * @param instance
   *         the instance to analyze for interfaces
   *
   * @return all interfaces that the given instance implements as a Set
   *
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
   * @param clazz
   *         the class to analyze for interfaces
   *
   * @return all interfaces that the given object implements as a Set
   *
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
   * @param clazz
   *         the class to analyze for interfaces
   * @param classLoader
   *         the ClassLoader that the interfaces need to be visible in
   *         (may be {@code null} when accepting all declared interfaces)
   *
   * @return all interfaces that the given object implements as a Set
   *
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
   * Check whether the given class is visible in the given ClassLoader.
   *
   * @param clazz
   *         the class to check (typically an interface)
   * @param classLoader
   *         the ClassLoader to check against
   *         (may be {@code null} in which case this method will always return {@code true})
   *
   * @since 3.0
   */
  public static boolean isVisible(Class<?> clazz, ClassLoader classLoader) {
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
   * @param clazz
   *         the class to check (typically an interface)
   * @param classLoader
   *         the ClassLoader to check against
   *
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
   * @param classes
   *         an array of Class objects
   *
   * @return a String of form "[com.foo.Bar, com.foo.Baz]"
   *
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
   * @param classes
   *         a Collection of Class objects (may be {@code null})
   *
   * @return a String of form "[com.foo.Bar, com.foo.Baz]"
   *
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
   * Determine whether the given class has a public method with the given signature.
   *
   * @param clazz
   *         the clazz to analyze
   * @param method
   *         the method to look for
   *
   * @return whether the class has a corresponding method
   *
   * @since 3.0
   */
  public static boolean hasMethod(Class<?> clazz, Method method) {
    Assert.notNull(clazz, "Class must not be null");
    Assert.notNull(method, "Method must not be null");
    if (clazz == method.getDeclaringClass()) {
      return true;
    }
    String methodName = method.getName();
    Class<?>[] paramTypes = method.getParameterTypes();
    return getMethodOrNull(clazz, methodName, paramTypes) != null;
  }

  private static Method getMethodOrNull(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
    try {
      return clazz.getMethod(methodName, paramTypes);
    }
    catch (NoSuchMethodException ex) {
      return null;
    }
  }

  /**
   * Determine whether the given class is a candidate for carrying one of the specified
   * annotations (at type, method or field level).
   *
   * @param clazz
   *         the class to introspect
   * @param annotationTypes
   *         the searchable annotation types
   *
   * @return {@code false} if the class is known to have no such annotations at any level;
   * {@code true} otherwise. Callers will usually perform full method/field introspection
   * if {@code true} is being returned here.
   *
   * @see #isCandidateClass(Class, Class)
   * @see #isCandidateClass(Class, String)
   * @since 3.0
   */
  public static boolean isCandidateClass(Class<?> clazz, Collection<Class<? extends Annotation>> annotationTypes) {
    for (Class<? extends Annotation> annotationType : annotationTypes) {
      if (isCandidateClass(clazz, annotationType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether the given class is a candidate for carrying the specified annotation
   * (at type, method or field level).
   *
   * @param clazz
   *         the class to introspect
   * @param annotationType
   *         the searchable annotation type
   *
   * @return {@code false} if the class is known to have no such annotations at any level;
   * {@code true} otherwise. Callers will usually perform full method/field introspection
   * if {@code true} is being returned here.
   *
   * @see #isCandidateClass(Class, String)
   * @since 3.0
   */
  public static boolean isCandidateClass(Class<?> clazz, Class<? extends Annotation> annotationType) {
    return isCandidateClass(clazz, annotationType.getName());
  }

  /**
   * Determine whether the given class is a candidate for carrying the specified annotation
   * (at type, method or field level).
   *
   * @param clazz
   *         the class to introspect
   * @param annotationName
   *         the fully-qualified name of the searchable annotation type
   *
   * @return {@code false} if the class is known to have no such annotations at any level;
   * {@code true} otherwise. Callers will usually perform full method/field introspection
   * if {@code true} is being returned here.
   *
   * @see #isCandidateClass(Class, Class)
   * @since 3.0
   */
  public static boolean isCandidateClass(Class<?> clazz, String annotationName) {
    if (annotationName.startsWith("java.")) {
      return true;
    }
    return !hasPlainJavaAnnotationsOnly(clazz);
  }

  static boolean hasPlainJavaAnnotationsOnly(Class<?> type) {
    return (type.getName().startsWith("java.") || type == Ordered.class);
  }

  /**
   * Given a method, which may come from an interface, and a target class used
   * in the current reflective invocation, find the corresponding target method
   * if there is one. E.g. the method may be {@code IFoo.bar()} and the
   * target class may be {@code DefaultFoo}. In this case, the method may be
   * {@code DefaultFoo.bar()}. This enables attributes on that method to be found.
   *
   * @param method
   *         the method to be invoked, which may come from an interface
   * @param targetClass
   *         the target class for the current invocation
   *         (may be {@code null} or may not even implement the method)
   *
   * @return the specific target method, or the original method if the
   * {@code targetClass} does not implement it
   *
   * @since 3.0
   */
  public static Method getMostSpecificMethod(Method method, Class<?> targetClass) {
    if (targetClass != null && targetClass != method.getDeclaringClass() && isOverridable(method, targetClass)) {
      try {
        if (Modifier.isPublic(method.getModifiers())) {
          try {
            return targetClass.getMethod(method.getName(), method.getParameterTypes());
          }
          catch (NoSuchMethodException ex) {
            return method;
          }
        }
        else {
          Method specificMethod =
                  ReflectionUtils.findMethod(targetClass, method.getName(), method.getParameterTypes());
          return (specificMethod != null ? specificMethod : method);
        }
      }
      catch (SecurityException ex) {
        // Security settings are disallowing reflective access; fall back to 'method' below.
      }
    }
    return method;
  }

  /**
   * Determine whether the given method is overridable in the given target class.
   *
   * @param method
   *         the method to check
   * @param targetClass
   *         the target class to check against
   */
  private static boolean isOverridable(Method method, Class<?> targetClass) {
    if (Modifier.isPrivate(method.getModifiers())) {
      return false;
    }
    if (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers())) {
      return true;
    }
    return (targetClass == null ||
            getPackageName(method.getDeclaringClass()).equals(getPackageName(targetClass)));
  }

  /**
   * Determine the name of the package of the given class,
   * e.g. "java.lang" for the {@code java.lang.String} class.
   *
   * @param clazz
   *         the class
   *
   * @return the package name, or the empty String if the class
   * is defined in the default package
   *
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
   * @param fqClassName
   *         the fully-qualified class name
   *
   * @return the package name, or the empty String if the class
   * is defined in the default package
   *
   * @since 3.0
   */
  public static String getPackageName(String fqClassName) {
    Assert.notNull(fqClassName, "Class name must not be null");
    int lastDotIndex = fqClassName.lastIndexOf(Constant.PACKAGE_SEPARATOR);
    return (lastDotIndex != -1 ? fqClassName.substring(0, lastDotIndex) : Constant.BLANK);
  }

  /**
   * Adapt the given arguments to the target signature in the given method,
   * if necessary: in particular, if a given vararg argument array does not
   * match the array type of the declared vararg parameter in the method.
   *
   * @param method
   *         the target method
   * @param arguments
   *         the given arguments
   *
   * @return a cloned argument array, or the original if no adaptation is needed
   */
  public static Object[] adaptArgumentsIfNecessary(Method method, Object[] arguments) {
    if (ObjectUtils.isEmpty(arguments)) {
      return Constant.EMPTY_OBJECT_ARRAY;
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
   * @param clazz
   *         the class
   *
   * @return the qualified name of the class
   *
   * @since 3.0
   */
  public static String getQualifiedName(Class<?> clazz) {
    Assert.notNull(clazz, "Class must not be null");
    return clazz.getTypeName();
  }

  /**
   * Get the class name without the qualified package name.
   *
   * @param className
   *         the className to get the short name for
   *
   * @return the class name of the class without the package name
   *
   * @throws IllegalArgumentException
   *         if the className is empty
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

  /**
   * Get the class name without the qualified package name.
   *
   * @param clazz
   *         the class to get the short name for
   *
   * @return the class name of the class without the package name
   *
   * @since 3.0
   */
  public static String getShortName(Class<?> clazz) {
    return getShortName(getQualifiedName(clazz));
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
   * @param clazz
   *         the input class. A {@code null} value or the default
   *         (empty) package will result in an empty string ("") being returned.
   *
   * @return a path which represents the package name
   *
   * @see ClassLoader#getResource
   * @see Class#getResource
   * @since 3.0
   */
  public static String classPackageAsResourcePath(Class<?> clazz) {
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
   * @throws IllegalArgumentException
   *         target is not a enum
   * @since 3.0
   */
  public static Class<?> getEnumType(final Class<?> targetType) {
    Class<?> enumType = targetType;
    while (enumType != null && !enumType.isEnum()) {
      enumType = enumType.getSuperclass();
    }
    Assert.notNull(enumType, () -> "The target type " + targetType.getName() + " does not refer to an enum");
    return enumType;
  }

}
