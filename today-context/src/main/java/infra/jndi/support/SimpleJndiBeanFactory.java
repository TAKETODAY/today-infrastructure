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

package infra.jndi.support;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import infra.beans.BeansException;
import infra.beans.factory.BeanDefinitionStoreException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanNotOfRequiredTypeException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.ObjectProvider;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.support.DependencyInjector;
import infra.beans.factory.support.StandardBeanFactory;
import infra.context.annotation.CommonAnnotationBeanPostProcessor;
import infra.core.ResolvableType;
import infra.core.annotation.MergedAnnotation;
import infra.jndi.JndiLocatorSupport;
import infra.jndi.TypeMismatchNamingException;
import infra.lang.Nullable;

/**
 * Simple JNDI-based implementation of Framework's
 * {@link BeanFactory} interface.
 * Does not support enumerating bean definitions
 *
 * <p>This factory resolves given bean names as JNDI names within the
 * Jakarta EE application's "java:comp/env/" namespace. It caches the resolved
 * types for all obtained objects, and optionally also caches shareable
 * objects (if they are explicitly marked as
 * {@link #addShareableResource shareable resource}.
 *
 * <p>The main intent of this factory is usage in combination with Framework's
 * {@link CommonAnnotationBeanPostProcessor},
 * configured as "resourceFactory" for resolving {@code @Resource}
 * annotations as JNDI objects without intermediate bean definitions.
 * It may be used for similar lookup scenarios as well, of course,
 * in particular if BeanFactory-style type checking is required.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see StandardBeanFactory
 * @see CommonAnnotationBeanPostProcessor
 * @since 4.0 2022/3/5 12:11
 */
public class SimpleJndiBeanFactory extends JndiLocatorSupport implements BeanFactory {

  /** JNDI names of resources that are known to be shareable, i.e. can be cached */
  private final Set<String> shareableResources = new HashSet<>();

  /** Cache of shareable singleton objects: bean name to bean instance. */
  private final Map<String, Object> singletonObjects = new HashMap<>();

  /** Cache of the types of nonshareable resources: bean name to bean type. */
  private final Map<String, Class<?>> resourceTypes = new HashMap<>();

  @Nullable
  private DependencyInjector dependencyInjector;

  public SimpleJndiBeanFactory() {
    setResourceRef(true);
  }

  /**
   * Add the name of a shareable JNDI resource,
   * which this factory is allowed to cache once obtained.
   *
   * @param shareableResource the JNDI name
   * (typically within the "java:comp/env/" namespace)
   */
  public void addShareableResource(String shareableResource) {
    this.shareableResources.add(shareableResource);
  }

  /**
   * Set a list of names of shareable JNDI resources,
   * which this factory is allowed to cache once obtained.
   *
   * @param shareableResources the JNDI names
   * (typically within the "java:comp/env/" namespace)
   */
  public void setShareableResources(String... shareableResources) {
    Collections.addAll(this.shareableResources, shareableResources);
  }

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public Object getBean(String name) throws BeansException {
    return getBean(name, Object.class);
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    try {
      if (isSingleton(name)) {
        return doGetSingleton(name, requiredType);
      }
      else {
        return lookup(name, requiredType);
      }
    }
    catch (NameNotFoundException ex) {
      throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
    }
    catch (TypeMismatchNamingException ex) {
      throw new BeanNotOfRequiredTypeException(name, ex.getRequiredType(), ex.getActualType());
    }
    catch (NamingException ex) {
      throw new BeanDefinitionStoreException("JNDI environment", name, "JNDI lookup failed", ex);
    }
  }

  @Override
  public Object getBean(String name, @Nullable Object... args) throws BeansException {
    if (args != null) {
      throw new UnsupportedOperationException(
              "SimpleJndiBeanFactory does not support explicit bean creation arguments");
    }
    return getBean(name);
  }

  @Override
  public <T> T getBean(Class<T> requiredType) throws BeansException {
    return getBean(requiredType.getSimpleName(), requiredType);
  }

  @Override
  public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
    if (args != null) {
      throw new UnsupportedOperationException(
              "SimpleJndiBeanFactory does not support explicit bean creation arguments");
    }
    return getBean(requiredType);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
    return new ObjectProvider<>() {
      @Override
      public T get() throws BeansException {
        return getBean(requiredType);
      }

      @Override
      public T get(Object... args) throws BeansException {
        return getBean(requiredType, args);
      }

      @Override
      @Nullable
      public T getIfAvailable() throws BeansException {
        return getBean(requiredType);
      }

      @Override
      @Nullable
      public T getIfUnique() throws BeansException {
        return getBean(requiredType);
      }
    };
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
    throw new UnsupportedOperationException(
            "SimpleJndiBeanFactory does not support resolution by ResolvableType");
  }

  @Override
  public boolean containsBean(String name) {
    if (this.singletonObjects.containsKey(name) || this.resourceTypes.containsKey(name)) {
      return true;
    }
    try {
      doGetType(name);
      return true;
    }
    catch (NamingException ex) {
      return false;
    }
  }

  @Override
  public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    return this.shareableResources.contains(name);
  }

  @Override
  public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
    return !this.shareableResources.contains(name);
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
    Class<?> type = getType(name);
    return (type != null && typeToMatch.isAssignableFrom(type));
  }

  @Override
  public boolean isTypeMatch(String name, @Nullable Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    Class<?> type = getType(name);
    return (typeToMatch == null || (type != null && typeToMatch.isAssignableFrom(type)));
  }

  @Override
  @Nullable
  public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
    return getType(name, true);
  }

  @Override
  @Nullable
  public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    try {
      return doGetType(name);
    }
    catch (NameNotFoundException ex) {
      throw new NoSuchBeanDefinitionException(name, "not found in JNDI environment");
    }
    catch (NamingException ex) {
      return null;
    }
  }

  @Override
  public String[] getAliases(String name) {
    return new String[0];
  }

  @SuppressWarnings("unchecked")
  private <T> T doGetSingleton(String name, @Nullable Class<T> requiredType) throws NamingException {
    synchronized(this.singletonObjects) {
      Object singleton = this.singletonObjects.get(name);
      if (singleton != null) {
        if (requiredType != null && !requiredType.isInstance(singleton)) {
          throw new TypeMismatchNamingException(convertJndiName(name), requiredType, singleton.getClass());
        }
        return (T) singleton;
      }
      T jndiObject = lookup(name, requiredType);
      this.singletonObjects.put(name, jndiObject);
      return jndiObject;
    }
  }

  private Class<?> doGetType(String name) throws NamingException {
    if (isSingleton(name)) {
      return doGetSingleton(name, null).getClass();
    }
    else {
      synchronized(this.resourceTypes) {
        Class<?> type = this.resourceTypes.get(name);
        if (type == null) {
          type = lookup(name, null).getClass();
          this.resourceTypes.put(name, type);
        }
        return type;
      }
    }
  }

  //

  @Nullable
  @Override
  public <A extends Annotation> A findSynthesizedAnnotation(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support findSynthesizedAnnotation");
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support findAnnotationOnBean");
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support findAnnotationOnBean");
  }

  @Override
  public <A extends Annotation> Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support findAllAnnotationsOnBean");
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support containsBeanDefinition");
  }

  @Nullable
  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeanDefinition");
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(
          Class<? extends Annotation> annotationType, boolean includeNonSingletons) throws BeansException {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeansWithAnnotation");
  }

  @Override
  public <T> Map<String, T> getBeansOfType(ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeansOfType");
  }

  @Override
  public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) throws BeansException {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeansOfType");
  }

  @Override
  public String[] getBeanNamesForType(@Nullable Class<?> type) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeanNamesForType");
  }

  @Override
  public String[] getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeanNamesForType");
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType type) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeanNamesForType");
  }

  @Override
  public String[] getBeanNamesForType(ResolvableType type, boolean includeNonSingletons, boolean allowEagerInit) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeanNamesForType");
  }

  @Override
  public String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeanNamesForAnnotation");
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getObjectProvider");
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getObjectProvider");
  }

  @Override
  public int getBeanDefinitionCount() {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeanDefinitionCount");
  }

  @Override
  public String[] getBeanDefinitionNames() {
    throw new UnsupportedOperationException("SimpleJndiBeanFactory does not support getBeanDefinitionNames");
  }

  @Override
  public DependencyInjector getInjector() {
    if (dependencyInjector == null) {
      this.dependencyInjector = new DependencyInjector(null);
    }
    return dependencyInjector;
  }
}
