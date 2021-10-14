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

package cn.taketoday.beans.support;

import cn.taketoday.beans.ArgumentsResolver;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanInstantiationException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationContextHolder;
import cn.taketoday.core.ConstructorNotFoundException;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author TODAY 2021/8/22 21:51
 * @since 4.0
 */
public abstract class BeanUtils {

  /**
   * Get instance with bean class use default {@link Constructor}
   *
   * @param beanClass
   *         bean class
   *
   * @return the instance of target class
   *
   * @throws BeanInstantiationException
   *         if any reflective operation exception occurred
   * @see ApplicationContextHolder#getLastStartupContext()
   * @since 2.1.2
   */
  public static <T> T newInstance(Class<T> beanClass) {
    // maybe there has already a bean-factory ContextUtils#getLastStartupContext
    Constructor<T> constructor = obtainConstructor(beanClass);
    if (constructor.getParameterCount() == 0) {
      return newInstance(constructor, null);
    }
    ApplicationContext lastStartupContext = ApplicationContextHolder.getLastStartupContext();
    ArgumentsResolver argumentsResolver = ArgumentsResolver.getOrShared(lastStartupContext);
    Object[] parameter = argumentsResolver.resolve(constructor, lastStartupContext, null);
    return newInstance(constructor, parameter);
  }

  /**
   * Get instance with bean class
   *
   * @param beanClassName
   *         bean class name string
   *
   * @return the instance of target class
   *
   * @throws ClassNotFoundException
   *         If the class was not found
   * @see #obtainConstructor(Class)
   * @since 2.1.2
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(String beanClassName) throws ClassNotFoundException {
    return (T) newInstance(ClassUtils.forName(beanClassName));
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass
   *         target bean class
   * @param beanFactory
   *         bean factory
   *
   * @return bean class 's instance
   *
   * @throws BeanInstantiationException
   *         if any reflective operation exception occurred
   * @see #obtainConstructor(Class)
   */
  public static <T> T newInstance(final Class<T> beanClass, final BeanFactory beanFactory) {
    return newInstance(beanClass, beanFactory, null);
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass
   *         target bean class
   * @param providedArgs
   *         User provided arguments
   *
   * @return bean class 's instance
   *
   * @throws BeanInstantiationException
   *         if any reflective operation exception occurred
   * @see #obtainConstructor(Class)
   */
  public static <T> T newInstance(
          Class<T> beanClass, @Nullable BeanFactory beanFactory, @Nullable Object[] providedArgs) {
    Constructor<T> constructor = obtainConstructor(beanClass);
    if (constructor.getParameterCount() == 0) {
      return newInstance(constructor, null);
    }
    ArgumentsResolver argumentsResolver = ArgumentsResolver.getOrShared(beanFactory);
    Object[] parameter = argumentsResolver.resolve(constructor, beanFactory, providedArgs);
    return newInstance(constructor, parameter);
  }

  /**
   * use obtainConstructor to get {@link Constructor} to create bean instance.
   *
   * @param beanClass
   *         target bean class
   * @param providedArgs
   *         User provided arguments
   *
   * @return bean class 's instance
   *
   * @throws BeanInstantiationException
   *         if any reflective operation exception occurred
   * @see #obtainConstructor(Class)
   * @since 4.0
   */
  public static <T> T newInstance(
          Class<T> beanClass, ArgumentsResolver argumentsResolver,
          @Nullable BeanFactory beanFactory, @Nullable Object[] providedArgs) {
    Assert.notNull(argumentsResolver, "ArgumentsResolver must not be null");
    Constructor<T> constructor = obtainConstructor(beanClass);
    Object[] parameter = argumentsResolver.resolve(constructor, beanFactory, providedArgs);
    return newInstance(constructor, parameter);
  }

  /**
   * @throws BeanInstantiationException
   *         cannot instantiate a bean
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  public static <T> T newInstance(BeanInstantiator constructor, @Nullable Object[] parameter) {
    return (T) constructor.instantiate(parameter);
  }

  /**
   * use Constructor to create bean instance
   *
   * @param constructor
   *         java reflect Constructor
   * @param parameter
   *         initargs
   * @param <T>
   *         target bean type
   *
   * @return instance create from constructor
   *
   * @see Constructor#newInstance(Object...)
   */
  public static <T> T newInstance(Constructor<T> constructor, @Nullable Object[] parameter) {
    try {
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
   * @param <T>
   *         Target type
   * @param beanClass
   *         target bean class
   *
   * @return Suitable constructor
   *
   * @throws ConstructorNotFoundException
   *         If there is no suitable constructor
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
   * @param <T>
   *         Target type
   * @param beanClass
   *         target bean class
   *
   * @return Suitable constructor If there isn't a suitable {@link Constructor}
   * returns null
   *
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

}
