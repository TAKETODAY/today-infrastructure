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

package cn.taketoday.beans.factory.support;

import java.beans.ConstructorProperties;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.time.temporal.Temporal;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.BeanInstantiationException;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.DependencyInjectorProvider;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.support.BeanInstantiator;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.core.DefaultParameterNameDiscoverer;
import cn.taketoday.core.ParameterNameDiscoverer;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ConcurrentReferenceHashMap;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.util.ReflectionUtils;

/**
 * @author TODAY 2021/8/22 21:51
 * @since 4.0
 */
public abstract class BeanUtils {

  private static final ParameterNameDiscoverer parameterNameDiscoverer =
          new DefaultParameterNameDiscoverer();

  private static final Set<Class<?>> unknownEditorTypes =
          Collections.newSetFromMap(new ConcurrentReferenceHashMap<>(64));

  private static final Map<Class<?>, Object> DEFAULT_TYPE_VALUES = Map.of(
          boolean.class, false,
          byte.class, (byte) 0,
          short.class, (short) 0,
          int.class, 0,
          long.class, 0L,
          float.class, 0F,
          double.class, 0D,
          char.class, '\0');

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
    return newInstance(constructor, null);
  }

  /**
   * Get instance with bean class
   *
   * @param beanClassName bean class name string
   * @return the instance of target class
   * @throws ClassNotFoundException If the class was not found
   * @see #obtainConstructor(Class)
   * @since 2.1.2
   */
  public static <T> T newInstance(String beanClassName) throws ClassNotFoundException {
    return newInstance(ClassUtils.forName(beanClassName));
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass target bean class
   * @param beanFactory bean factory
   * @return bean class 's instance
   * @throws BeanInstantiationException if any reflective operation exception occurred
   * @see #obtainConstructor(Class)
   */
  public static <T> T newInstance(final Class<T> beanClass, final DependencyInjectorProvider beanFactory) {
    return newInstance(beanClass, beanFactory, null);
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass target bean class
   * @param providedArgs User provided arguments
   * @return bean class 's instance
   * @throws BeanInstantiationException if any reflective operation exception occurred
   * @see #obtainConstructor(Class)
   */
  public static <T> T newInstance(
          Class<T> beanClass, @Nullable DependencyInjectorProvider injectorProvider, @Nullable Object[] providedArgs) {
    Constructor<T> constructor = obtainConstructor(beanClass);
    if (constructor.getParameterCount() == 0) {
      return newInstance(constructor, null);
    }
    Assert.notNull(injectorProvider, "resolverProvider is required");
    return injectorProvider.getInjector().inject(constructor, providedArgs);
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
  public static <T> T newInstance(
          Class<T> beanClass, DependencyInjector injector, @Nullable Object... providedArgs) {
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
   * use Constructor to create bean instance
   *
   * @param constructor java reflect Constructor
   * @param parameter initargs
   * @param <T> target bean type
   * @return instance create from constructor
   * @see Constructor#newInstance(Object...)
   */
  public static <T> T newInstance(Constructor<T> constructor, @Nullable Object[] parameter) {
    try {
      ReflectionUtils.makeAccessible(constructor);
      return constructor.newInstance(parameter);
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
    Constructor<T>[] constructors = (Constructor<T>[]) beanClass.getDeclaredConstructors();
    if (constructors.length == 1) {
      return ReflectionUtils.makeAccessible(constructors[0]);
    }
    for (final Constructor<T> constructor : constructors) {
      if (constructor.getParameterCount() == 0 // default constructor
              || constructor.isAnnotationPresent(Autowired.class)) {
        return ReflectionUtils.makeAccessible(constructor);
      }
    }
    return null;
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
    String[] paramNames = cp != null ? cp.value() : parameterNameDiscoverer.getParameterNames(ctor);
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

  public static PropertyDescriptor[] getPropertyDescriptors(Class<?> clazz) throws BeansException {
    try {
      return Introspector.getBeanInfo(clazz).getPropertyDescriptors();
    }
    catch (IntrospectionException e) {
      throw ExceptionUtils.sneakyThrow(e);
    }
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
   * Check if the given type represents a "simple" value type: a primitive or
   * primitive wrapper, an enum, a String or other CharSequence, a Number, a
   * Date, a Temporal, a URI, a URL, a Locale, or a Class.
   * <p>{@code Void} and {@code void} are not considered simple value types.
   *
   * @param type the type to check
   * @return whether the given type represents a "simple" value type
   * @see #isSimpleProperty(Class)
   * @since 4.0
   */
  public static boolean isSimpleValueType(Class<?> type) {
    return Void.class != type && void.class != type
            && (
            ClassUtils.isPrimitiveOrWrapper(type)
                    || URI.class == type
                    || URL.class == type
                    || Class.class == type
                    || Locale.class == type
                    || Date.class.isAssignableFrom(type)
                    || Enum.class.isAssignableFrom(type)
                    || Number.class.isAssignableFrom(type)
                    || Temporal.class.isAssignableFrom(type)
                    || CharSequence.class.isAssignableFrom(type)
    );
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
        BeanProperty beanProperty = BeanMetadata.from(beanClass).getBeanProperty(propertyName);
        if (beanProperty != null) {
          return beanProperty.getType();
        }
      }
    }
    return Object.class;
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

}
