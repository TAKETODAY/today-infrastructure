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
import java.util.List;

import cn.taketoday.beans.Component;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Conditional;
import cn.taketoday.context.Scope;
import cn.taketoday.core.AnnotationAttributes;
import cn.taketoday.core.Nullable;

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
   * @return returns a new BeanDefinition
   * if {@link cn.taketoday.beans.factory.StandardBeanFactory#transformBeanDefinition} transformed,
   * If returns {@code null} or empty list indicates that none register to the registry
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @see #register(String, BeanDefinition)
   */
  List<BeanDefinition> register(Class<?> candidate) throws BeanDefinitionStoreException;

  /**
   * Register bean definition with given name and {@link BeanDefinition}
   *
   * @param name
   *         Bean name
   * @param beanDefinition
   *         Bean definition instance
   *
   * @return returns a new BeanDefinition
   * if {@link cn.taketoday.beans.factory.StandardBeanFactory#transformBeanDefinition} transformed,
   * If returns {@code null} indicates that none register to the registry
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   */
  BeanDefinition register(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException;

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
  default BeanDefinition register(BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
    return register(beanDefinition.getName(), beanDefinition);
  }

  /**
   * @return ApplicationContext
   *
   * @since 3.0
   */
  ApplicationContext getApplicationContext();

  /**
   * Load bean definitions with given bean collection.
   *
   * @param candidates
   *         candidates beans collection
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   */
  void load(Collection<Class<?>> candidates) throws BeanDefinitionStoreException;

  /**
   * Load bean definition with given bean class.
   * <p>
   * The candidate bean class can't be abstract and must pass the condition which
   * {@link Conditional} is annotated.
   *
   * @param candidate
   *         Candidate bean class the class will be load
   *
   * @return returns a new BeanDefinition
   * if {@link cn.taketoday.beans.factory.StandardBeanFactory#transformBeanDefinition} transformed,
   * If returns {@code null} or empty list indicates that none register to the registry
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @see #register(Class)
   */
  @Nullable
  List<BeanDefinition> load(Class<?> candidate) throws BeanDefinitionStoreException;

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
   * @return returns a new BeanDefinition
   * if {@link cn.taketoday.beans.factory.StandardBeanFactory#transformBeanDefinition} transformed,
   * If returns {@code null} or empty list indicates that none register to the registry
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @since 4.0
   */
  List<BeanDefinition> load(String name, Class<?> beanClass) throws BeanDefinitionStoreException;

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
   *         default bean name
   * @param beanClass
   *         Bean class
   * @param ignoreAnnotation
   *         ignore {@link Component} scanning
   *
   * @return returns a new BeanDefinition
   * if {@link cn.taketoday.beans.factory.StandardBeanFactory#transformBeanDefinition} transformed,
   * If returns {@code null} or empty list indicates that none register to the registry
   *
   * @throws BeanDefinitionStoreException
   *         If BeanDefinition could not be store
   * @since 4.0
   */
  List<BeanDefinition> load(String name, Class<?> beanClass, boolean ignoreAnnotation)
          throws BeanDefinitionStoreException;

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
   * @see #load(Class)
   * @since 4.0
   */
  void load(String... locations) throws BeanDefinitionStoreException;

}
