/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.support;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanIsNotAFactoryException;
import cn.taketoday.beans.factory.BeanNotOfRequiredTypeException;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.SmartFactoryBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.RepeatableContainers;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Static {@link cn.taketoday.beans.factory.BeanFactory} implementation
 * which allows one to register existing singleton instances programmatically.
 *
 * <p>Does not have support for prototype beans or aliases.
 *
 * <p>Serves as an example for a simple implementation of the
 * {@link cn.taketoday.beans.factory.BeanFactory} interface,
 * managing existing bean instances rather than creating new ones based on bean
 * definitions, and not implementing any extended SPI interfaces (such as
 * {@link cn.taketoday.beans.factory.config.ConfigurableBeanFactory}).
 *
 * <p>For a full-fledged factory based on bean definitions, have a look at
 * {@link StandardBeanFactory}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/30 20:28
 */
public class StaticListableBeanFactory extends SimpleBeanDefinitionRegistry implements BeanFactory {

  /** Map from bean name to bean instance. */
  private final Map<String, Object> beans;

  /**
   * Create a regular {@code StaticListableBeanFactory}, to be populated
   * with singleton bean instances through {@link #addBean} calls.
   */
  public StaticListableBeanFactory() {
    this.beans = new LinkedHashMap<>();
  }

  /**
   * Create a {@code StaticListableBeanFactory} wrapping the given {@code Map}.
   * <p>Note that the given {@code Map} may be pre-populated with beans;
   * or new, still allowing for beans to be registered via {@link #addBean};
   * or {@link java.util.Collections#emptyMap()} for a dummy factory which
   * enforces operating against an empty set of beans.
   *
   * @param beans a {@code Map} for holding this factory's beans, with the
   * bean name as key and the corresponding singleton object as value
   */
  public StaticListableBeanFactory(Map<String, Object> beans) {
    Assert.notNull(beans, "Beans Map is required");
    this.beans = beans;
  }

  /**
   * Add a new singleton bean.
   * <p>Will overwrite any existing instance for the given name.
   *
   * @param name the name of the bean
   * @param bean the bean instance
   */
  public void addBean(String name, Object bean) {
    this.beans.put(name, bean);
  }

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public Object getBean(String name) throws BeansException {
    String beanName = BeanFactoryUtils.transformedBeanName(name);
    Object bean = this.beans.get(beanName);

    if (bean == null) {
      throw new NoSuchBeanDefinitionException(beanName,
              "Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
    }

    // Don't let calling code try to dereference the
    // bean factory if the bean isn't a factory
    if (BeanFactoryUtils.isFactoryDereference(name) && !(bean instanceof FactoryBean)) {
      throw new BeanIsNotAFactoryException(beanName, bean.getClass());
    }

    if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
      try {
        Object exposedObject = factoryBean.getObject();
        if (exposedObject == null) {
          throw new BeanCreationException(beanName, "FactoryBean exposed null object");
        }
        return exposedObject;
      }
      catch (Exception ex) {
        throw new BeanCreationException(beanName, "FactoryBean threw exception on object creation", ex);
      }
    }
    else {
      return bean;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getBean(String name, @Nullable Class<T> requiredType) throws BeansException {
    Object bean = getBean(name);
    if (requiredType != null && !requiredType.isInstance(bean)) {
      throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
    }
    return (T) bean;
  }

  @Override
  public Object getBean(String name, Object... args) throws BeansException {
    if (ObjectUtils.isNotEmpty(args)) {
      throw new UnsupportedOperationException(
              "StaticListableBeanFactory does not support explicit bean creation arguments");
    }
    return getBean(name);
  }

  @Override
  public <T> T getBean(Class<T> requiredType) throws BeansException {
    Set<String> beanNames = getBeanNamesForType(requiredType);
    if (beanNames.size() == 1) {
      return getBean(CollectionUtils.firstElement(beanNames), requiredType);
    }
    else if (beanNames.size() > 1) {
      throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
    }
    else {
      throw new NoSuchBeanDefinitionException(requiredType);
    }
  }

  @Override
  public <T> T getBean(Class<T> requiredType, Object... args) throws BeansException {
    if (ObjectUtils.isNotEmpty(args)) {
      throw new UnsupportedOperationException(
              "StaticListableBeanFactory does not support explicit bean creation arguments");
    }
    return getBean(requiredType);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) throws BeansException {
    return getBeanProvider(ResolvableType.forRawClass(requiredType), true);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
    return getBeanProvider(requiredType, true);
  }

  @Override
  public boolean containsBean(String name) {
    return this.beans.containsKey(name);
  }

  @Override
  public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    Object bean = getBean(name);
    // In case of FactoryBean, return singleton status of created object.
    if (bean instanceof FactoryBean<?> factoryBean) {
      return factoryBean.isSingleton();
    }
    return true;
  }

  @Override
  public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
    Object bean = getBean(name);
    // In case of FactoryBean, return prototype status of created object.
    return ((bean instanceof SmartFactoryBean<?> smartFactoryBean && smartFactoryBean.isPrototype()) ||
            (bean instanceof FactoryBean<?> factoryBean && !factoryBean.isSingleton()));
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
  public BeanDefinition getBeanDefinition(String beanName) throws BeansException {
    throw new NoSuchBeanDefinitionException(beanName);
  }

  @Override
  public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
    return getType(name, true);
  }

  @Override
  public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    String beanName = BeanFactoryUtils.transformedBeanName(name);

    Object bean = this.beans.get(beanName);
    if (bean == null) {
      throw new NoSuchBeanDefinitionException(beanName,
              "Defined beans are [" + StringUtils.collectionToCommaDelimitedString(this.beans.keySet()) + "]");
    }

    if (bean instanceof FactoryBean<?> factoryBean && !BeanFactoryUtils.isFactoryDereference(name)) {
      // If it's a FactoryBean, we want to look at what it creates, not the factory class.
      return factoryBean.getObjectType();
    }
    return bean.getClass();
  }

  @Override
  public String[] getAliases(String name) {
    return new String[0];
  }

  //---------------------------------------------------------------------
  // Implementation of ListableBeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public boolean containsBeanDefinition(String name) {
    return this.beans.containsKey(name);
  }

  @Override
  public int getBeanDefinitionCount() {
    return this.beans.size();
  }

  @Override
  public String[] getBeanDefinitionNames() {
    return StringUtils.toStringArray(this.beans.keySet());
  }

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return Collections.emptyMap();
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
    return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
    return new ObjectProvider<>() {
      @Override
      public T get() throws BeansException {
        Set<String> beanNames = getBeanNamesForType(requiredType);
        if (beanNames.size() == 1) {
          return (T) getBean(CollectionUtils.firstElement(beanNames), requiredType);
        }
        else if (beanNames.size() > 1) {
          throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
        }
        else {
          throw new NoSuchBeanDefinitionException(requiredType);
        }
      }

      @Override
      public T get(Object... args) throws BeansException {
        Set<String> beanNames = getBeanNamesForType(requiredType);
        if (beanNames.size() == 1) {
          return (T) getBean(CollectionUtils.firstElement(beanNames), args);
        }
        else if (beanNames.size() > 1) {
          throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
        }
        else {
          throw new NoSuchBeanDefinitionException(requiredType);
        }
      }

      @Override
      @Nullable
      public T getIfAvailable() throws BeansException {
        Set<String> beanNames = getBeanNamesForType(requiredType);
        if (beanNames.size() == 1) {
          return (T) getBean(CollectionUtils.firstElement(beanNames));
        }
        else if (beanNames.size() > 1) {
          throw new NoUniqueBeanDefinitionException(requiredType, beanNames);
        }
        else {
          return null;
        }
      }

      @Override
      @Nullable
      public T getIfUnique() throws BeansException {
        Set<String> beanNames = getBeanNamesForType(requiredType);
        if (beanNames.size() == 1) {
          return (T) getBean(CollectionUtils.firstElement(beanNames));
        }
        else {
          return null;
        }
      }

      @Override
      public Stream<T> stream() {
        return getBeanNamesForType(requiredType).stream().map(name -> (T) getBean(name));
      }

      @Override
      public Stream<T> orderedStream() {
        return stream().sorted(OrderComparator.INSTANCE);
      }
    };
  }

  @Override
  public Set<String> getBeanNamesForType(@Nullable ResolvableType type) {
    return getBeanNamesForType(type, true, true);
  }

  @Override
  public Set<String> getBeanNamesForType(@Nullable ResolvableType type,
          boolean includeNonSingletons, boolean allowEagerInit) {

    Class<?> resolved = (type != null ? type.resolve() : null);
    boolean isFactoryType = resolved != null && FactoryBean.class.isAssignableFrom(resolved);
    List<String> matches = new ArrayList<>();

    for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
      String beanName = entry.getKey();
      Object beanInstance = entry.getValue();
      if (beanInstance instanceof FactoryBean<?> factoryBean && !isFactoryType) {
        Class<?> objectType = factoryBean.getObjectType();
        if ((includeNonSingletons || factoryBean.isSingleton()) &&
                objectType != null && (type == null || type.isAssignableFrom(objectType))) {
          matches.add(beanName);
        }
      }
      else {
        if (type == null || type.isInstance(beanInstance)) {
          matches.add(beanName);
        }
      }
    }
    return new LinkedHashSet<>(matches);
  }

  @Override
  public Set<String> getBeanNamesForType(@Nullable Class<?> type) {
    return getBeanNamesForType(ResolvableType.forClass(type));
  }

  @Override
  public Set<String> getBeanNamesForType(Class<?> requiredType, boolean includeNonSingletons) {
    return getBeanNamesForType(requiredType, includeNonSingletons, false);
  }

  @Override
  public Set<String> getBeanNamesForType(@Nullable Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
    return getBeanNamesForType(ResolvableType.forClass(type), includeNonSingletons, allowEagerInit);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type) throws BeansException {
    return getBeansOfType(type, true, true);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getBeansOfType(@Nullable Class<T> type, boolean includeNonSingletons, boolean allowEagerInit)
          throws BeansException {

    boolean isFactoryType = (type != null && FactoryBean.class.isAssignableFrom(type));
    Map<String, T> matches = new LinkedHashMap<>();

    for (Map.Entry<String, Object> entry : this.beans.entrySet()) {
      String beanName = entry.getKey();
      Object beanInstance = entry.getValue();
      // Is bean a FactoryBean?
      if (beanInstance instanceof FactoryBean<?> factoryBean && !isFactoryType) {
        // Match object created by FactoryBean.
        Class<?> objectType = factoryBean.getObjectType();
        if ((includeNonSingletons || factoryBean.isSingleton()) &&
                objectType != null && (type == null || type.isAssignableFrom(objectType))) {
          matches.put(beanName, getBean(beanName, type));
        }
      }
      else {
        if (type == null || type.isInstance(beanInstance)) {
          // If type to match is FactoryBean, return FactoryBean itself.
          // Else, return bean instance.
          if (isFactoryType) {
            beanName = FACTORY_BEAN_PREFIX + beanName;
          }
          matches.put(beanName, (T) beanInstance);
        }
      }
    }
    return matches;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getBeansOfType(ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    return getBeansOfType((Class<T>) requiredType.resolve(Object.class), includeNonSingletons, allowEagerInit);
  }

  @Override
  public Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    List<String> results = new ArrayList<>();
    for (String beanName : this.beans.keySet()) {
      if (findAnnotationOnBean(beanName, annotationType) != null) {
        results.add(beanName);
      }
    }
    return new LinkedHashSet<>(results);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType)
          throws BeansException {
    return getBeansWithAnnotation(annotationType, true);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType, boolean includeNonSingletons) throws BeansException {

    Map<String, Object> results = new LinkedHashMap<>();
    for (String beanName : this.beans.keySet()) {
      if (findAnnotationOnBean(beanName, annotationType) != null) {
        results.put(beanName, getBean(beanName));
      }
    }
    return results;
  }

  @Override
  @Nullable
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(String beanName, Class<A> annotationType)
          throws NoSuchBeanDefinitionException {

    return findAnnotationOnBean(beanName, annotationType, true);
  }

  @Override
  @Nullable
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(
          String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
          throws NoSuchBeanDefinitionException {

    Class<?> beanType = getType(beanName, allowFactoryBeanInit);
    return (beanType != null
            ? MergedAnnotations.from(beanType, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY, RepeatableContainers.none())
                    .get(annotationType) : null);
  }

  @Override
  public <A extends Annotation> Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {

    var annotations = new LinkedHashSet<A>();
    Class<?> beanType = getType(beanName, allowFactoryBeanInit);
    if (beanType != null) {
      MergedAnnotations.from(beanType, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
              .stream(annotationType)
              .filter(MergedAnnotation::isPresent)
              .forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
    }
    return annotations;
  }

  @Nullable
  @Override
  public <A extends Annotation> A findSynthesizedAnnotation(String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
    return findAnnotationOnBean(beanName, annotationType)
            .synthesize(MergedAnnotation::isPresent).orElse(null);
  }

  @Override
  public DependencyInjector getInjector() {
    throw new UnsupportedOperationException();
  }
}
