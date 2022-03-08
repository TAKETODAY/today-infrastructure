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

package cn.taketoday.context.loader;

import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.config.Scope;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionCustomizer;
import cn.taketoday.lang.Component;
import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2021/10/26 11:49
 * @since 4.0
 */
public interface BeanDefinitionRegistrar {

  /**
   * Register a bean from the given bean class, optionally providing explicit
   * constructor arguments for consideration in the autowiring process.
   *
   * @param beanClass the class of the bean
   * @param constructorArgs custom argument values to be fed into constructor
   * resolution algorithm, resolving either all arguments or just
   * specific ones, with the rest to be resolved through regular autowiring
   * (may be {@code null} or empty)
   */
  default <T> void registerBean(Class<T> beanClass, Object... constructorArgs) {
    registerBean(null, beanClass, constructorArgs);
  }

  /**
   * Register a bean from the given bean class, optionally providing explicit
   * constructor arguments for consideration in the autowiring process.
   *
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean
   * @param constructorArgs custom argument values to be fed into constructor
   * resolution algorithm, resolving either all arguments or just
   * specific ones, with the rest to be resolved through regular autowiring
   * (may be {@code null} or empty)
   */
  <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass, Object... constructorArgs);

  /**
   * Register a bean from the given bean class, optionally customizing its
   * bean definition metadata (typically declared as a lambda expression).
   *
   * @param beanClass the class of the bean (resolving a public constructor
   * to be autowired, possibly simply the default constructor)
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  default <T> void registerBean(Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
    registerBean(null, beanClass, null, customizers);
  }

  /**
   * Register a bean from the given bean class, optionally customizing its
   * bean definition metadata (typically declared as a lambda expression).
   *
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean (resolving a public constructor
   * to be autowired, possibly simply the default constructor)
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  default <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass, BeanDefinitionCustomizer... customizers) {
    registerBean(beanName, beanClass, null, customizers);
  }

  /**
   * Register a bean from the given bean class, using the given supplier for
   * obtaining a new instance (typically declared as a lambda expression or
   * method reference), optionally customizing its bean definition metadata
   * (again typically declared as a lambda expression).
   *
   * @param beanClass the class of the bean
   * @param supplier a callback for creating an instance of the bean
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   * @see #registerBean(String, Class, Supplier, BeanDefinitionCustomizer...)
   */
  default <T> void registerBean(
          Class<T> beanClass, Supplier<T> supplier, @Nullable BeanDefinitionCustomizer... customizers) {
    registerBean(null, beanClass, supplier, customizers);
  }

  /**
   * Register a bean from the given bean class, using the given supplier for
   * obtaining a new instance (typically declared as a lambda expression or
   * method reference), optionally customizing its bean definition metadata
   * (again typically declared as a lambda expression).
   * <p>This method can be overridden to adapt the registration mechanism for
   * all {@code registerBean} methods (since they all delegate to this one).
   *
   * @param beanName the name of the bean (may be {@code null})
   * @param beanClass the class of the bean
   * @param supplier a callback for creating an instance of the bean (in case
   * of {@code null}, resolving a public constructor to be autowired instead)
   * @param customizers one or more callbacks for customizing the factory's
   * {@link BeanDefinition}, e.g. setting a lazy-init or primary flag
   */
  <T> void registerBean(
          @Nullable String beanName, Class<T> beanClass,
          @Nullable Supplier<T> supplier, @Nullable BeanDefinitionCustomizer... customizers);

  //

  /**
   * register a bean with the given bean class
   *
   * @since 3.0
   */
  default void registerBean(Class<?> clazz) {
    registerBean(null, clazz, null, (BeanDefinitionCustomizer[]) null);
  }

  default void registerBean(Class<?>... candidates) {
    for (Class<?> candidate : candidates) {
      registerBean(candidate);
    }
  }

  /**
   * @since 4.0
   */
  default void registerBean(Set<Class<?>> candidates) {
    for (Class<?> candidate : candidates) {
      registerBean(candidate);
    }
  }

  /**
   * Load bean definition with given bean class and bean name.
   * <p>
   * If the provided bean class annotated {@link Component} annotation will
   * register beans with given {@link Component} metadata.
   * <p>
   * Otherwise register a bean will given default metadata: use the default bean
   * name creator create the default bean name, use default bean scope
   * {@link Scope#SINGLETON} , empty initialize method ,empty property value and
   * empty destroy method.
   *
   * @param name Bean name
   * @param beanClass Bean class
   * @throws BeanDefinitionStoreException If BeanDefinition could not be store
   */
  default void registerBean(String name, Class<?> beanClass) {
    registerBean(name, beanClass, null, (BeanDefinitionCustomizer[]) null);
  }

  /**
   * Register a bean with the given name and bean instance
   *
   * @param name bean name (must not be null)
   * @param obj bean instance (must not be null)
   */
  void registerSingleton(String name, Object obj);

  /**
   * Register a bean with the bean instance
   * <p>
   *
   * register a BeanDefinition and its' singleton
   *
   * @param obj bean instance
   * @throws BeanDefinitionStoreException If can't store a bean
   */
  void registerSingleton(Object obj);

  //---------------------------------------------------------------------
  // Manual prototype
  //---------------------------------------------------------------------

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz bean class
   * @param supplier bean instance supplier
   * @throws BeanDefinitionStoreException If can't store a bean
   */
  default <T> void registerBean(Class<T> clazz, @Nullable Supplier<T> supplier) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, false);
  }

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz bean class
   * @param supplier bean instance supplier
   * @param prototype register as prototype?
   * @throws BeanDefinitionStoreException If can't store a bean
   */
  default <T> void registerBean(
          Class<T> clazz, @Nullable Supplier<T> supplier, boolean prototype) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, prototype, true);
  }

  /**
   * Register a bean with the given type and instance supplier
   *
   * @param clazz bean class
   * @param supplier bean instance supplier
   * @param prototype register as prototype?
   * @param ignoreAnnotation ignore {@link Component} scanning
   * @throws BeanDefinitionStoreException If can't store a bean
   */
  <T> void registerBean(
          Class<T> clazz, @Nullable Supplier<T> supplier, boolean prototype, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException;

}
