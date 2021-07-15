/**
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.factory;

import java.util.Set;
import java.util.function.Supplier;

import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Scope;
import cn.taketoday.context.exception.BeanDefinitionStoreException;

/**
 * @author TODAY <br>
 * 2018-11-14 19:40
 */
public interface ConfigurableBeanFactory
        extends BeanFactory, SingletonBeanRegistry, BeanDefinitionRegistry {

  /**
   * Register a bean with the given name and bean definition
   *
   * @param beanDefinition
   *         Bean definition
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 1.2.0
   */
  void registerBean(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;

  /**
   * Register a bean with the given bean definition
   *
   * @param beanDefinition
   *         Bean definition
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 2.1.7
   */
  default void registerBean(BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
    registerBean(beanDefinition.getName(), beanDefinition);
  }

  /**
   * Remove bean with the given name
   *
   * @param name
   *         bean name
   */
  void removeBean(String name);

  /**
   * Register a bean with the given name and type
   *
   * @param name
   *         bean name
   * @param clazz
   *         bean class
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  @Override
  void registerBean(String name, Class<?> clazz) throws BeanDefinitionStoreException;

  /**
   * Register a bean with the given name and bean instance
   *
   * @param name
   *         bean name (must not be null)
   * @param obj
   *         bean instance (must not be null)
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  void registerBean(String name, Object obj) throws BeanDefinitionStoreException;

  /**
   * Register a bean with the bean instance
   * <p>
   * Use the {@link BeanNameCreator} to create a bean name
   *
   * @param obj
   *         bean instance
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  void registerBean(Object obj) throws BeanDefinitionStoreException;

  /**
   * Register a bean with the given type
   * <p>
   * This method will use {@link BeanNameCreator} create a bean name and register
   * it
   *
   * @param clazz
   *         bean class
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  @Override
  void registerBean(Class<?> clazz) throws BeanDefinitionStoreException;

  /**
   * Register a bean with the given types
   *
   * @param classes
   *         bean classes
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   */
  void registerBean(Set<Class<?>> classes) throws BeanDefinitionStoreException;

  /**
   * Register a bean with the given type and instance supplier
   * <p>
   * This method will use {@link BeanNameCreator} create a bean name and register
   * it
   * <p>
   * default register as singleton
   * </p>
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 3.0.6
   */
  default <T> void registerBean(Class<T> clazz, Supplier<T> supplier) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, false);
  }

  /**
   * Register a bean with the given type and instance supplier
   * <p>
   * This method will use {@link BeanNameCreator} create a bean name and register
   * it
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   * @param prototype
   *         register as prototype?
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 3.0.6
   */
  default <T> void registerBean(
          Class<T> clazz, Supplier<T> supplier, boolean prototype) throws BeanDefinitionStoreException {
    registerBean(clazz, supplier, prototype, true);
  }

  /**
   * Register a bean with the given type and instance supplier
   * <p>
   * This method will use {@link BeanNameCreator} create a bean name and register
   * it
   *
   * @param clazz
   *         bean class
   * @param supplier
   *         bean instance supplier
   * @param prototype
   *         register as prototype?
   * @param ignoreAnnotation
   *         ignore {@link cn.taketoday.context.annotation.Component} scanning
   *
   * @throws BeanDefinitionStoreException
   *         If can't store a bean
   * @since 3.0.6
   */
  <T> void registerBean(Class<T> clazz, Supplier<T> supplier, boolean prototype, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException;

  /**
   * Destroy bean with given name
   * <p>Any exception that arises during destruction should be caught
   * and logged instead of propagated to the caller of this method.
   *
   * @param name
   *         the bean name
   *
   * @since 2.1.0
   */
  void destroyBean(String name);

  /**
   * Destroy the given bean instance (usually a prototype instance
   * obtained from this factory) according to its bean definition.
   * <p>Any exception that arises during destruction should be caught
   * and logged instead of propagated to the caller of this method.
   *
   * @param beanName
   *         the name of the bean definition
   * @param beanInstance
   *         the bean instance to destroy
   */
  void destroyBean(String beanName, Object beanInstance);

  /**
   * Destroy a bean with bean instance and bean definition
   *
   * @param beanInstance
   *         Bean instance
   * @param def
   *         Bean definition
   *
   * @since 3.0
   */
  void destroyBean(Object beanInstance, BeanDefinition def);

  /**
   * Refresh bean with given name, and publish
   * {@link cn.taketoday.context.event.ObjectRefreshedEvent ObjectRefreshedEvent}.
   *
   * @param name
   *         bean name
   *
   * @since 1.2.0
   */
  void refresh(String name);

  /**
   * Refresh bean definition, and publish
   * {@link cn.taketoday.context.event.ObjectRefreshedEvent ObjectRefreshedEvent}.
   *
   * @param beanDefinition
   *         bean definition
   *
   * @return initialized object
   *
   * @since 2.0.0
   */
  Object refresh(BeanDefinition beanDefinition);

  /**
   * Initialize singletons
   * <p>
   * Ensure that all non-lazy-init singletons are instantiated, also considering
   * {@link FactoryBean FactoryBeans}.
   * Typically invoked at the end of factory setup, if desired.
   * </p>
   *
   * @throws BeansException
   *         if one of the singleton beans could not be created.
   *         Note: This may have left the factory with some beans already initialized!
   * @throws Throwable
   *         when could not initialize singletons
   * @since 2.1.2
   */
  void initializeSingletons() throws Throwable;

  /**
   * Add a {@link BeanPostProcessor}
   *
   * @param beanPostProcessor
   *         bean post processor instance
   *
   * @since 2.1.2
   */
  void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

  /**
   * Remove a {@link BeanPostProcessor}
   *
   * @param beanPostProcessor
   *         bean post processor instance
   *
   * @since 2.1.2
   */
  void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor);

  /**
   * Enable full {@link cn.taketoday.context.annotation.Prototype Prototype}
   *
   * @see #setFullPrototype(boolean)
   * @since 2.1.6
   */
  @Deprecated
  void enableFullPrototype();

  /**
   * Enable full {@link cn.taketoday.context.annotation.Prototype Prototype}'s
   * life cycle, default is not support
   *
   * @see #setFullLifecycle(boolean)
   * @since 2.1.6
   */
  @Deprecated
  void enableFullLifecycle();

  /**
   * Enable full {@link cn.taketoday.context.annotation.Prototype Prototype}
   *
   * @see Prototypes
   * @since 3.0
   */
  void setFullPrototype(boolean fullPrototype);

  /**
   * Enable full {@link cn.taketoday.context.annotation.Prototype Prototype}'s
   * life cycle, default is not support
   *
   * @see Prototypes
   * @since 3.0
   */
  void setFullLifecycle(boolean fullLifecycle);

  /**
   * Register the given scope, backed by the given Scope implementation.
   *
   * @param name
   *         scope name
   * @param scope
   *         The backing Scope implementation
   *
   * @since 2.1.7
   */
  void registerScope(String name, Scope scope);

  /**
   * Destroy the specified scoped bean in the current target scope, if any.
   * <p>
   * Any exception that arises during destruction should be caught and logged
   * instead of propagated to the caller of this method.
   *
   * @param beanName
   *         the name of the scoped bean
   *
   * @since 2.1.7
   */
  void destroyScopedBean(String beanName);
}
