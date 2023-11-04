/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans;

import java.beans.ConstructorProperties;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.support.DependencyInjector;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Static convenience methods for JavaBeans: for instantiating beans,
 * checking bean property types, copying bean properties, etc.
 *
 * <p>Mainly for internal use within the framework, but to some degree also
 * useful for application classes. Consider
 * <a href="https://commons.apache.org/proper/commons-beanutils/">Apache Commons BeanUtils</a>,
 * <a href="https://hotelsdotcom.github.io/bull/">BULL - Bean Utils Light Library</a>,
 * or similar third-party frameworks for more comprehensive bean utilities.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author TODAY 2021/8/22 21:51
 * @since 4.0
 */
public abstract class BeanUtils {

  private static final Set<Class<?>> unknownEditorTypes =
          Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

  private static final Class<? extends Annotation> Autowired = ClassUtils.resolveClassName(
          "cn.taketoday.beans.factory.annotation.Autowired", BeanUtils.class.getClassLoader());

  private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES = Map.of(
          boolean.class, false,
          byte.class, (byte) 0,
          short.class, (short) 0,
          int.class, 0,
          long.class, 0L,
          float.class, 0F,
          double.class, 0D,
          char.class, '\0'
  );

  /**
   * Get instance with bean class use default {@link Constructor}
   *
   * @param beanClass bean class
   * @return the instance of target class
   * @throws BeanInstantiationException if any reflective operation exception occurred
   * @since 2.1.2
   */
  public static <T> T newInstance(Class<T> beanClass) {
    Constructor<T> constructor = obtainConstructor(beanClass);
    return newInstance(constructor);
  }

  /**
   * Get instance with bean class
   *
   * @param beanClassName bean class name string
   * @return the instance of target class
   * @see #obtainConstructor(Class)
   * @since 2.1.2
   */
  public static <T> T newInstance(String beanClassName, @Nullable ClassLoader classLoader) {
    return newInstance(ClassUtils.resolveClassName(beanClassName, classLoader));
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass target bean class
   * @param providedArgs User provided arguments
   * @return bean class 's instance
   * @throws BeanInstantiationException if any reflective operation exception occurred
   * @see #obtainConstructor(Class)
   * @since 4.0
   */
  public static <T> T newInstance(Class<T> beanClass, DependencyInjector injector, @Nullable Object... providedArgs) {
    Assert.notNull(injector, "ArgumentsResolver must not be null");
    Constructor<T> constructor = obtainConstructor(beanClass);
    return injector.inject(constructor, providedArgs);
  }

  /**
   * @throws BeanInstantiationException cannot instantiate a bean
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(BeanInstantiator constructor, @Nullable Object[] parameter) {
    return (T) constructor.instantiate(parameter);
  }

  /**
   * Convenience method to instantiate a class using the given constructor.
   * <p>Note that this method tries to set the constructor accessible if given a
   * non-accessible (that is, non-public) constructor,
   * with optional parameters and default values.
   *
   * @param constructor the constructor to instantiate
   * @param args the constructor arguments to apply (use {@code null} for an unspecified
   * parameter)
   * @return the new instance
   * @throws BeanInstantiationException if the bean cannot be instantiated
   * @see Constructor#newInstance
   */
  public static <T> T newInstance(Constructor<T> constructor, @Nullable Object... args) {
    if (ObjectUtils.isNotEmpty(args)) {
      if (args.length > constructor.getParameterCount()) {
        throw new BeanInstantiationException(
                constructor, "Illegal arguments for constructor, can't specify more arguments than constructor parameters", null);
      }
      int i = 0;
      Class<?>[] parameterTypes = null;
      for (Object arg : args) {
        if (arg == null) {
          if (parameterTypes == null) {
            parameterTypes = constructor.getParameterTypes();
          }
          Class<?> parameterType = parameterTypes[i];
          // argsWithDefaultValues
          args[i] = parameterType.isPrimitive() ? DEFAULT_TYPE_VALUES.get(parameterType) : null;
        }
        i++;
      }
    }
    try {
      ReflectionUtils.makeAccessible(constructor);
      return constructor.newInstance(args);
    }
    catch (InstantiationException ex) {
      throw new BeanInstantiationException(constructor, "Is it an abstract class?", ex);
    }
    catch (IllegalAccessException ex) {
      throw new BeanInstantiationException(constructor, "Is the constructor accessible?", ex);
    }
    catch (IllegalArgumentException ex) {
      throw new BeanInstantiationException(constructor, "Illegal arguments for constructor", ex);
    }
    catch (InvocationTargetException ex) {
      throw new BeanInstantiationException(constructor, "Constructor threw exception", ex.getTargetException());
    }
  }

  /**
   * Obtain a suitable {@link Constructor}.
   * <p>
   * Look for the default constructor, if there is no default constructor, then
   * get all the constructors, if there is only one constructor then use this
   * constructor, if not more than one use the @Autowired constructor if there is
   * no suitable {@link Constructor} will throw an exception
   * <p>
   *
   * @param <T> Target type
   * @param beanClass target bean class
   * @return Suitable constructor
   * @throws ConstructorNotFoundException If there is no suitable constructor
   * @since 2.1.7
   */
  public static <T> Constructor<T> obtainConstructor(Class<T> beanClass) {
    final Constructor<T> ret = getConstructor(beanClass);
    if (ret == null) {
      throw new ConstructorNotFoundException(beanClass);
    }
    return ret;
  }

  /**
   * Get a suitable {@link Constructor}.
   * <p>
   * Look for the default constructor, if there is no default constructor, then
   * get all the constructors, if there is only one constructor then use this
   * constructor, if not more than one use the @Autowired constructor if there is
   * no suitable {@link Constructor} will throw an exception
   * <p>
   *
   * @param <T> Target type
   * @param beanClass target bean class
   * @return Suitable constructor If there isn't a suitable {@link Constructor}
   * returns null
   * @since 2.1.7
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> getConstructor(Class<T> beanClass) {
    Assert.notNull(beanClass, "bean-class must not be null");
    Constructor<?>[] ctors = beanClass.getConstructors();
    if (ctors.length == 1) {
      // A single public constructor
      return (Constructor<T>) ctors[0];
    }
    else if (ctors.length == 0) {
      ctors = beanClass.getDeclaredConstructors();
      if (ctors.length == 1) {
        // A single non-public constructor, e.g. from a non-public record type
        return (Constructor<T>) ctors[0];
      }
    }

    return selectConstructor(ctors);
  }

  /**
   * select a suitable {@link Constructor}.
   *
   * @param <T> Target type
   * @return Suitable constructor If there isn't a suitable {@link Constructor}
   * returns null
   * @since 4.0
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> selectConstructor(Constructor<?>[] ctors) {
    if (ctors.length == 1) {
      // A single constructor
      return (Constructor<T>) ctors[0];
    }

    // iterate all constructors
    for (Constructor<?> constructor : ctors) {
      if (constructor.getParameterCount() == 0 // default constructor
              || constructor.isAnnotationPresent(Autowired)) {
        return (Constructor<T>) constructor;
      }
    }
    return null;
  }

  /**
   * Obtain a new MethodParameter object for the write method of the
   * specified property.
   *
   * @param pd the PropertyDescriptor for the property
   * @return a corresponding MethodParameter object
   */
  public static MethodParameter getWriteMethodParameter(PropertyDescriptor pd) {
    if (pd instanceof GenericTypeAwarePropertyDescriptor) {
      return new MethodParameter(((GenericTypeAwarePropertyDescriptor) pd).getWriteMethodParameter());
    }
    else {
      Method writeMethod = pd.getWriteMethod();
      Assert.state(writeMethod != null, "No write method available");
      return new MethodParameter(writeMethod, 0);
    }
  }

  /**
   * Determine required parameter names for the given constructor,
   * considering the JavaBeans {@link ConstructorProperties} annotation
   * as well as Framework's {@link DefaultParameterNameDiscoverer}.
   *
   * @param ctor the constructor to find parameter names for
   * @return the parameter names (matching the constructor's parameter count)
   * @throws IllegalStateException if the parameter names are not resolvable
   * @see ConstructorProperties
   * @see DefaultParameterNameDiscoverer
   * @since 4.0
   */
  public static String[] getParameterNames(Constructor<?> ctor) {
    ConstructorProperties cp = ctor.getAnnotation(ConstructorProperties.class);
    String[] paramNames = cp != null ? cp.value() : ParameterNameDiscoverer.findParameterNames(ctor);
    if (paramNames == null) {
      throw new IllegalStateException("Cannot resolve parameter names for constructor " + ctor);
    }
    if (paramNames.length != ctor.getParameterCount()) {
      throw new IllegalStateException(
              "Invalid number of parameter names: " + paramNames.length + " for constructor " + ctor);
    }
    return paramNames;
  }

  /**
   * Check if the given type represents a "simple" property: a simple value
   * type or an array of simple value types.
   * <p>See {@link #isSimpleValueType(Class)} for the definition of <em>simple
   * value type</em>.
   * <p>Used to determine properties to check for a "simple" dependency-check.
   *
   * @param type the type to check
   * @return whether the given type represents a "simple" property
   * @see #isSimpleValueType(Class)
   * @since 4.0
   */
  public static boolean isSimpleProperty(Class<?> type) {
    Assert.notNull(type, "'type' must not be null");
    return isSimpleValueType(type) || (type.isArray() && isSimpleValueType(type.getComponentType()));
  }

  /**
   * Check if the given type represents a "simple" value type for
   * bean property and data binding purposes:
   * a primitive or primitive wrapper, an {@code Enum}, a {@code String}
   * or other {@code CharSequence}, a {@code Number}, a {@code Date},
   * a {@code Temporal}, a {@code UUID}, a {@code URI}, a {@code URL},
   * a {@code Locale}, or a {@code Class}.
   * <p>{@code Void} and {@code void} are not considered simple value types.
   * <p>this method delegates to {@link ClassUtils#isSimpleValueType}
   * as-is but could potentially add further rules for bean property purposes.
   *
   * @param type the type to check
   * @return whether the given type represents a "simple" value type
   * @see #isSimpleProperty(Class)
   * @see ClassUtils#isSimpleValueType(Class)
   * @since 4.0
   */
  public static boolean isSimpleValueType(Class<?> type) {
    return ClassUtils.isSimpleValueType(type);
  }

  /**
   * Determine the bean property type for the given property from the
   * given classes/interfaces, if possible.
   *
   * @param propertyName the name of the bean property
   * @param beanClasses the classes to check against
   * @return the property type, or {@code Object.class} as fallback
   * @since 4.0
   */
  public static Class<?> findPropertyType(String propertyName, @Nullable Class<?>... beanClasses) {
    if (beanClasses != null) {
      for (Class<?> beanClass : beanClasses) {
        PropertyDescriptor pd = getPropertyDescriptor(beanClass, propertyName);
        if (pd != null) {
          return pd.getPropertyType();
        }
      }
    }
    return Object.class;
  }

  /**
   * Retrieve the JavaBeans {@code PropertyDescriptor}s of a given class.
   *
   * @param clazz the Class to retrieve the PropertyDescriptors for
   * @return an array of {@code PropertyDescriptors} for the given class
   * @throws BeansException if PropertyDescriptor look fails
   * @since 4.0
   */
  public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
    return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptors();
  }

  /**
   * Retrieve the JavaBeans {@code PropertyDescriptors} for the given property.
   *
   * @param clazz the Class to retrieve the PropertyDescriptor for
   * @param propertyName the name of the property
   * @return the corresponding PropertyDescriptor, or {@code null} if none
   * @throws BeansException if PropertyDescriptor lookup fails
   * @since 4.0
   */
  @Nullable
  public static PropertyDescriptor getPropertyDescriptor(Class<?> clazz, String propertyName) throws BeansException {
    return CachedIntrospectionResults.forClass(clazz).getPropertyDescriptor(propertyName);
  }

  /**
   * Find a JavaBeans {@code PropertyDescriptor} for the given method,
   * with the method either being the read method or the write method for
   * that bean property.
   *
   * @param method the method to find a corresponding PropertyDescriptor for,
   * introspecting its declaring class
   * @return the corresponding PropertyDescriptor, or {@code null} if none
   * @throws BeansException if PropertyDescriptor lookup fails
   * @since 4.0
   */
  @Nullable
  public static PropertyDescriptor findPropertyForMethod(Method method) throws BeansException {
    return findPropertyForMethod(method, method.getDeclaringClass());
  }

  /**
   * Find a JavaBeans {@code PropertyDescriptor} for the given method,
   * with the method either being the read method or the write method for
   * that bean property.
   *
   * @param method the method to find a corresponding PropertyDescriptor for
   * @param clazz the (most specific) class to introspect for descriptors
   * @return the corresponding PropertyDescriptor, or {@code null} if none
   * @throws BeansException if PropertyDescriptor lookup fails
   * @since 4.0
   */
  @Nullable
  public static PropertyDescriptor findPropertyForMethod(Method method, Class<?> clazz) throws BeansException {
    Assert.notNull(method, "Method must not be null");
    PropertyDescriptor[] pds = getPropertyDescriptors(clazz);
    for (PropertyDescriptor pd : pds) {
      if (method.equals(pd.getReadMethod()) || method.equals(pd.getWriteMethod())) {
        return pd;
      }
    }
    return null;
  }

  /**
   * Find a JavaBeans PropertyEditor following the 'Editor' suffix convention
   * (e.g. "mypackage.MyDomainClass" &rarr; "mypackage.MyDomainClassEditor").
   * <p>Compatible to the standard JavaBeans convention as implemented by
   * {@link java.beans.PropertyEditorManager} but isolated from the latter's
   * registered default editors for primitive types.
   *
   * @param targetType the type to find an editor for
   * @return the corresponding editor, or {@code null} if none found
   * @since 4.0
   */
  @Nullable
  public static PropertyEditor findEditorByConvention(@Nullable Class<?> targetType) {
    if (targetType == null || targetType.isArray() || unknownEditorTypes.contains(targetType)) {
      return null;
    }

    ClassLoader cl = targetType.getClassLoader();
    if (cl == null) {
      try {
        cl = ClassLoader.getSystemClassLoader();
        if (cl == null) {
          return null;
        }
      }
      catch (Throwable ex) {
        // e.g. AccessControlException on Google App Engine
        return null;
      }
    }

    String targetTypeName = targetType.getName();
    String editorName = targetTypeName + "Editor";
    try {
      Class<?> editorClass = cl.loadClass(editorName);
      if (editorClass != null) {
        if (!PropertyEditor.class.isAssignableFrom(editorClass)) {
          unknownEditorTypes.add(targetType);
          return null;
        }
        return (PropertyEditor) newInstance(editorClass);
      }
      // Misbehaving ClassLoader returned null instead of ClassNotFoundException
      // - fall back to unknown editor type registration below
    }
    catch (ClassNotFoundException ex) {
      // Ignore - fall back to unknown editor type registration below
    }
    unknownEditorTypes.add(targetType);
    return null;
  }

  /**
   * Parse a method signature in the form {@code methodName[([arg_list])]},
   * where {@code arg_list} is an optional, comma-separated list of fully-qualified
   * type names, and attempts to resolve that signature against the supplied {@code Class}.
   * <p>When not supplying an argument list ({@code methodName}) the method whose name
   * matches and has the least number of parameters will be returned. When supplying an
   * argument type list, only the method whose name and argument types match will be returned.
   * <p>Note then that {@code methodName} and {@code methodName()} are <strong>not</strong>
   * resolved in the same way. The signature {@code methodName} means the method called
   * {@code methodName} with the least number of arguments, whereas {@code methodName()}
   * means the method called {@code methodName} with exactly 0 arguments.
   * <p>If no method can be found, then {@code null} is returned.
   *
   * @param signature the method signature as String representation
   * @param clazz the class to resolve the method signature against
   * @return the resolved Method
   * @see #findMethod
   * @see ReflectionUtils#findMethodWithMinimalParameters
   * @since 4.0
   */
  @Nullable
  public static Method resolveSignature(String signature, Class<?> clazz) {
    Assert.hasText(signature, "'signature' must not be empty");
    Assert.notNull(clazz, "Class must not be null");
    int startParen = signature.indexOf('(');
    int endParen = signature.indexOf(')');
    if (startParen > -1 && endParen == -1) {
      throw new IllegalArgumentException(
              "Invalid method signature '" + signature + "': expected closing ')' for args list");
    }
    else if (startParen == -1 && endParen > -1) {
      throw new IllegalArgumentException(
              "Invalid method signature '" + signature + "': expected opening '(' for args list");
    }
    else if (startParen == -1) {
      return ReflectionUtils.findMethodWithMinimalParameters(clazz, signature);
    }
    else {
      String methodName = signature.substring(0, startParen);
      String[] parameterTypeNames =
              StringUtils.commaDelimitedListToStringArray(signature.substring(startParen + 1, endParen));
      Class<?>[] parameterTypes = new Class<?>[parameterTypeNames.length];
      for (int i = 0; i < parameterTypeNames.length; i++) {
        String parameterTypeName = parameterTypeNames[i].trim();
        try {
          parameterTypes[i] = ClassUtils.forName(parameterTypeName, clazz.getClassLoader());
        }
        catch (Throwable ex) {
          throw new IllegalArgumentException("Invalid method signature: unable to resolve type [" +
                  parameterTypeName + "] for argument " + i + ". Root cause: " + ex);
        }
      }
      return findMethod(clazz, methodName, parameterTypes);
    }
  }

  /**
   * Find a method with the given method name and the given parameter types,
   * declared on the given class or one of its superclasses. Prefers public methods,
   * but will return a protected, package access, or private method too.
   * <p>Checks {@code Class.getMethod} first, falling back to
   * {@code findDeclaredMethod}. This allows to find public methods
   * without issues even in environments with restricted Java security settings.
   *
   * @param clazz the class to check
   * @param methodName the name of the method to find
   * @param paramTypes the parameter types of the method to find
   * @return the Method object, or {@code null} if not found
   * @see Class#getMethod
   * @see #findDeclaredMethod
   * @since 4.0
   */
  @Nullable
  public static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
    try {
      return clazz.getMethod(methodName, paramTypes);
    }
    catch (NoSuchMethodException ex) {
      return findDeclaredMethod(clazz, methodName, paramTypes);
    }
  }

  /**
   * Find a method with the given method name and the given parameter types,
   * declared on the given class or one of its superclasses. Will return a public,
   * protected, package access, or private method.
   * <p>Checks {@code Class.getDeclaredMethod}, cascading upwards to all superclasses.
   *
   * @param clazz the class to check
   * @param methodName the name of the method to find
   * @param paramTypes the parameter types of the method to find
   * @return the Method object, or {@code null} if not found
   * @see Class#getDeclaredMethod
   * @since 4.0
   */
  @Nullable
  public static Method findDeclaredMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
    try {
      return clazz.getDeclaredMethod(methodName, paramTypes);
    }
    catch (NoSuchMethodException ex) {
      if (clazz.getSuperclass() != null) {
        return findDeclaredMethod(clazz.getSuperclass(), methodName, paramTypes);
      }
      return null;
    }
  }

  /**
   * Copy the property values of the given source bean into the target bean.
   * <p>Note: The source and target classes do not have to match or even be derived
   * from each other, as long as the properties match. Any bean properties that the
   * source bean exposes but the target bean does not will silently be ignored.
   * <p>This is just a convenience method. For more complex transfer needs,
   * consider using a full BeanWrapper.
   *
   * @param source the source bean
   * @param target the target bean
   * @throws BeansException if the copying failed
   * @see BeanWrapper
   * @since 4.0
   */
  public static void copyProperties(Object source, Object target) throws BeansException {
    copyProperties(source, target, null, null);
  }

  /**
   * Copy the property values of the given source bean into the given target bean,
   * only setting properties defined in the given "editable" class (or interface).
   * <p>Note: The source and target classes do not have to match or even be derived
   * from each other, as long as the properties match. Any bean properties that the
   * source bean exposes but the target bean does not will silently be ignored.
   * <p>This is just a convenience method. For more complex transfer needs,
   * consider using a full BeanWrapper.
   *
   * @param source the source bean
   * @param target the target bean
   * @param editable the class (or interface) to restrict property setting to
   * @throws BeansException if the copying failed
   * @see BeanWrapper
   * @since 4.0
   */
  public static void copyProperties(
          Object source, Object target, Class<?> editable) throws BeansException {
    copyProperties(source, target, editable, null);
  }

  /**
   * Copy the property values of the given source bean into the given target bean,
   * ignoring the given "ignoreProperties".
   * <p>Note: The source and target classes do not have to match or even be derived
   * from each other, as long as the properties match. Any bean properties that the
   * source bean exposes but the target bean does not will silently be ignored.
   * <p>This is just a convenience method. For more complex transfer needs,
   * consider using a full BeanWrapper.
   *
   * @param source the source bean
   * @param target the target bean
   * @param ignoreProperties array of property names to ignore
   * @throws BeansException if the copying failed
   * @see BeanWrapper
   * @since 4.0
   */
  public static void copyProperties(
          Object source, Object target, String... ignoreProperties) throws BeansException {
    copyProperties(source, target, null, ignoreProperties);
  }

  /**
   * Copy the property values of the given source bean into the given target bean.
   * <p>Note: The source and target classes do not have to match or even be derived
   * from each other, as long as the properties match. Any bean properties that the
   * source bean exposes but the target bean does not will silently be ignored.
   * <p>this method honors generic type information when matching properties
   * in the source and target objects.
   *
   * @param source the source bean
   * @param target the target bean
   * @param editable the class (or interface) to restrict property setting to
   * @param ignoreProperties array of property names to ignore
   * @throws BeansException if the copying failed
   * @see BeanWrapper
   * @since 4.0
   */
  private static void copyProperties(Object source, Object target,
          @Nullable Class<?> editable, @Nullable String[] ignoreProperties) throws BeansException {
    Assert.notNull(source, "Source must not be null");
    Assert.notNull(target, "Target must not be null");
    Class<?> actualEditable;
    if (editable != null) {
      if (!editable.isInstance(target)) {
        throw new IllegalArgumentException("Target class [" + target.getClass().getName()
                + "] not assignable to Editable class [" + editable.getName() + "]");
      }
      actualEditable = editable;
    }
    else {
      actualEditable = target.getClass();
    }

    PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
    Set<String> ignoreSet =
            ObjectUtils.isNotEmpty(ignoreProperties) ? Set.of(ignoreProperties) : Collections.emptySet();

    if (source.getClass() == actualEditable) {
      for (PropertyDescriptor targetPd : targetPds) {
        Method writeMethod = targetPd.getWriteMethod();
        // filter
        if (writeMethod != null && !ignoreSet.contains(targetPd.getName())) {
          Method readMethod = targetPd.getReadMethod();
          if (readMethod != null && isAssignable(writeMethod, readMethod)) {
            try {
              doCopy(source, target, writeMethod, readMethod);
            }
            catch (Throwable ex) {
              throw new FatalBeanException(
                      "Could not copy property '" + targetPd.getName() + "' from source to target", ex);
            }
          }
        }
      }
    }
    else {
      CachedIntrospectionResults sourceResults =
              CachedIntrospectionResults.forClass(source.getClass());
      for (PropertyDescriptor targetPd : targetPds) {
        Method writeMethod = targetPd.getWriteMethod();
        // filter
        if (writeMethod != null && !ignoreSet.contains(targetPd.getName())) {
          // not a same type
          PropertyDescriptor sourcePd = sourceResults.getPropertyDescriptor(targetPd.getName());
          if (sourcePd != null) {
            Method readMethod = sourcePd.getReadMethod();
            if (readMethod != null && isAssignable(writeMethod, readMethod)) {
              try {
                doCopy(source, target, writeMethod, readMethod);
              }
              catch (Throwable ex) {
                throw new FatalBeanException(
                        "Could not copy property '" + targetPd.getName() + "' from source to target", ex);
              }
            }
          }
        }
      }
    }
  }

  private static void doCopy(Object source, Object target,
          Method writeMethod, Method readMethod) throws Exception {
    if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
      ReflectionUtils.makeAccessible(readMethod);
    }
    Object value = readMethod.invoke(source);
    if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
      ReflectionUtils.makeAccessible(readMethod);
    }
    writeMethod.invoke(target, value);
  }

  private static boolean isAssignable(Method writeMethod, Method readMethod) {
    ResolvableType sourceResolvableType = ResolvableType.forReturnType(readMethod);
    ResolvableType targetResolvableType = ResolvableType.forParameter(writeMethod, 0);

    // Ignore generic types in assignable check if either ResolvableType has unresolvable generics.
    return (sourceResolvableType.hasUnresolvableGenerics()
            || targetResolvableType.hasUnresolvableGenerics())
           ? ClassUtils.isAssignable(writeMethod.getParameterTypes()[0], readMethod.getReturnType())
           : targetResolvableType.isAssignableFrom(sourceResolvableType);
  }

}
