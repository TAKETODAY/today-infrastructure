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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.ArgumentsResolverProvider;
import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.FactoryBean;
import cn.taketoday.lang.Nullable;
import cn.taketoday.core.ResolvableType;

/**
 * Bean factory
 * <p>
 * this factory that can enumerate all their bean instances, rather than attempting bean lookup
 * by name one by one as requested by clients.
 *
 * @author TODAY <br>
 * 2018-06-23 11:22:26
 */
public interface BeanFactory extends ArgumentsResolverProvider {

  String ENABLE_FULL_PROTOTYPE = "enable.full.prototype";
  String ENABLE_FULL_LIFECYCLE = "enable.full.lifecycle";

  /**
   * If a bean name start with this its a {@link FactoryBean}
   */
  String FACTORY_BEAN_PREFIX = "$";

  char FACTORY_BEAN_PREFIX_CHAR = '$';

  //---------------------------------------------------------------------
  // Get operations for name-lookup
  //---------------------------------------------------------------------

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
   * Find the bean with the given name and cast to required type.
   *
   * @param name
   *         Bean name
   * @param requiredType
   *         Cast to required type
   *
   * @return get casted bean instance. returns null if it doesn't exist.
   */
  <T> T getBean(String name, Class<T> requiredType) throws BeansException;

  /**
   * Determine if it is Singleton
   * <p>
   * Find it in the singleton pool when it is not found in the bean definition map
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
  @Nullable
  Class<?> getType(String name) throws NoSuchBeanDefinitionException;

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
  @Nullable
  public <A extends Annotation> A getAnnotationOnBean(String beanName, Class<A> annotationType)
          throws NoSuchBeanDefinitionException;

  /**
   * Does this bean factory contain a bean definition or externally registered singleton
   * instance with the given name?
   * <p>If the given name is an alias, it will be translated back to the corresponding
   * canonical bean name.
   * <p>If this factory is hierarchical, will ask any parent factory if the bean cannot
   * be found in this factory instance.
   * <p>If a bean definition or singleton instance matching the given name is found,
   * this method will return {@code true} whether the named bean definition is concrete
   * or abstract, lazy or eager, in scope or not. Therefore, note that a {@code true}
   * return value from this method does not necessarily indicate that {@link #getBean}
   * will be able to obtain an instance for the same name.
   *
   * @param name
   *         the name of the bean to query
   *
   * @return whether a bean with the given name is present
   *
   * @since 4.0
   */
  boolean containsBean(String name);

  /**
   * Check if this bean factory contains a bean definition with the given name.
   * <p>Does not consider any hierarchy this factory may participate in,
   * and ignores any singleton beans that have been registered by
   * other means than bean definitions.
   *
   * @param beanName
   *         the name of the bean to look for
   *
   * @return if this bean factory contains a bean definition with the given name
   *
   * @see #containsBean(String)
   * @since 4.0
   */
  boolean containsBeanDefinition(String beanName);

  /**
   * Check whether the bean with the given name matches the specified type.
   * More specifically, check whether a {@link #getBean} call for the given name
   * would return an object that is assignable to the specified target type.
   * <p>Translates aliases back to the corresponding canonical bean name.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name
   *         the name of the bean to query
   * @param typeToMatch
   *         the type to match against (as a {@code ResolvableType})
   *
   * @return {@code true} if the bean type matches,
   * {@code false} if it doesn't match or cannot be determined yet
   *
   * @throws NoSuchBeanDefinitionException
   *         if there is no bean with the given name
   * @see #getBean
   * @see #getType
   * @since 4.0
   */
  boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException;

  /**
   * Check whether the bean with the given name matches the specified type.
   * More specifically, check whether a {@link #getBean} call for the given name
   * would return an object that is assignable to the specified target type.
   * <p>Translates aliases back to the corresponding canonical bean name.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name
   *         the name of the bean to query
   * @param typeToMatch
   *         the type to match against (as a {@code Class})
   *
   * @return {@code true} if the bean type matches,
   * {@code false} if it doesn't match or cannot be determined yet
   *
   * @throws NoSuchBeanDefinitionException
   *         if there is no bean with the given name
   * @see #getBean
   * @see #getType
   * @since 4.0
   */
  boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

  /**
   * Return the registered BeanDefinition for the specified bean, allowing access
   * to its property values and constructor argument value (which can be
   * modified during bean factory post-processing).
   * <p>A returned BeanDefinition object should not be a copy but the original
   * definition object as registered in the factory. This means that it should
   * be castable to a more specific implementation type, if necessary.
   * <p><b>NOTE:</b> This method does <i>not</i> consider ancestor factories.
   * It is only meant for accessing local bean definitions of this factory.
   *
   * @param beanName
   *         the name of the bean
   *
   * @return the registered BeanDefinition
   *
   * @throws NoSuchBeanDefinitionException
   *         if there is no bean with the given name defined in this factory
   * @since 4.0
   */
  BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException;

  //---------------------------------------------------------------------
  // Listing Get operations for type-lookup
  //---------------------------------------------------------------------

  /**
   * Return the bean instance that uniquely matches the given object type, if any.
   * <p>This method goes into {@link BeanFactory} by-type lookup territory
   * but may also be translated into a conventional by-name lookup based on the name
   * of the given type. For more extensive retrieval operations across sets of beans,
   * use {@link BeanFactory} and/or {@link BeanFactoryUtils}.
   *
   * @param requiredType
   *         type the bean must match; can be an interface or superclass
   *
   * @return an instance of the single bean matching the required type
   *
   * @throws NoSuchBeanDefinitionException
   *         if no bean of the given type was found
   * @throws NoUniqueBeanException
   *         if more than one bean of the given type was found
   * @throws BeansException
   *         if the bean could not be created
   * @since 3.0
   */
  <T> T getBean(Class<T> requiredType) throws BeansException;

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
  Map<String, Object> getBeansOfAnnotation(
          Class<? extends Annotation> annotationType, boolean includeNonSingletons)
          throws BeansException;

  /**
   * Get a set of beans with given type
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
   * Get a map of beans with given type
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
   * Get a map of beans with given type
   * </p>
   *
   * <p>
   * <b>NOTE:</b>
   * include singletons already in {@code singletons} but not in {@code beanDefinitionMap}
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
   * Return the bean instances that match the given object type (including
   * subclasses), judging from either bean definitions or the value of
   * {@code getBeanClass} in the case of FactoryBeans.
   * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
   * by other means than bean definitions.
   * <p>
   * Get a map of beans with given type
   * </p>
   *
   * @param requiredType
   *         the class or interface to match, or {@code null} for all concrete beans
   * @param includeNonSingletons
   *         whether to include prototype or scoped beans too
   *         or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered
   *         whether to include singletons already in {@code singletons}
   *         but not in {@code beanDefinitionMap}
   * @param <T>
   *         required type
   *
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values
   *
   * @throws BeansException
   *         if a bean could not be created
   * @see FactoryBean#getBeanClass
   * @since 3.0
   */
  default <T> Map<String, T> getBeansOfType(
          Class<T> requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    return getBeansOfType(ResolvableType.fromClass(requiredType), includeNoneRegistered, includeNonSingletons);
  }

  /**
   * Return the bean instances that match the given object type (including
   * subclasses), judging from either bean definitions or the value of
   * {@code getBeanClass} in the case of FactoryBeans.
   * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
   * by other means than bean definitions.
   * <p>
   * Get a map of beans with given type
   * </p>
   *
   * @param requiredType
   *         the ResolvableType or interface to match, or {@code null} for all concrete beans
   * @param includeNonSingletons
   *         whether to include prototype or scoped beans too
   *         or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered
   *         whether to include singletons already in {@code singletons}
   *         but not in {@code beanDefinitionMap}
   * @param <T>
   *         required type
   *
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values
   *
   * @throws BeansException
   *         if a bean could not be created
   * @see FactoryBean#getBeanClass
   * @since 3.0
   */
  <T> Map<String, T> getBeansOfType(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons);

  //

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
  default Set<String> getBeanNamesOfType(Class<?> requiredType) {
    return getBeanNamesOfType(requiredType, true);
  }

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getBeanClass}
   * in the case of FactoryBeans.
   * <p>
   * include singletons already in {@code singletons} but not in {@code beanDefinitionMap}
   * </p>
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
  Set<String> getBeanNamesOfType(Class<?> requiredType, boolean includeNonSingletons);

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getBeanClass}
   * in the case of FactoryBeans.
   *
   * @param requiredType
   *         the class or interface to match, or {@code null} for all concrete beans
   * @param includeNonSingletons
   *         whether to include prototype or scoped beans too
   *         or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered
   *         whether to include singletons already in {@code singletons}
   *         but not in {@code beanDefinitionMap}
   *
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty array if none
   *
   * @see FactoryBean#getBeanClass()
   * @since 3.0
   */
  default Set<String> getBeanNamesOfType(
          Class<?> requiredType, boolean includeNoneRegistered, boolean includeNonSingletons) {
    return getBeanNamesOfType(ResolvableType.fromClass(requiredType), includeNoneRegistered, includeNonSingletons);
  }

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getBeanClass}
   * in the case of FactoryBeans.
   *
   * @param requiredType
   *         the ResolvableType or interface to match, or {@code null} for all concrete beans
   * @param includeNonSingletons
   *         whether to include prototype or scoped beans too
   *         or just singletons (also applies to FactoryBeans)
   * @param includeNoneRegistered
   *         whether to include singletons already in {@code singletons}
   *         but not in {@code beanDefinitionMap}
   *
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty array if none
   *
   * @see FactoryBean#getBeanClass()
   * @since 4.0
   */
  Set<String> getBeanNamesOfType(
          ResolvableType requiredType, boolean includeNoneRegistered, boolean includeNonSingletons);

  /**
   * Find all names of beans which are annotated with the supplied {@link Annotation}
   * type, without creating corresponding bean instances yet.
   * <p>Note that this method considers objects created by FactoryBeans, which means
   * that FactoryBeans will get initialized in order to determine their object type.
   *
   * @param annotationType
   *         the type of annotation to look for
   *         (at class, interface or factory method level of the specified bean)
   *
   * @return the names of all matching beans
   *
   * @see #getAnnotationOnBean(String, Class)
   * @since 4.0
   */
  Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

  /**
   * Return a provider for the specified bean, allowing for lazy on-demand retrieval
   * of instances, including availability and uniqueness options.
   *
   * @param requiredType
   *         type the bean must match; can be an interface or superclass
   *
   * @return a corresponding provider handle
   *
   * @see #getObjectSupplier(ResolvableType)
   * @since 3.0
   */
  <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType);

  /**
   * Return a provider for the specified bean, allowing for lazy on-demand retrieval
   * of instances, including availability and uniqueness options.
   *
   * @param requiredType
   *         type the bean must match; can be a generic type declaration.
   *         Note that collection types are not supported here, in contrast to reflective
   *         injection points. For programmatically retrieving a list of beans matching a
   *         specific type, specify the actual bean type as an argument here and subsequently
   *         use {@link ObjectSupplier#orderedStream()} or its lazy streaming/iteration options.
   *
   * @return a corresponding provider handle
   *
   * @see ObjectSupplier#iterator()
   * @see ObjectSupplier#stream()
   * @see ObjectSupplier#orderedStream()
   * @since 4.0
   */
  <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType);

  /**
   * Get all bean name
   *
   * @param type
   *         Bean type
   *
   * @return A set of names with given type
   */
  @Deprecated
  Set<String> getAliases(Class<?> type);

  //---------------------------------------------------------------------
  // Operations for BeanDefinition
  //---------------------------------------------------------------------

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
   * @param def
   *         BeanDefinition
   *
   * @see #getObjectSupplier(ResolvableType)
   * @since 3.0
   */
  <T> ObjectSupplier<T> getObjectSupplier(BeanDefinition def);

  //---------------------------------------------------------------------
  // bean-factory options
  //---------------------------------------------------------------------

  /**
   * if this feature is enabled and if a property is prototype bean
   * this bean-factory will inject a proxy instance to get prototype
   * instance from every single method call.
   *
   * <p>
   * default is false
   * </p>
   *
   * @see Prototypes#newProxyInstance(Class, BeanDefinition, BeanFactory)
   * @since 3.0
   */
  boolean isFullPrototype();

  /**
   * invoke prototype's destroyBean after every call
   * <p>
   * default is false
   * </p>
   *
   * @see ConfigurableBeanFactory#destroyBean(Object, BeanDefinition)
   * @see #isFullPrototype()
   * @since 3.0
   */
  boolean isFullLifecycle();

  //---------------------------------------------------------------------
  // bean-factory stat
  //---------------------------------------------------------------------

  /**
   * Return the number of beans defined in the factory.
   * <p>Does not consider any hierarchy this factory may participate in,
   * and ignores any singleton beans that have been registered by
   * other means than bean definitions.
   *
   * @return the number of beans defined in the factory
   *
   * @since 4.0
   */
  int getBeanDefinitionCount();

  /**
   * Return the names of all beans defined in this factory.
   * <p>Does not consider any hierarchy this factory may participate in,
   * and ignores any singleton beans that have been registered by
   * other means than bean definitions.
   *
   * @return the names of all beans defined in this factory,
   * or an empty array if none defined
   *
   * @since 4.0
   */
  Set<String> getBeanDefinitionNames();

  /**
   * Return a unified view over all bean names managed by this factory.
   * <p>Includes bean definition names as well as names of manually registered
   * singleton instances, with bean definition names consistently coming first,
   * analogous to how type/annotation specific retrieval of bean names works.
   *
   * @return the composite iterator for the bean names view
   *
   * @see #containsBeanDefinition
   * @see #getBeanNamesOfType(Class)
   * @see #getBeanNamesForAnnotation
   * @since 4.0
   */
  Iterator<String> getBeanNamesIterator();

  /**
   * Get all {@link BeanDefinition}s
   *
   * @return All {@link BeanDefinition}s
   *
   * @since 2.1.6
   */
  Map<String, BeanDefinition> getBeanDefinitions();

}
