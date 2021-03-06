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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Scope;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;

/**
 * Bean factory
 *
 * @author TODAY <br>
 * 2018-06-23 11:22:26
 */
public interface BeanFactory {

  /**
   * If a bean name start with this its a {@link FactoryBean}
   */
  String FACTORY_BEAN_PREFIX = "$";

  char FACTORY_BEAN_PREFIX_CHAR = '$';

  /**
   * Find the bean with the given type
   *
   * @param name
   *         Bean name
   *
   * @return Bet bean instance, returns null if it doesn't exist .
   *
   * @throws BeansException
   *         Exception occurred when getting a named bean
   */
  Object getBean(String name) throws BeansException;

  /**
   * Find the bean with the given type,
   *
   * @param requiredType
   *         Bean type
   *
   * @return Get safe casted bean instance. returns null if it doesn't exist .
   */
  <T> T getBean(Class<T> requiredType);

  /**
   * Find the bean with the given name and cast to required type.
   *
   * @param name
   *         Bean name
   * @param requiredType
   *         Cast to required type
   *
   * @return get casted bean instance. returns null if it doesn't exist.
   */
  <T> T getBean(String name, Class<T> requiredType);

  /**
   * Is Singleton ?
   *
   * @param name
   *         Bean name
   *
   * @return If this bean is a singleton
   *
   * @throws NoSuchBeanDefinitionException
   *         If a bean does not exist
   */
  boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

  /**
   * Is Prototype ?
   *
   * @param name
   *         Bean name
   *
   * @return If this bean is a prototype
   *
   * @throws NoSuchBeanDefinitionException
   *         If a bean does not exist
   */
  boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

  /**
   * Get bean type
   *
   * @param name
   *         Bean name
   *
   * @return Target bean type
   *
   * @throws NoSuchBeanDefinitionException
   *         If a bean does not exist
   */
  Class<?> getType(String name) throws NoSuchBeanDefinitionException;

  /**
   * Get all bean name
   *
   * @param type
   *         Bean type
   *
   * @return A set of names with given type
   */
  Set<String> getAliases(Class<?> type);

  /**
   * Get the target class's name
   *
   * @param beanType
   *         bean type
   *
   * @return Get bane name
   *
   * @since 2.1.2
   */
  String getBeanName(Class<?> beanType) throws NoSuchBeanDefinitionException;

  /**
   * Get a set of beans with given type, this method must invoke after
   * {@link ApplicationContext#loadContext(String...)}
   *
   * @param requiredType
   *         Given bean type
   *
   * @return A set of beans with given type, never be {@code null}
   *
   * @since 2.1.2
   */
  default <T> List<T> getBeans(Class<T> requiredType) {
    return new ArrayList<>(getBeansOfType(requiredType).values());
  }

  /**
   * Return the bean instances that match the given object type (including
   * subclasses), judging from either bean definitions or the value of
   * {@code getBeanClass} in the case of FactoryBeans.
   * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
   * by other means than bean definitions.
   * <p>
   * Get a map of beans with given type, this method must invoke after
   * {@link ApplicationContext#loadContext(String...)}
   * </p>
   *
   * @param requiredType
   *         Given bean type
   *
   * @return A Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values, never be {@code null}
   *
   * @since 2.1.6
   */
  default <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
    return getBeansOfType(requiredType, true);
  }

  /**
   * Return the bean instances that match the given object type (including
   * subclasses), judging from either bean definitions or the value of
   * {@code getBeanClass} in the case of FactoryBeans.
   * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
   * by other means than bean definitions.
   * <p>
   * Get a map of beans with given type, this method must invoke after
   * {@link ApplicationContext#loadContext(String...)}
   * </p>
   *
   * @param requiredType
   *         the class or interface to match, or {@code null} for all concrete beans
   * @param includeNonSingletons
   *         whether to include prototype or scoped beans too
   *         or just singletons (also applies to FactoryBeans)
   *
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values
   *
   * @throws BeansException
   *         if a bean could not be created
   * @see FactoryBean#getBeanClass
   * @since 3.0
   */
  <T> Map<String, T> getBeansOfType(Class<T> requiredType, boolean includeNonSingletons);

  /**
   * Get all {@link BeanDefinition}s
   *
   * @return All {@link BeanDefinition}s
   *
   * @since 2.1.6
   */
  Map<String, BeanDefinition> getBeanDefinitions();

  /**
   * Get the bean with the given {@link BeanDefinition}
   *
   * @param def
   *         {@link BeanDefinition}
   *
   * @return Target {@link Object}
   *
   * @since 2.1.7
   */
  Object getBean(BeanDefinition def);

  /**
   * Get the bean with the given {@link BeanDefinition} and {@link Scope}
   *
   * @param def
   *         {@link BeanDefinition}
   * @param scope
   *         {@link Scope}
   *
   * @return Target {@link Object}
   *
   * @since 3.0
   */
  Object getScopeBean(BeanDefinition def, Scope scope);

  /**
   * Return a provider for the specified bean, allowing for lazy on-demand retrieval
   * of instances, including availability and uniqueness options.
   *
   * @param requiredType
   *         type the bean must match; can be an interface or superclass
   *
   * @return a corresponding provider handle
   *
   * @since 3.0
   */
  <T> ObjectSupplier<T> getBeanSupplier(Class<T> requiredType);

  /**
   * Return a provider for the specified bean, allowing for lazy on-demand retrieval
   * of instances, including availability and uniqueness options.
   *
   * @param def
   *         BeanDefinition
   *
   * @since 3.0
   */
  <T> ObjectSupplier<T> getBeanSupplier(BeanDefinition def);

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getBeanClass}
   * in the case of FactoryBeans.
   *
   * @param requiredType
   *         the class or interface to match, or {@code null} for all bean names
   *
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty array if none
   *
   * @see FactoryBean#getBeanClass()
   * @since 3.0
   */
  default String[] getBeanNamesOfType(Class<?> requiredType) {
    return getBeanNamesOfType(requiredType, true);
  }

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getBeanClass}
   * in the case of FactoryBeans.
   *
   * @param requiredType
   *         the class or interface to match, or {@code null} for all bean names
   * @param includeNonSingletons
   *         whether to include prototype or scoped beans too
   *         or just singletons (also applies to FactoryBeans)
   *
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty array if none
   *
   * @see FactoryBean#getBeanClass()
   * @since 3.0
   */
  String[] getBeanNamesOfType(Class<?> requiredType, boolean includeNonSingletons);

  /**
   * Find all beans which are annotated with the supplied {@link Annotation} type,
   * returning a List of bean instances.
   * <p>Note that this method considers objects created by FactoryBeans, which means
   * that FactoryBeans will get initialized in order to determine their object type.
   *
   * @param annotationType
   *         the type of annotation to look for
   *         (at class, interface or factory method level of the specified bean)
   *
   * @return a List with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values, never be {@code null}
   *
   * @throws BeansException
   *         if a bean could not be created
   * @see #getAnnotationOnBean
   * @since 3.0
   */
  @SuppressWarnings("unchecked")
  default <T> List<T> getAnnotatedBeans(Class<? extends Annotation> annotationType) throws BeansException {
    return (List<T>) new ArrayList<>(getBeansOfAnnotation(annotationType).values());
  }

  /**
   * Find all beans which are annotated with the supplied {@link Annotation} type,
   * returning a Map of bean names with corresponding bean instances.
   * <p>Note that this method considers objects created by FactoryBeans, which means
   * that FactoryBeans will get initialized in order to determine their object type.
   *
   * @param annotationType
   *         the type of annotation to look for
   *         (at class, interface or factory method level of the specified bean)
   *
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values, never be {@code null}
   *
   * @throws BeansException
   *         if a bean could not be created
   * @see #getAnnotationOnBean
   * @since 3.0
   */
  default Map<String, Object> getBeansOfAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
    return getBeansOfAnnotation(annotationType, true);
  }

  /**
   * Find all beans which are annotated with the supplied {@link Annotation} type,
   * returning a Map of bean names with corresponding bean instances.
   * <p>Note that this method considers objects created by FactoryBeans, which means
   * that FactoryBeans will get initialized in order to determine their object type.
   *
   * @param annotationType
   *         the type of annotation to look for
   *         (at class, interface or factory method level of the specified bean)
   * @param includeNonSingletons
   *         whether to include prototype or scoped beans too
   *         or just singletons (also applies to FactoryBeans)
   *
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values, never be {@code null}
   *
   * @throws BeansException
   *         if a bean could not be created
   * @see #getAnnotationOnBean
   * @since 3.0
   */
  Map<String, Object> getBeansOfAnnotation(Class<? extends Annotation> annotationType, boolean includeNonSingletons)
          throws BeansException;

  /**
   * Find an {@link Annotation} of {@code annotationType} on the specified bean,
   * traversing its interfaces and super classes if no annotation can be found on
   * the given class itself, as well as checking the bean's factory method (if any).
   *
   * @param beanName
   *         the name of the bean to look for annotations on
   * @param annotationType
   *         the type of annotation to look for
   *         (at class, interface or factory method level of the specified bean)
   *
   * @return the annotation of the given type if found, or {@code null} otherwise
   *
   * @throws NoSuchBeanDefinitionException
   *         if there is no bean with the given name
   * @see #getAnnotatedBeans
   * @since 3.0
   */
  <A extends Annotation> A getAnnotationOnBean(String beanName, Class<A> annotationType)
          throws NoSuchBeanDefinitionException;

}
