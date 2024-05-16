/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */
package cn.taketoday.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import cn.taketoday.lang.Nullable;

/**
 * Exception thrown when instantiation of a bean failed. Carries the offending
 * bean class.
 *
 * @author Juergen Hoeller
 * @author TODAY 2020-02-19 21:36
 * @since 2.17
 */
public class BeanInstantiationException extends FatalBeanException {

  private final Class<?> beanClass;

  private final Method constructingMethod;

  private final Constructor<?> constructor;

  public BeanInstantiationException(String msg, Throwable cause) {
    super(msg, cause);
    this.beanClass = null;
    this.constructor = null;
    this.constructingMethod = null;
  }

  public BeanInstantiationException(Class<?> beanClass, String msg) {
    this(beanClass, msg, null);
  }

  public BeanInstantiationException(Class<?> beanClass, String msg, Throwable cause) {
    super("Failed to instantiate [%s]: %s".formatted(beanClass.getName(), msg), cause);
    this.beanClass = beanClass;
    this.constructor = null;
    this.constructingMethod = null;
  }

  public BeanInstantiationException(Constructor<?> constructor, String msg, Throwable cause) {
    super("Failed to instantiate [%s]: %s".formatted(constructor.getDeclaringClass().getName(), msg), cause);
    this.beanClass = constructor.getDeclaringClass();
    this.constructor = constructor;
    this.constructingMethod = null;
  }

  public BeanInstantiationException(Method constructingMethod, String msg, Throwable cause) {
    super("Failed to instantiate [%s]: %s".formatted(constructingMethod.getReturnType().getName(), msg), cause);
    this.beanClass = constructingMethod.getReturnType();
    this.constructor = null;
    this.constructingMethod = constructingMethod;
  }

  /**
   * Return the offending bean class (never {@code null}).
   *
   * @return the class that was to be instantiated
   */
  @Nullable
  public Class<?> getBeanClass() {
    return this.beanClass;
  }

  /**
   * Return the offending constructor, if known.
   *
   * @return the constructor in use, or {@code null} in case of a factory method
   * or in case of default instantiation
   */
  @Nullable
  public Constructor<?> getConstructor() {
    return this.constructor;
  }

  /**
   * Return the delegate for bean construction purposes, if known.
   *
   * @return the method in use (typically a static factory method), or
   * {@code null} in case of constructor-based instantiation
   */
  @Nullable
  public Method getConstructingMethod() {
    return this.constructingMethod;
  }

}
