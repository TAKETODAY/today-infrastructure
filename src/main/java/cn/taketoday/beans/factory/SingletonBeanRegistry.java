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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory;

import java.util.Map;

import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.lang.Nullable;

/**
 * Interface that defines a registry for shared bean instances.
 * Can be implemented by {@link BeanFactory} implementations in
 * order to expose their singleton management facility in a uniform manner.
 *
 * @author TODAY 2018-11-14 19:47
 * @since 2.0.1
 */
public interface SingletonBeanRegistry {

  /**
   * Register the given existing object as singleton in the bean registry,
   * under the given bean name.
   * <p>The given instance is supposed to be fully initialized; the registry
   * will not perform any initialization callbacks (in particular, it won't
   * call InitializingBean's {@code afterPropertiesSet} method).
   * The given instance will not receive any destruction callbacks
   * (like DisposableBean's {@code destroy} method) either.
   * <p>When running within a full BeanFactory: <b>Register a bean definition
   * instead of an existing instance if your bean is supposed to receive
   * initialization and/or destruction callbacks.</b>
   * <p>Typically invoked during registry configuration, but can also be used
   * for runtime registration of singletons. As a consequence, a registry
   * implementation should synchronize singleton access; it will have to do
   * this anyway if it supports a BeanFactory's lazy initialization of singletons.
   *
   * @param name the name of the bean
   * @param singletonObject the existing singleton object
   * @see InitializingBean#afterPropertiesSet
   * @see DisposableBean#destroy
   * @see BeanDefinitionRegistry#registerBeanDefinition
   */
  void registerSingleton(String name, Object singletonObject);

  /**
   * Register a singleton
   * <p>
   * sub-classes provide a strategy to create bean name
   * ,default is use {@link cn.taketoday.util.ClassUtils#getShortName(Class)}
   * </p>
   *
   * @param bean bean instance
   * @see BeanDefinitionBuilder#defaultBeanName(Class)
   * @since 2.1.2
   */
  void registerSingleton(Object bean);

  /**
   * Get all instances Map
   *
   * @return the map of singletons
   */
  Map<String, Object> getSingletons();

  /**
   * Return the (raw) singleton object registered under the given name.
   * <p>Only checks already instantiated singletons; does not return an Object
   * for singleton bean definitions which have not been instantiated yet.
   * <p>The main purpose of this method is to access manually registered singletons
   * (see {@link #registerSingleton}). Can also be used to access a singleton
   * defined by a bean definition that already been created, in a raw fashion.
   *
   * @param name the name of the bean to look for
   * @return the registered singleton object, or {@code null} if none found
   */
  @Nullable
  Object getSingleton(String name);

  /**
   * Return the (raw) singleton object registered under the given name.
   * <p>
   * singleton must be instance of required type
   * </p>
   *
   * @param name the name of the bean to look for
   * @param requiredType required type
   * @param <T> required type
   * @return the registered singleton object, or {@code null} if none found
   */
  default <T> T getSingleton(String name, Class<T> requiredType) {
    Object singleton = getSingleton(name);
    if (requiredType.isInstance(singleton)) {
      return requiredType.cast(singleton);
    }
    return null;
  }

  /**
   * Get singleton objects
   *
   * @param requiredType required type
   * @param <T> required type
   * @return singleton object
   */
  <T> T getSingleton(Class<T> requiredType);

  /**
   * remove a singleton with given name
   *
   * @param name bean name
   */
  void removeSingleton(String name);

  /**
   * contains instance with given name?
   *
   * @param name bean name
   * @return if contains singleton
   */
  boolean containsSingleton(String name);

  /**
   * Return the number of singleton beans registered in this registry.
   * <p>Only checks already instantiated singletons; does not count
   * singleton bean definitions which have not been instantiated yet.
   * <p>The main purpose of this method is to check manually registered singletons
   * (see {@link #registerSingleton}). Can also be used to count the number of
   * singletons defined by a bean definition that have already been created.
   *
   * @return the number of singleton beans
   * @see #registerSingleton
   * @see BeanDefinitionRegistry#getBeanDefinitionCount
   * @see cn.taketoday.beans.factory.BeanFactory#getBeanDefinitionCount
   * @since 4.0
   */
  int getSingletonCount();

  /**
   * Return the names of singleton beans registered in this registry.
   * <p>Only checks already instantiated singletons; does not return names
   * for singleton bean definitions which have not been instantiated yet.
   * <p>The main purpose of this method is to check manually registered singletons
   * (see {@link #registerSingleton}). Can also be used to check which singletons
   * defined by a bean definition have already been created.
   *
   * @return the list of names as a String array (never {@code null})
   * @see #registerSingleton
   * @see BeanDefinitionRegistry#getBeanDefinitionNames
   * @see cn.taketoday.beans.factory.BeanFactory#getBeanDefinitionNames
   * @since 4.0
   */
  String[] getSingletonNames();

  /**
   * Return the singleton mutex used by this registry (for external collaborators).
   *
   * @return the mutex object (never {@code null})
   * @since 4.0
   */
  Object getSingletonMutex();

}
