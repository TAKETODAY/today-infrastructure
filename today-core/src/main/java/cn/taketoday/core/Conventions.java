/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;

/**
 * Provides methods to support various naming and other conventions used
 * throughout the framework. Mainly for internal use within the framework.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @author Harry Yang 2021/11/5 11:10
 * @since 4.0
 */
public final class Conventions {

  /**
   * Suffix added to names when using arrays.
   */
  private static final String PLURAL_SUFFIX = "List";

  private Conventions() { }

  /**
   * Determine the conventional variable name for the supplied {@code Object}
   * based on its concrete type. The convention used is to return the
   * un-capitalized short name of the {@code Class}, according to JavaBeans
   * property naming rules.
   * <p>For example:<br>
   * {@code com.myapp.Product} becomes {@code "product"}<br>
   * {@code com.myapp.MyProduct} becomes {@code "myProduct"}<br>
   * {@code com.myapp.UKProduct} becomes {@code "UKProduct"}<br>
   * <p>For arrays the pluralized version of the array component type is used.
   * For {@code Collection}s an attempt is made to 'peek ahead' to determine
   * the component type and return its pluralized version.
   *
   * @param value the value to generate a variable name for
   * @return the generated variable name
   */
  public static String getVariableName(Object value) {
    Assert.notNull(value, "Value is required");
    Class<?> valueClass;
    boolean pluralize = false;

    if (value.getClass().isArray()) {
      valueClass = value.getClass().getComponentType();
      pluralize = true;
    }
    else if (value instanceof Collection<?> collection) {
      if (collection.isEmpty()) {
        throw new IllegalArgumentException(
                "Cannot generate variable name for an empty Collection");
      }
      Object valueToCheck = peekAhead(collection);
      valueClass = getClassForValue(valueToCheck);
      pluralize = true;
    }
    else {
      valueClass = getClassForValue(value);
    }

    String name = ClassUtils.getShortNameAsProperty(valueClass);
    return (pluralize ? pluralize(name) : name);
  }

  /**
   * Determine the conventional variable name for the given parameter taking
   * the generic collection type, if any, into account.
   * <p>this method supports reactive types:<br>
   * {@code Mono<com.myapp.Product>} becomes {@code "productMono"}<br>
   * {@code Flux<com.myapp.MyProduct>} becomes {@code "myProductFlux"}<br>
   * {@code Observable<com.myapp.MyProduct>} becomes {@code "myProductObservable"}<br>
   *
   * @param parameter the method or constructor parameter
   * @return the generated variable name
   */
  public static String getVariableNameForParameter(Parameter parameter) {
    Assert.notNull(parameter, "Parameter is required");
    Class<?> valueClass;
    boolean pluralize = false;
    String reactiveSuffix = "";

    if (parameter.getType().isArray()) {
      valueClass = parameter.getType().getComponentType();
      pluralize = true;
    }
    else if (Collection.class.isAssignableFrom(parameter.getType())) {
      valueClass = ResolvableType.fromParameter(parameter).asCollection().resolveGeneric();
      if (valueClass == null) {
        throw new IllegalArgumentException(
                "Cannot generate variable name for non-typed Collection parameter type");
      }
      pluralize = true;
    }
    else {
      valueClass = parameter.getType();
      ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
      if (adapter != null && !adapter.getDescriptor().isNoValue()) {
        reactiveSuffix = ClassUtils.getShortName(valueClass);
        valueClass = ResolvableType.fromParameter(parameter).getGeneric().toClass();
      }
    }

    String name = ClassUtils.getShortNameAsProperty(valueClass);
    return (pluralize ? pluralize(name) : name + reactiveSuffix);
  }

  /**
   * Determine the conventional variable name for the given parameter taking
   * the generic collection type, if any, into account.
   * <p>this method supports reactive types:<br>
   * {@code Mono<com.myapp.Product>} becomes {@code "productMono"}<br>
   * {@code Flux<com.myapp.MyProduct>} becomes {@code "myProductFlux"}<br>
   * {@code Observable<com.myapp.MyProduct>} becomes {@code "myProductObservable"}<br>
   *
   * @param parameter the method or constructor parameter
   * @return the generated variable name
   */
  public static String getVariableNameForParameter(MethodParameter parameter) {
    Assert.notNull(parameter, "MethodParameter is required");
    Class<?> valueClass;
    boolean pluralize = false;
    String reactiveSuffix = "";

    if (parameter.getParameterType().isArray()) {
      valueClass = parameter.getParameterType().getComponentType();
      pluralize = true;
    }
    else if (Collection.class.isAssignableFrom(parameter.getParameterType())) {
      valueClass = ResolvableType.forMethodParameter(parameter).asCollection().resolveGeneric();
      if (valueClass == null) {
        throw new IllegalArgumentException(
                "Cannot generate variable name for non-typed Collection parameter type");
      }
      pluralize = true;
    }
    else {
      valueClass = parameter.getParameterType();
      ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
      if (adapter != null && !adapter.getDescriptor().isNoValue()) {
        reactiveSuffix = ClassUtils.getShortName(valueClass);
        valueClass = parameter.nested().getNestedParameterType();
      }
    }

    String name = ClassUtils.getShortNameAsProperty(valueClass);
    return (pluralize ? pluralize(name) : name + reactiveSuffix);
  }

  /**
   * Determine the conventional variable name for the return type of the
   * given method, taking the generic collection type, if any, into account.
   *
   * @param method the method to generate a variable name for
   * @return the generated variable name
   */
  public static String getVariableNameForReturnType(Method method) {
    return getVariableNameForReturnType(method, method.getReturnType(), null);
  }

  /**
   * Determine the conventional variable name for the return type of the given
   * method, taking the generic collection type, if any, into account, falling
   * back on the given actual return value if the method declaration is not
   * specific enough, e.g. {@code Object} return type or untyped collection.
   *
   * @param method the method to generate a variable name for
   * @param value the return value (may be {@code null} if not available)
   * @return the generated variable name
   */
  public static String getVariableNameForReturnType(Method method, @Nullable Object value) {
    return getVariableNameForReturnType(method, method.getReturnType(), value);
  }

  /**
   * Determine the conventional variable name for the return type of the given
   * method, taking the generic collection type, if any, into account, falling
   * back on the given return value if the method declaration is not specific
   * enough, e.g. {@code Object} return type or untyped collection.
   * <p>this method supports reactive types:<br>
   * {@code Mono<com.myapp.Product>} becomes {@code "productMono"}<br>
   * {@code Flux<com.myapp.MyProduct>} becomes {@code "myProductFlux"}<br>
   * {@code Observable<com.myapp.MyProduct>} becomes {@code "myProductObservable"}<br>
   *
   * @param method the method to generate a variable name for
   * @param resolvedType the resolved return type of the method
   * @param value the return value (may be {@code null} if not available)
   * @return the generated variable name
   */
  public static String getVariableNameForReturnType(Method method, Class<?> resolvedType, @Nullable Object value) {
    Assert.notNull(method, "Method is required");

    if (Object.class == resolvedType) {
      Assert.notNull(value, "Cannot generate variable name for an Object return type with null value");
      return getVariableName(value);
    }

    Class<?> valueClass;
    boolean pluralize = false;
    String reactiveSuffix = "";

    if (resolvedType.isArray()) {
      valueClass = resolvedType.getComponentType();
      pluralize = true;
    }
    else if (Collection.class.isAssignableFrom(resolvedType)) {
      valueClass = ResolvableType.forReturnType(method).asCollection().resolveGeneric();
      if (valueClass == null) {
        if (!(value instanceof Collection<?> collection)) {
          throw new IllegalArgumentException("Cannot generate variable name " +
                  "for non-typed Collection return type and a non-Collection value");
        }
        if (collection.isEmpty()) {
          throw new IllegalArgumentException("Cannot generate variable name " +
                  "for non-typed Collection return type and an empty Collection value");
        }
        Object valueToCheck = peekAhead(collection);
        valueClass = getClassForValue(valueToCheck);
      }
      pluralize = true;
    }
    else {
      valueClass = resolvedType;
      ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(valueClass);
      if (adapter != null && !adapter.getDescriptor().isNoValue()) {
        reactiveSuffix = ClassUtils.getShortName(valueClass);
        valueClass = ResolvableType.forReturnType(method).getGeneric().toClass();
      }
    }

    String name = ClassUtils.getShortNameAsProperty(valueClass);
    return (pluralize ? pluralize(name) : name + reactiveSuffix);
  }

  /**
   * Convert {@code String}s in attribute name format (e.g. lowercase, hyphens
   * separating words) into property name format (camel-case). For example
   * {@code transaction-manager} becomes {@code "transactionManager"}.
   */
  public static String attributeNameToPropertyName(String attributeName) {
    Assert.notNull(attributeName, "'attributeName' is required");
    if (!attributeName.contains("-")) {
      return attributeName;
    }
    char[] result = new char[attributeName.length() - 1]; // not completely accurate but good guess
    int currPos = 0;
    boolean upperCaseNext = false;
    for (int i = 0; i < attributeName.length(); i++) {
      char c = attributeName.charAt(i);
      if (c == '-') {
        upperCaseNext = true;
      }
      else if (upperCaseNext) {
        result[currPos++] = Character.toUpperCase(c);
        upperCaseNext = false;
      }
      else {
        result[currPos++] = c;
      }
    }
    return new String(result, 0, currPos);
  }

  /**
   * Return an attribute name qualified by the given enclosing {@link Class}.
   * For example the attribute name '{@code foo}' qualified by {@link Class}
   * '{@code com.myapp.SomeClass}' would be '{@code com.myapp.SomeClass.foo}'
   */
  public static String getQualifiedAttributeName(Class<?> enclosingClass, String attributeName) {
    Assert.notNull(enclosingClass, "'enclosingClass' is required");
    Assert.notNull(attributeName, "'attributeName' is required");
    return enclosingClass.getName() + '.' + attributeName;
  }

  /**
   * Determine the class to use for naming a variable containing the given value.
   * <p>Will return the class of the given value, except when encountering a
   * JDK proxy, in which case it will determine the 'primary' interface
   * implemented by that proxy.
   *
   * @param value the value to check
   * @return the class to use for naming a variable
   */
  private static Class<?> getClassForValue(Object value) {
    Class<?> valueClass = value.getClass();
    if (Proxy.isProxyClass(valueClass)) {
      Class<?>[] ifcs = valueClass.getInterfaces();
      for (Class<?> ifc : ifcs) {
        if (!ClassUtils.isJavaLanguageInterface(ifc)) {
          return ifc;
        }
      }
    }
    else if (valueClass.getName().lastIndexOf('$') != -1 && valueClass.getDeclaringClass() == null) {
      // '$' in the class name but no inner class -
      // assuming it's a special subclass (e.g. by OpenJPA)
      valueClass = valueClass.getSuperclass();
    }
    return valueClass;
  }

  /**
   * Pluralize the given name.
   */
  private static String pluralize(String name) {
    return name + PLURAL_SUFFIX;
  }

  /**
   * Retrieve the {@code Class} of an element in the {@code Collection}.
   * The exact element for which the {@code Class} is retrieved will depend
   * on the concrete {@code Collection} implementation.
   */
  private static <E> E peekAhead(Collection<E> collection) {
    Iterator<E> it = collection.iterator();
    if (!it.hasNext()) {
      throw new IllegalStateException(
              "Unable to peek ahead in non-empty collection - no element found");
    }
    E value = it.next();
    if (value == null) {
      throw new IllegalStateException(
              "Unable to peek ahead in non-empty collection - only null element found");
    }
    return value;
  }

}
