/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
package cn.taketoday.beans.factory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DestructionAwareBeanPostProcessor;
import cn.taketoday.beans.factory.support.ChildBeanDefinition;
import cn.taketoday.beans.factory.support.DependencyInjectorProvider;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.lang.Nullable;

/**
 * The root interface for accessing a bean container.
 *
 * <p>This is the basic client view of a bean container;
 * further interfaces such as {@link AutowireCapableBeanFactory} and
 * {@link ConfigurableBeanFactory} are available for specific purposes.
 *
 * <p>This interface is implemented by objects that hold a number of bean definitions,
 * each uniquely identified by a String name. Depending on the bean definition,
 * the factory will return either an independent instance of a contained object
 * (the Prototype design pattern), or a single shared instance (a superior
 * alternative to the Singleton design pattern, in which the instance is a
 * singleton in the scope of the factory). Which type of instance will be returned
 * depends on the bean factory configuration: the API is the same.
 * further scopes are available depending on the concrete application
 * context (e.g. "request" and "session" scopes in a web environment).
 *
 * <p>The point of this approach is that the BeanFactory is a central registry
 * of application components, and centralizes configuration of application
 * components (no more do individual objects need to read properties files,
 * for example). See chapters 4 and 11 of "Expert One-on-One J2EE Design and
 * Development" for a discussion of the benefits of this approach.
 *
 * <p>Note that it is generally better to rely on Dependency Injection
 * ("push" configuration) to configure application objects through setters
 * or constructors, rather than use any form of "pull" configuration like a
 * BeanFactory lookup.  Dependency Injection functionality is
 * implemented using this BeanFactory interface and its subinterfaces.
 *
 * <p>Normally a BeanFactory will load bean definitions stored in a configuration
 * source (such as an XML document), and use the {@code cn.taketoday.beans}
 * package to configure the beans. However, an implementation could simply return
 * Java objects it creates as necessary directly in Java code. There are no
 * constraints on how the definitions could be stored: LDAP, RDBMS, XML,
 * properties file, etc. Implementations are encouraged to support references
 * amongst beans (Dependency Injection).
 * <p>
 * this interface includes operations like  {@code BeanFactory}
 *
 * <p>In contrast to the methods in  {@code BeanFactory}, all of the
 * operations in this interface will also check parent factories if this is a
 * {@link HierarchicalBeanFactory}. If a bean is not found in this factory instance,
 * the immediate parent factory will be asked. Beans in this factory instance
 * are supposed to override beans of the same name in any parent factory.
 *
 * <p>Bean factory implementations should support the standard bean lifecycle interfaces
 * as far as possible. The full set of initialization methods and their standard order is:
 * <ol>
 * <li>BeanNameAware's {@code setBeanName}
 * <li>BeanClassLoaderAware's {@code setBeanClassLoader}
 * <li>BeanFactoryAware's {@code setBeanFactory}
 * <li>EnvironmentAware's {@code setEnvironment}
 * <li>ResourceLoaderAware's {@code setResourceLoader}
 * (only applicable when running in an application context)
 * <li>ApplicationEventPublisherAware's {@code setApplicationEventPublisher}
 * (only applicable when running in an application context)
 * <li>ApplicationContextAware's {@code setApplicationContext}
 * (only applicable when running in an application context)
 * <li>ServletContextAware's {@code setServletContext}
 * (only applicable when running in a web application context)
 * <li>{@code postProcessBeforeInitialization} methods of BeanPostProcessors
 * <li>InitializingBean's {@code afterPropertiesSet}
 * <li>a custom {@code init-method} definition
 * <li>{@code postProcessAfterInitialization} methods of BeanPostProcessors
 * </ol>
 *
 * <p>On shutdown of a bean factory, the following lifecycle methods apply:
 * <ol>
 * <li>{@code postProcessBeforeDestruction} methods of DestructionAwareBeanPostProcessors
 * <li>DisposableBean's {@code destroy}
 * <li>a custom {@code destroy-method} definition
 * </ol>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author TODAY
 * @see BeanNameAware#setBeanName
 * @see BeanClassLoaderAware#setBeanClassLoader
 * @see BeanFactoryAware#setBeanFactory
 * @see cn.taketoday.context.EnvironmentAware#setEnvironment
 * @see cn.taketoday.context.ResourceLoaderAware#setResourceLoader
 * @see cn.taketoday.context.ApplicationEventPublisherAware#setApplicationEventPublisher
 * @see cn.taketoday.context.ApplicationContextAware#setApplicationContext
 * @see cn.taketoday.web.servlet.ServletContextAware#setServletContext
 * @see InitializationBeanPostProcessor#postProcessBeforeInitialization
 * @see InitializingBean#afterPropertiesSet
 * @see BeanDefinition#getInitMethodName()
 * @see InitializationBeanPostProcessor#postProcessAfterInitialization
 * @see DestructionAwareBeanPostProcessor#postProcessBeforeDestruction
 * @see DisposableBean#destroy
 * @see BeanDefinition#getDestroyMethodName()
 * @since 2018-06-23 11:22:26
 */
public interface BeanFactory extends DependencyInjectorProvider {

  /**
   * Used to dereference a {@link FactoryBean} instance and distinguish it from
   * beans <i>created</i> by the FactoryBean. For example, if the bean named
   * {@code myJndiObject} is a FactoryBean, getting {@code &myJndiObject}
   * will return the factory, not the instance returned by the factory.
   */
  String FACTORY_BEAN_PREFIX = "&";

  //---------------------------------------------------------------------
  // Get operations for name-lookup
  //---------------------------------------------------------------------

  /**
   * Return an instance, which may be shared or independent, of the specified bean.
   * <p>This method allows a BeanFactory to be used as a replacement for the
   * Singleton or Prototype design pattern. Callers may retain references to
   * returned objects in the case of Singleton beans.
   * <p>Translates aliases back to the corresponding canonical bean name.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name the name of the bean to retrieve
   * @return bean instance, returns null if its return from a factory-method
   * @throws BeansException Exception occurred when getting a named bean
   * @throws NoSuchBeanDefinitionException if there is no bean with the specified name
   */
  Object getBean(String name) throws BeansException;

  /**
   * Return an instance, which may be shared or independent, of the specified bean.
   * <p>Allows for specifying explicit constructor arguments / factory method arguments,
   * overriding the specified default arguments (if any) in the bean definition.
   *
   * @param name the name of the bean to retrieve
   * @param args arguments to use when creating a bean instance using explicit arguments
   * (only applied when creating a new instance as opposed to retrieving an existing one)
   * @return an instance of the bean, returns null if its return from a factory-method
   * @throws BeanDefinitionStoreException if arguments have been given but
   * the affected bean isn't a prototype
   * @throws BeansException if the bean could not be created
   * @throws NoSuchBeanDefinitionException if there is no such bean definition
   * @since 4.0
   */
  Object getBean(String name, Object... args) throws BeansException;

  /**
   * Return an instance, which may be shared or independent, of the specified bean.
   * <p>Behaves the same as {@link #getBean(String)}, but provides a measure of type
   * safety by throwing a BeanNotOfRequiredTypeException if the bean is not of the
   * required type. This means that ClassCastException can't be thrown on casting
   * the result correctly, as can happen with {@link #getBean(String)}.
   * <p>Translates aliases back to the corresponding canonical bean name.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name the name of the bean to retrieve
   * @param requiredType type the bean must match; can be an interface or superclass
   * @return an instance of the bean,returns null if its return from a factory-method
   * @throws BeanNotOfRequiredTypeException if the bean is not of the required type
   * @throws BeansException if the bean could not be created
   * @throws NoSuchBeanDefinitionException if there is no such bean definition
   */
  <T> T getBean(String name, Class<T> requiredType) throws BeansException;

  /**
   * Is this bean a shared singleton? That is, will {@link #getBean} always
   * return the same instance?
   * <p>Note: This method returning {@code false} does not clearly indicate
   * independent instances. It indicates non-singleton instances, which may correspond
   * to a scoped bean as well. Use the {@link #isPrototype} operation to explicitly
   * check for independent instances.
   * <p>Translates aliases back to the corresponding canonical bean name.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name the name of the bean to query
   * @return whether this bean corresponds to a singleton instance
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @see #getBean
   * @see #isPrototype
   */
  boolean isSingleton(String name) throws NoSuchBeanDefinitionException;

  /**
   * Is this bean a prototype? That is, will {@link #getBean} always return
   * independent instances?
   * <p>Note: This method returning {@code false} does not clearly indicate
   * a singleton object. It indicates non-independent instances, which may correspond
   * to a scoped bean as well. Use the {@link #isSingleton} operation to explicitly
   * check for a shared singleton instance.
   * <p>Translates aliases back to the corresponding canonical bean name.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name the name of the bean to query
   * @return whether this bean will always deliver independent instances
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @see #getBean
   * @see #isSingleton
   */
  boolean isPrototype(String name) throws NoSuchBeanDefinitionException;

  /**
   * Determine the type of the bean with the given name. More specifically,
   * determine the type of object that {@link #getBean} would return for the given name.
   * <p>For a {@link FactoryBean}, return the type of object that the FactoryBean creates,
   * as exposed by {@link FactoryBean#getObjectType()}. This may lead to the initialization
   * of a previously uninitialized {@code FactoryBean} (see {@link #getType(String, boolean)}).
   * <p>Translates aliases back to the corresponding canonical bean name.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name the name of the bean to query
   * @return the type of the bean, or {@code null} if not determinable
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @see #getBean
   * @see #isTypeMatch
   */
  @Nullable
  Class<?> getType(String name) throws NoSuchBeanDefinitionException;

  /**
   * Determine the type of the bean with the given name. More specifically,
   * determine the type of object that {@link #getBean} would return for the given name.
   * <p>For a {@link FactoryBean}, return the type of object that the FactoryBean creates,
   * as exposed by {@link FactoryBean#getObjectType()}. Depending on the
   * {@code allowFactoryBeanInit} flag, this may lead to the initialization of a previously
   * uninitialized {@code FactoryBean} if no early type information is available.
   * <p>Translates aliases back to the corresponding canonical bean name.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name the name of the bean to query
   * @param allowFactoryBeanInit whether a {@code FactoryBean} may get initialized
   * just for the purpose of determining its object type
   * @return the type of the bean, or {@code null} if not determinable
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @see #getBean
   * @see #isTypeMatch
   * @since 4.0
   */
  @Nullable
  Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException;

  /**
   * Find an {@link Annotation} of {@code annotationType} on the specified bean,
   * checking the bean's factory method if no annotation can be found traversing its
   * interfaces and super classes(if any).
   * <p>
   * Synthesize MergedAnnotation into Annotation
   * </p>
   *
   * @param beanName the name of the bean to look for annotations on
   * @param annotationType the type of annotation to look for
   * (at class, interface or factory method level of the specified bean)
   * @return the annotation of the given type if found, or {@code null} otherwise
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @see #getAnnotatedBeans
   * @since 3.0
   */
  @Nullable
  <A extends Annotation> A findSynthesizedAnnotation(String beanName, Class<A> annotationType)
          throws NoSuchBeanDefinitionException;

  /**
   * Find an {@link MergedAnnotation} of {@code annotationType} on the specified bean,
   * checking the bean's factory method if no annotation can be found traversing its
   * interfaces and super classes(if any).
   *
   * @param beanName the name of the bean to look for annotations on
   * @param annotationType the type of annotation to look for
   * (at class, interface or factory method level of the specified bean)
   * @return the annotation of the given type if found, or {@code null} otherwise
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @see #getBeanNamesForAnnotation
   * @see #getBeansWithAnnotation
   * @see #getType(String)
   * @since 3.0
   */
  <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(String beanName, Class<A> annotationType)
          throws NoSuchBeanDefinitionException;

  /**
   * Find an {@link MergedAnnotation} of {@code annotationType} on the specified bean,
   * checking the bean's factory method if no annotation can be found traversing its
   * interfaces and super classes(if any).
   *
   * @param beanName the name of the bean to look for annotations on
   * @param annotationType the type of annotation to look for
   * (at class, interface or factory method level of the specified bean)
   * @param allowFactoryBeanInit whether a {@code FactoryBean} may get initialized
   * just for the purpose of determining its object type
   * @return the annotation of the given type if found, or {@code null} otherwise
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @see #getBeanNamesForAnnotation
   * @see #getBeansWithAnnotation
   * @see #getType(String, boolean)
   * @since 4.0
   */
  <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(
          String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
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
   * @param name the name of the bean to query
   * @return whether a bean with the given name is present
   * @since 4.0
   */
  boolean containsBean(String name);

  /**
   * Check if this bean factory contains a bean definition with the given name.
   * <p>Does not consider any hierarchy this factory may participate in,
   * and ignores any singleton beans that have been registered by
   * other means than bean definitions.
   *
   * @param beanName the name of the bean to look for
   * @return if this bean factory contains a bean definition with the given name
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
   * @param name the name of the bean to query
   * @param typeToMatch the type to match against (as a {@code ResolvableType})
   * @return {@code true} if the bean type matches,
   * {@code false} if it doesn't match or cannot be determined yet
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
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
   * @param name the name of the bean to query
   * @param typeToMatch the type to match against (as a {@code Class})
   * @return {@code true} if the bean type matches,
   * {@code false} if it doesn't match or cannot be determined yet
   * @throws NoSuchBeanDefinitionException if there is no bean with the given name
   * @see #getBean
   * @see #getType
   * @since 4.0
   */
  boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException;

  /**
   * Return the bean definition for the given bean name.
   * Subclasses should normally implement caching, as this method is invoked
   * by this class every time bean definition metadata is needed.
   * <p>Depending on the nature of the concrete bean factory implementation,
   * this operation might be expensive (for example, because of directory lookups
   * in external registries). However, for listable bean factories, this usually
   * just amounts to a local hash lookup: The operation is therefore part of the
   * public interface there. The same implementation can serve for both this
   * template method and the public interface method in that case.
   *
   * @param beanName the name of the bean to find a definition for
   * @return the BeanDefinition for this prototype name (never {@code null})
   * @throws NoSuchBeanDefinitionException if the bean definition cannot be resolved
   * @throws BeansException in case of errors
   * @see RootBeanDefinition
   * @see ChildBeanDefinition
   * @see ConfigurableBeanFactory#getBeanDefinition
   */
  BeanDefinition getBeanDefinition(String beanName) throws BeansException;

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
   * @param requiredType type the bean must match; can be an interface or superclass
   * @return an instance of the single bean matching the required type
   * @throws NoUniqueBeanDefinitionException if more than one bean of the given type was found
   * @throws BeansException if the bean could not be created
   * @throws NoSuchBeanDefinitionException if no bean of the given type was found
   * @since 3.0
   */
  <T> T getBean(Class<T> requiredType) throws BeansException;

  /**
   * Return an instance, which may be shared or independent, of the specified bean.
   * <p>Allows for specifying explicit constructor arguments / factory method arguments,
   * overriding the specified default arguments (if any) in the bean definition.
   * <p>This method goes into {@link BeanFactory} by-type lookup territory
   * but may also be translated into a conventional by-name lookup based on the name
   * of the given type. For more extensive retrieval operations across sets of beans,
   * use {@link BeanFactory} and/or {@link BeanFactoryUtils}.
   *
   * @param requiredType type the bean must match; can be an interface or superclass
   * @param args arguments to use when creating a bean instance using explicit arguments
   * (only applied when creating a new instance as opposed to retrieving an existing one)
   * @return an instance of the bean, returns null if its return from a factory-method.
   * @throws BeanDefinitionStoreException if arguments have been given but
   * the affected bean isn't a prototype
   * @throws NoSuchBeanDefinitionException if there is no such bean definition
   * @throws BeansException if the bean could not be created
   * @since 4.0
   */
  <T> T getBean(Class<T> requiredType, Object... args) throws BeansException;

  /**
   * Find all beans which are annotated with the supplied {@link Annotation} type,
   * returning a List of bean instances.
   * <p>Note that this method considers objects created by FactoryBeans, which means
   * that FactoryBeans will get initialized in order to determine their object type.
   *
   * @param annotationType the type of annotation to look for
   * (at class, interface or factory method level of the specified bean)
   * @return a List with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values, never be {@code null}
   * @throws BeansException if a bean could not be created
   * @see #findSynthesizedAnnotation
   * @since 3.0
   */
  @SuppressWarnings("unchecked")
  default <T> List<T> getAnnotatedBeans(Class<? extends Annotation> annotationType) throws BeansException {
    return (List<T>) new ArrayList<>(getBeansWithAnnotation(annotationType).values());
  }

  /**
   * Find all beans which are annotated with the supplied {@link Annotation} type,
   * returning a Map of bean names with corresponding bean instances.
   * <p>Note that this method considers objects created by FactoryBeans, which means
   * that FactoryBeans will get initialized in order to determine their object type.
   *
   * @param annotationType the type of annotation to look for
   * (at class, interface or factory method level of the specified bean)
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values, never be {@code null}
   * @throws BeansException if a bean could not be created
   * @see #findSynthesizedAnnotation
   * @since 3.0
   */
  default Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) throws BeansException {
    return getBeansWithAnnotation(annotationType, true);
  }

  /**
   * Find all beans which are annotated with the supplied {@link Annotation} type,
   * returning a Map of bean names with corresponding bean instances.
   * <p>Note that this method considers objects created by FactoryBeans, which means
   * that FactoryBeans will get initialized in order to determine their object type.
   *
   * @param annotationType the type of annotation to look for
   * (at class, interface or factory method level of the specified bean)
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values, never be {@code null}
   * @throws BeansException if a bean could not be created
   * @see #findSynthesizedAnnotation
   * @since 3.0
   */
  Map<String, Object> getBeansWithAnnotation(
          Class<? extends Annotation> annotationType, boolean includeNonSingletons)
          throws BeansException;

  /**
   * Get a set of beans with given type
   *
   * @param requiredType Given bean type
   * @return A set of beans with given type, never be {@code null}
   * @since 2.1.2
   */
  default <T> List<T> getBeans(Class<T> requiredType) {
    return new ArrayList<>(getBeansOfType(requiredType).values());
  }

  /**
   * Return the bean instances that match the given object type (including
   * subclasses), judging from either bean definitions or the value of
   * {@code getObjectType} in the case of FactoryBeans.
   * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
   * check nested beans which might match the specified type as well.
   * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
   * will get initialized. If the object created by the FactoryBean doesn't match,
   * the raw FactoryBean itself will be matched against the type.
   * <p>Does not consider any hierarchy this factory may participate in.
   * Use BeanFactoryUtils' {@code beansOfTypeIncludingAncestors}
   * to include beans in ancestor factories too.
   * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
   * by other means than bean definitions.
   * <p>This version of getBeansOfType matches all kinds of beans, be it
   * singletons, prototypes, or FactoryBeans. In most implementations, the
   * result will be the same as for {@code getBeansOfType(type, true, true)}.
   * <p>The Map returned by this method should always return bean names and
   * corresponding bean instances <i>in the order of definition</i> in the
   * backend configuration, as far as possible.
   *
   * @param requiredType the class or interface to match, or {@code null} for all concrete beans
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values
   * @throws BeansException if a bean could not be created
   * @see FactoryBean#getObjectType()
   * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(BeanFactory, Class)
   * @since 2.1.6
   */
  default <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
    return getBeansOfType(requiredType, true, true);
  }

  /**
   * Return the bean instances that match the given object type (including
   * subclasses), judging from either bean definitions or the value of
   * {@code getObjectType} in the case of FactoryBeans.
   * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
   * check nested beans which might match the specified type as well.
   * <p>Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set,
   * which means that FactoryBeans will get initialized. If the object created by the
   * FactoryBean doesn't match, the raw FactoryBean itself will be matched against the
   * type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked
   * (which doesn't require initialization of each FactoryBean).
   * <p>Does not consider any hierarchy this factory may participate in.
   * Use BeanFactoryUtils' {@code beansOfTypeIncludingAncestors}
   * to include beans in ancestor factories too.
   * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
   * by other means than bean definitions.
   * <p>The Map returned by this method should always return bean names and
   * corresponding bean instances <i>in the order of definition</i> in the
   * backend configuration, as far as possible.
   *
   * @param type the class or interface to match, or {@code null} for all concrete beans
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values
   * @throws BeansException if a bean could not be created
   * @see FactoryBean#getObjectType()
   * @see BeanFactoryUtils#beansOfTypeIncludingAncestors(BeanFactory, Class, boolean, boolean)
   * @since 3.0
   */
  default <T> Map<String, T> getBeansOfType(
          @Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
    return getBeansOfType(ResolvableType.fromClass(type), includeNonSingletons, allowEagerInit);
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
   * @param requiredType the ResolvableType or interface to match, or {@code null} for all concrete beans
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @param <T> required type
   * @return a Map with the matching beans, containing the bean names as
   * keys and the corresponding bean instances as values
   * @throws BeansException if a bean could not be created
   * @see FactoryBean#getObjectType
   * @since 3.0
   */
  <T> Map<String, T> getBeansOfType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit);

  //

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getBeanClass}
   * in the case of FactoryBeans.
   *
   * @param requiredType the class or interface to match, or {@code null} for all bean names
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty Set if none
   * @see FactoryBean#getObjectType()
   * @since 3.0
   */
  default Set<String> getBeanNamesForType(Class<?> requiredType) {
    return getBeanNamesForType(requiredType, true);
  }

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getBeanClass}
   * in the case of FactoryBeans.
   * <p>
   * include singletons already in {@code singletons} but not in {@code beanDefinitionMap}
   * </p>
   *
   * @param requiredType the class or interface to match, or {@code null} for all bean names
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty Set if none
   * @see FactoryBean#getObjectType()
   * @since 3.0
   */
  Set<String> getBeanNamesForType(Class<?> requiredType, boolean includeNonSingletons);

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getObjectType}
   * in the case of FactoryBeans.
   * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
   * check nested beans which might match the specified type as well.
   * <p>Does consider objects created by FactoryBeans if the "allowEagerInit" flag is set,
   * which means that FactoryBeans will get initialized. If the object created by the
   * FactoryBean doesn't match, the raw FactoryBean itself will be matched against the
   * type. If "allowEagerInit" is not set, only raw FactoryBeans will be checked
   * (which doesn't require initialization of each FactoryBean).
   * <p>Does not consider any hierarchy this factory may participate in.
   * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
   * to include beans in ancestor factories too.
   * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
   * by other means than bean definitions.
   * <p>Bean names returned by this method should always return bean names <i>in the
   * order of definition</i> in the backend configuration, as far as possible.
   *
   * @param requiredType the class or interface to match, or {@code null} for all bean names
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty Set if none
   * @see FactoryBean#getObjectType()
   * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(BeanFactory, Class, boolean, boolean)
   * @since 3.0
   */
  default Set<String> getBeanNamesForType(
          @Nullable Class<?> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    return getBeanNamesForType(ResolvableType.fromClass(requiredType), includeNonSingletons, allowEagerInit);
  }

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getObjectType}
   * in the case of FactoryBeans.
   * <p><b>NOTE: This method introspects top-level beans only.</b> It does <i>not</i>
   * check nested beans which might match the specified type as well.
   * <p>Does consider objects created by FactoryBeans, which means that FactoryBeans
   * will get initialized. If the object created by the FactoryBean doesn't match,
   * the raw FactoryBean itself will be matched against the type.
   * <p>Does not consider any hierarchy this factory may participate in.
   * Use BeanFactoryUtils' {@code beanNamesForTypeIncludingAncestors}
   * to include beans in ancestor factories too.
   * <p>Note: Does <i>not</i> ignore singleton beans that have been registered
   * by other means than bean definitions.
   * <p>This version of {@code getBeanNamesForType} matches all kinds of beans,
   * be it singletons, prototypes, or FactoryBeans. In most implementations, the
   * result will be the same as for {@code getBeanNamesForType(type, true, true)}.
   * <p>Bean names returned by this method should always return bean names <i>in the
   * order of definition</i> in the backend configuration, as far as possible.
   *
   * @param type the generically typed class or interface to match
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty set if none
   * @see #isTypeMatch(String, ResolvableType)
   * @see FactoryBean#getObjectType
   * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors(BeanFactory, ResolvableType)
   * @since 4.0
   */
  Set<String> getBeanNamesForType(ResolvableType type);

  /**
   * Return the names of beans matching the given type (including subclasses),
   * judging from either bean definitions or the value of {@code getBeanClass}
   * in the case of FactoryBeans.
   *
   * @param requiredType the generically typed class or interface to match
   * @param includeNonSingletons whether to include prototype or scoped beans too
   * or just singletons (also applies to FactoryBeans)
   * @param allowEagerInit whether to initialize <i>lazy-init singletons</i> and
   * <i>objects created by FactoryBeans</i> (or by factory methods with a
   * "factory-bean" reference) for the type check. Note that FactoryBeans need to be
   * eagerly initialized to determine their type: So be aware that passing in "true"
   * for this flag will initialize FactoryBeans and "factory-bean" references.
   * @return the names of beans (or objects created by FactoryBeans) matching
   * the given object type (including subclasses), or an empty array if none
   * @see FactoryBean#getObjectType()
   * @since 4.0
   */
  Set<String> getBeanNamesForType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit);

  /**
   * Find all names of beans which are annotated with the supplied {@link Annotation}
   * type, without creating corresponding bean instances yet.
   * <p>Note that this method considers objects created by FactoryBeans, which means
   * that FactoryBeans will get initialized in order to determine their object type.
   *
   * @param annotationType the type of annotation to look for
   * (at class, interface or factory method level of the specified bean)
   * @return the names of all matching beans
   * @see #findSynthesizedAnnotation(String, Class)
   * @since 4.0
   */
  Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType);

  /**
   * Return a provider for the specified bean, allowing for lazy on-demand retrieval
   * of instances, including availability and uniqueness options.
   * <p>For matching a generic type, consider {@link #getBeanProvider(ResolvableType)}.
   *
   * @param requiredType type the bean must match; can be an interface or superclass
   * @return a corresponding provider handle
   * @see #getBeanProvider(ResolvableType)
   * @since 3.0
   */
  <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType);

  /**
   * Return a provider for the specified bean, allowing for lazy on-demand retrieval
   * of instances, including availability and uniqueness options. This variant allows
   * for specifying a generic type to match, similar to reflective injection points
   * with generic type declarations in method/constructor parameters.
   * <p>Note that collections of beans are not supported here, in contrast to reflective
   * injection points. For programmatically retrieving a list of beans matching a
   * specific type, specify the actual bean type as an argument here and subsequently
   * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
   * <p>Also, generics matching is strict here, as per the Java assignment rules.
   * For lenient fallback matching with unchecked semantics (similar to the ´unchecked´
   * Java compiler warning), consider calling {@link #getBeanProvider(Class)} with the
   * raw type as a second step if no full generic match is
   * {@link ObjectProvider#getIfAvailable() available} with this variant.
   *
   * @param requiredType type the bean must match; can be a generic type declaration
   * @return a corresponding provider handle
   * @see ObjectProvider#iterator()
   * @see ObjectProvider#stream()
   * @see ObjectProvider#orderedStream()
   * @since 4.0
   */
  <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType);

  /**
   * Return a provider for the specified bean, allowing for lazy on-demand retrieval
   * of instances, including availability and uniqueness options.
   *
   * @param requiredType type the bean must match; can be an interface or superclass
   * @param allowEagerInit whether stream-based access may initialize <i>lazy-init
   * singletons</i> and <i>objects created by FactoryBeans</i> (or by factory methods
   * with a "factory-bean" reference) for the type check
   * @return a corresponding provider handle
   * @see #getBeanProvider(ResolvableType, boolean)
   * @see #getBeanProvider(Class)
   * @see #getBeansOfType(Class, boolean, boolean)
   * @see #getBeanNamesForType(Class, boolean, boolean)
   * @since 4.0
   */
  <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit);

  /**
   * Return a provider for the specified bean, allowing for lazy on-demand retrieval
   * of instances, including availability and uniqueness options.
   *
   * @param requiredType type the bean must match; can be a generic type declaration.
   * Note that collection types are not supported here, in contrast to reflective
   * injection points. For programmatically retrieving a list of beans matching a
   * specific type, specify the actual bean type as an argument here and subsequently
   * use {@link ObjectProvider#orderedStream()} or its lazy streaming/iteration options.
   * @param allowEagerInit whether stream-based access may initialize <i>lazy-init
   * singletons</i> and <i>objects created by FactoryBeans</i> (or by factory methods
   * with a "factory-bean" reference) for the type check
   * @return a corresponding provider handle
   * @see #getBeanProvider(ResolvableType)
   * @see ObjectProvider#iterator()
   * @see ObjectProvider#stream()
   * @see ObjectProvider#orderedStream()
   * @see #getBeanNamesForType(ResolvableType, boolean, boolean)
   * @since 4.0
   */
  <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit);

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
   * @since 4.0
   */
  String[] getBeanDefinitionNames();

  /**
   * Get all {@link BeanDefinition}s
   *
   * @return All {@link BeanDefinition}s
   * @since 2.1.6
   */
  Map<String, BeanDefinition> getBeanDefinitions();

  /**
   * Return the aliases for the given bean name, if any.
   * <p>All of those aliases point to the same bean when used in a {@link #getBean} call.
   * <p>If the given name is an alias, the corresponding original bean name
   * and other aliases (if any) will be returned, with the original bean name
   * being the first element in the array.
   * <p>Will ask the parent factory if the bean cannot be found in this factory instance.
   *
   * @param name the bean name to check for aliases
   * @return the aliases, or an empty array if none
   * @see #getBean
   * @since 4.0
   */
  String[] getAliases(String name);

  /**
   * unwrap this BeanFactory to {@code requiredType}
   *
   * @throws IllegalArgumentException not a requiredType
   * @since 4.0
   */
  @SuppressWarnings("unchecked")
  default <T> T unwrap(Class<T> requiredType) {
    if (requiredType.isInstance(this)) {
      return (T) this;
    }
    throw new IllegalArgumentException("This BeanFactory '" + this + "' is not a " + requiredType);
  }

}
