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
package cn.taketoday.context.loader;

import java.util.Collection;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.factory.BeanDefinition;
import cn.taketoday.context.factory.BeanDefinitionRegistry;

/**
 * Create bean definition
 *
 * @author TODAY <br>
 * 2018-06-23 11:18:22
 */
public interface BeanDefinitionLoader {

  /**
   * Create a bean definition with given class type
   *
   * @param beanClass
   *         The bean type
   *
   * @return A new {@link BeanDefinition}
   */
  BeanDefinition createBeanDefinition(Class<?> beanClass);

  default BeanDefinition createBeanDefinition(String beanName, Class<?> beanClass) {
    return createBeanDefinition(beanName, beanClass, null);
  }

  /**
   * @since 3.0
   */
  BeanDefinition createBeanDefinition(
          String beanName, Class<?> beanClass, AnnotationAttributes attributes);

  /**
   * Get registered bean definition registry
   *
   * @return registry
   */
  BeanDefinitionRegistry getRegistry();

  /**
   * Load bean definitions with given bean collection.
   *
   * @param candidates
   *         candidates beans collection
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   */
  void loadBeanDefinitions(Collection<Class<?>> candidates) throws BeanDefinitionStoreException;

  /**
   * Load bean definition with given bean class.
   * <p>
   * The candidate bean class can't be abstract and must pass the condition which
   * {@link Conditional} is annotated.
   *
   * @param candidate
   *         Candidate bean class the class will be load
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @see #register(Class)
   */
  void loadBeanDefinition(Class<?> candidate) throws BeanDefinitionStoreException;

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
   * @param name
   *         Bean name
   * @param beanClass
   *         Bean class
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   */
  void loadBeanDefinition(String name, Class<?> beanClass) throws BeanDefinitionStoreException;

  /**
   * Load {@link BeanDefinition}s from input package locations
   *
   * <p>
   * {@link CandidateComponentScanner} will scan the classes from given package
   * locations. And register the {@link BeanDefinition}s using
   * loadBeanDefinition(Class)
   *
   * @param locations
   *         package locations
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @see #loadBeanDefinition(Class)
   * @since 2.1.7
   */
  void loadBeanDefinition(String... locations) throws BeanDefinitionStoreException;

  /**
   * Register bean definition with given class.
   * <p>
   * If candidate bean class isn't present {@link Component} will not register the
   * {@link BeanDefinition}
   * <p>
   * Otherwise will register a bean with given candidate bean class and indicate a
   * bean name from {@link BeanDefinition} metadata.
   *
   * @param candidate
   *         Candidate bean class
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @see #register(String, BeanDefinition)
   */
  void register(Class<?> candidate) throws BeanDefinitionStoreException;

  /**
   * Register bean definition with given name and {@link BeanDefinition}
   *
   * @param name
   *         Bean name
   * @param beanDefinition
   *         Bean definition instance
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   */
  void register(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;

  /**
   * Register bean definition with {@link BeanDefinition#getName()}
   *
   * @param beanDefinition
   *         Target {@link BeanDefinition}
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @since 2.1.6
   */
  default void register(BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
    register(beanDefinition.getName(), beanDefinition);
  }

  /**
   * @return ApplicationContext
   *
   * @since 3.0
   */
  ApplicationContext getApplicationContext();
}
