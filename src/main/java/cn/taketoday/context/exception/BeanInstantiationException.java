/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
package cn.taketoday.context.exception;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import cn.taketoday.context.factory.BeanDefinition;

/**
 * Exception thrown when instantiation of a bean failed. Carries the offending
 * bean class.
 *
 * @author Juergen Hoeller
 * @author TODAY <br>
 *         2020-02-19 21:36
 * @since 2.17
 */
public class BeanInstantiationException extends ContextException {

  private static final long serialVersionUID = 1L;

  private final Class<?> beanClass;
  private final Method constructingMethod;
  private final Constructor<?> constructor;

  public BeanInstantiationException(BeanDefinition def, String msg) {
    this(def.getBeanClass(), msg, null);
  }

  public BeanInstantiationException(Class<?> beanClass, String msg) {
    this(beanClass, msg, null);
  }

  public BeanInstantiationException(Class<?> beanClass, String msg, Throwable cause) {
    super("Failed to instantiate [" + beanClass.getName() + "]: " + msg, cause);
    this.beanClass = beanClass;
    this.constructor = null;
    this.constructingMethod = null;
  }

  public BeanInstantiationException(Constructor<?> constructor, String msg, Throwable cause) {
    super("Failed to instantiate [" + constructor.getDeclaringClass().getName() + "]: " + msg, cause);
    this.beanClass = constructor.getDeclaringClass();
    this.constructor = constructor;
    this.constructingMethod = null;
  }

  public BeanInstantiationException(Method constructingMethod, String msg, Throwable cause) {
    super("Failed to instantiate [" + constructingMethod.getReturnType().getName() + "]: " + msg, cause);
    this.beanClass = constructingMethod.getReturnType();
    this.constructor = null;
    this.constructingMethod = constructingMethod;
  }

  /**
   * Return the offending bean class (never {@code null}).
   *
   * @return the class that was to be instantiated
   */
  public Class<?> getBeanClass() {
    return this.beanClass;
  }

  /**
   * Return the offending constructor, if known.
   *
   * @return the constructor in use, or {@code null} in case of a factory method
   *         or in case of default instantiation
   */
  public Constructor<?> getConstructor() {
    return this.constructor;
  }

  /**
   * Return the delegate for bean construction purposes, if known.
   *
   * @return the method in use (typically a static factory method), or
   *         {@code null} in case of constructor-based instantiation
   */
  public Method getConstructingMethod() {
    return this.constructingMethod;
  }

}
