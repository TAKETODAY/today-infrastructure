/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.beans.factory.support;

import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.config.BeanDefinition;
import infra.core.AliasRegistry;
import infra.lang.Nullable;

/**
 * Interface for registries that hold bean definitions, for example BeanDefinition.
 * Typically implemented by BeanFactories that internally work with the BeanDefinition hierarchy.
 *
 * <p>This is the only interface in Infra bean factory packages that encapsulates
 * <i>registration</i> of bean definitions. The standard BeanFactory interfaces
 * only cover access to a <i>fully configured factory instance</i>.
 *
 * <p>Infra bean definition readers expect to work on an implementation of this
 * interface. Known implementors within the Framework core are StandardBeanFactory
 * and DefaultApplicationContext.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see BeanDefinition
 * @see AbstractBeanDefinition
 * @see RootBeanDefinition
 * @see ChildBeanDefinition
 * @since 2018-07-08 19:56:53
 */
public interface BeanDefinitionRegistry extends AliasRegistry {

  /**
   * Register a new bean definition with this registry.
   * Must support RootBeanDefinition and ChildBeanDefinition.
   *
   * @param beanName the name of the bean instance to register
   * @param beanDefinition definition of the bean instance to register
   * @throws BeanDefinitionStoreException if the BeanDefinition is invalid
   * @throws BeanDefinitionOverrideException if there is already a BeanDefinition
   * for the specified bean name and we are not allowed to override it
   * @see GenericBeanDefinition
   * @see RootBeanDefinition
   * @see ChildBeanDefinition
   * @since 1.2.0
   */
  void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
          throws BeanDefinitionStoreException;

  /**
   * Remove the BeanDefinition for the given name.
   *
   * @param beanName the name of the bean instance to register
   * @throws NoSuchBeanDefinitionException if there is no such bean definition
   */
  void removeBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

  /**
   * Return the BeanDefinition for the given bean name.
   *
   * @param beanName name of the bean to find a definition for
   * @return the BeanDefinition for the given name (never {@code null})
   * @throws NoSuchBeanDefinitionException if there is no such bean definition
   */
  BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

  /**
   * Return the BeanDefinition for the given bean class.
   *
   * @param beanClass Bean definition bean class
   */
  @Nullable
  BeanDefinition getBeanDefinition(Class<?> beanClass);

  /**
   * Check if this registry contains a bean definition with the given name.
   *
   * @param beanName The name of the bean to look for
   * @return If this registry contains a bean definition with the given name
   */
  boolean containsBeanDefinition(String beanName);

  /**
   * Whether there is a bean with the given name and type.
   *
   * @param beanName The name of the bean to look for
   * @param type Bean type
   * @return If exist a bean with given name and type
   * @since 2.1.7
   */
  default boolean containsBeanDefinition(String beanName, Class<?> type) {
    return containsBeanDefinition(beanName) && containsBeanDefinition(type);
  }

  /**
   * Whether there is a bean with the given type.
   *
   * @param type The bean class of the bean to look for
   * @return If exist a bean with given type
   */
  boolean containsBeanDefinition(Class<?> type);

  /**
   * Whether there is a bean with the given type.
   *
   * @param type Target type
   * @param equals Must equals type
   * @return If exist a bean with given type
   */
  boolean containsBeanDefinition(Class<?> type, boolean equals);

  /**
   * Return the names of all beans defined in this registry.
   *
   * @return the names of all beans defined in this registry, or an empty array if
   * none defined
   */
  String[] getBeanDefinitionNames();

  /**
   * Return the number of beans defined in the registry.
   *
   * @return the number of beans defined in the registry
   */
  int getBeanDefinitionCount();

  /**
   * Determine whether the given bean name is already in use within this registry,
   * i.e. whether there is a local bean or alias registered under this name.
   *
   * @param beanName the name to check
   * @return whether the given bean name is already in use
   * @since 4.0
   */
  boolean isBeanNameInUse(String beanName);

  /**
   * Return whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   *
   * @since 4.0
   */
  boolean isAllowBeanDefinitionOverriding();

  /**
   * Determine whether the bean definition for the given name is overridable,
   * i.e. whether {@link #registerBeanDefinition} would successfully return
   * against an existing definition of the same name.
   * <p>The default implementation returns {@code true}.
   *
   * @param beanName the name to check
   * @return whether the definition for the given bean name is overridable
   * @since 4.0
   */
  default boolean isBeanDefinitionOverridable(String beanName) {
    return isAllowBeanDefinitionOverriding();
  }

}
