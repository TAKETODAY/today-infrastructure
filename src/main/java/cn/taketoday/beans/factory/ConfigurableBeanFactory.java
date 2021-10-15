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

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.FactoryBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Prototype;

/**
 * Configuration interface to be implemented by most bean factories. Provides
 * facilities to configure a bean factory, in addition to the bean factory
 * client methods in the {@link BeanFactory} interface.
 *
 * <p>This bean factory interface is not meant to be used in normal application
 * code: Stick to {@link BeanFactory} for typical needs. This extended interface
 * is just meant to allow for framework-internal plug'n'play and for special
 * access to bean factory configuration methods.
 *
 * @author TODAY 2018-11-14 19:40
 * @see BeanFactory
 */
public interface ConfigurableBeanFactory extends HierarchicalBeanFactory, SingletonBeanRegistry {

  /**
   * Remove bean with the given name
   *
   * @param name bean name
   */
  void removeBean(String name);

  /**
   * Remove bean with the given bean class
   *
   * @param beanClass bean type
   *
   * @since 4.0
   */
  void removeBean(Class<?> beanClass);

  /**
   * Destroy bean with given name
   * <p>Any exception that arises during destruction should be caught
   * and logged instead of propagated to the caller of this method.
   *
   * @param name the bean name
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
   * @param beanName the name of the bean definition
   * @param beanInstance the bean instance to destroy
   */
  void destroyBean(String beanName, Object beanInstance);

  /**
   * Destroy a bean with bean instance and bean definition
   *
   * @param beanInstance Bean instance
   * @param def Bean definition
   *
   * @since 3.0
   */
  void destroyBean(Object beanInstance, BeanDefinition def);

  /**
   * initialize bean with given name
   *
   * @param name bean name
   *
   * @since 1.2.0
   */
  void initialize(String name);

  /**
   * initialize bean definition
   *
   * @param beanDefinition bean definition
   *
   * @return initialized object
   *
   * @since 2.0.0
   */
  Object initialize(BeanDefinition beanDefinition);

  /**
   * Initialize singletons
   * <p>
   * Ensure that all non-lazy-init singletons are instantiated, also considering
   * {@link FactoryBean FactoryBeans}.
   * Typically invoked at the end of factory setup, if desired.
   * </p>
   *
   * @throws BeansException if one of the singleton beans could not be created.
   * Note: This may have left the factory with some beans already initialized!
   * @throws Throwable when could not initialize singletons
   * @since 2.1.2
   */
  void initializeSingletons() throws Throwable;

  /**
   * Add a {@link BeanPostProcessor}
   *
   * @param beanPostProcessor bean post processor instance
   *
   * @since 2.1.2
   */
  void addBeanPostProcessor(BeanPostProcessor beanPostProcessor);

  /**
   * Remove a {@link BeanPostProcessor}
   *
   * @param beanPostProcessor bean post processor instance
   *
   * @since 2.1.2
   */
  void removeBeanPostProcessor(BeanPostProcessor beanPostProcessor);

  /**
   * Enable full {@link Prototype Prototype}
   *
   * @see Prototypes
   * @since 3.0
   */
  void setFullPrototype(boolean fullPrototype);

  /**
   * Enable full {@link Prototype Prototype}'s
   * life cycle, default is not support
   *
   * @see Prototypes
   * @since 3.0
   */
  void setFullLifecycle(boolean fullLifecycle);

  /**
   * Register the given scope, backed by the given Scope implementation.
   *
   * @param name scope name
   * @param scope The backing Scope implementation
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
   * @param beanName the name of the scoped bean
   *
   * @since 2.1.7
   */
  void destroyScopedBean(String beanName);

  /**
   * Set the parent of this bean factory.
   * <p>Note that the parent cannot be changed: It should only be set outside
   * a constructor if it isn't available at the time of factory instantiation.
   *
   * @param parentBeanFactory the parent BeanFactory
   *
   * @throws IllegalStateException if this factory is already associated with
   * a parent BeanFactory
   * @see #getParentBeanFactory()
   * @since 4.0
   */
  void setParentBeanFactory(BeanFactory parentBeanFactory) throws IllegalStateException;

  /**
   * Register a special dependency type with corresponding autowired value.
   * <p>This is intended for factory/context references that are supposed
   * to be autowirable but are not defined as beans in the factory:
   * e.g. a dependency of type ApplicationContext resolved to the
   * ApplicationContext instance that the bean is living in.
   * <p>Note: There are no such default types registered in a plain BeanFactory,
   * not even for the BeanFactory interface itself.
   *
   * @param dependencyType the dependency type to register. This will typically
   * be a base interface such as BeanFactory, with extensions of it resolved
   * as well if declared as an autowiring dependency (e.g. BeanFactory),
   * as long as the given value actually implements the extended interface.
   * @param autowiredValue the corresponding autowired value. This may also be an
   * implementation of the {@link java.util.function.Supplier} interface,
   * which allows for lazy resolution of the actual target value.
   *
   * @since 4.0
   */
  void registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue);

  /**
   * Ensure that all non-lazy-init singletons are instantiated, also considering
   * {@link FactoryBean FactoryBeans}. Typically, invoked at the end of factory
   * setup, if desired.
   *
   * @throws BeansException if one of the singleton beans could not be created.
   * @since 4.0
   */
  void preInitialization() throws BeansException;

}
