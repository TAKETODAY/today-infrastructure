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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.beans.factory.support;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.BeanClassLoadFailedException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanDefinitionPostProcessor;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanNotOfRequiredTypeException;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.NamedBeanHolder;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.ObjectSupplier;
import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.OrderSourceProvider;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;
import jakarta.inject.Provider;

/**
 * Standard {@link BeanFactory} implementation
 *
 * @author TODAY 2019-03-23 15:00
 */
public class StandardBeanFactory
        extends AbstractAutowireCapableBeanFactory implements ConfigurableBeanFactory, BeanDefinitionRegistry {
  private static final Logger log = LoggerFactory.getLogger(StandardBeanFactory.class);

  @Nullable
  private static final Class<?> injectProviderClass =
          // JSR-330 API not available - Provider interface simply not supported then.
          ClassUtils.load("jakarta.inject.Provider", StandardBeanFactory.class.getClassLoader());

  /** Whether to allow re-registration of a different definition with the same name. */
  private boolean allowBeanDefinitionOverriding = true;

  /** Map of bean definition objects, keyed by bean name */
  private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

  /** List of bean definition names, in registration order. */
  private final LinkedHashSet<String> beanDefinitionNames = new LinkedHashSet<>(256);

  /** List of names of manually registered singletons, in registration order. */
  private final LinkedHashSet<String> manualSingletonNames = new LinkedHashSet<>(16);

  /** Whether to allow eager class loading even for lazy-init beans. */
  private boolean allowEagerClassLoading = true;

  /** Optional OrderComparator for dependency Lists and arrays. */
  @Nullable
  private Comparator<Object> dependencyComparator;

  /** Resolver to use for checking if a bean definition is an autowire candidate. */
  private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

  /** Whether bean definition metadata may be cached for all beans. */
  private volatile boolean configurationFrozen;

  /** Cached array of bean definition names in case of frozen configuration. */
  @Nullable
  private volatile String[] frozenBeanDefinitionNames;

  /** Map of singleton and non-singleton bean names, keyed by dependency type. */
  private final ConcurrentHashMap<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

  /** Map of singleton-only bean names, keyed by dependency type. */
  private final ConcurrentHashMap<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

  /**
   * Create a new StandardBeanFactory.
   */
  public StandardBeanFactory() {
    super();
  }

  /**
   * Create a new StandardBeanFactory with the given parent.
   *
   * @param parentBeanFactory the parent BeanFactory
   */
  public StandardBeanFactory(@Nullable BeanFactory parentBeanFactory) {
    super(parentBeanFactory);
  }

  /**
   * Set whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   * If not, an exception will be thrown. This also applies to overriding aliases.
   * <p>Default is "true".
   *
   * @see #registerBeanDefinition
   * @since 4.0
   */
  public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
    this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
  }

  /**
   * Return whether it should be allowed to override bean definitions by registering
   * a different definition with the same name, automatically replacing the former.
   *
   * @since 4.0
   */
  @Override
  public boolean isAllowBeanDefinitionOverriding() {
    return this.allowBeanDefinitionOverriding;
  }

  /**
   * Set whether the factory is allowed to eagerly load bean classes
   * even for bean definitions that are marked as "lazy-init".
   * <p>Default is "true". Turn this flag off to suppress class loading
   * for lazy-init beans unless such a bean is explicitly requested.
   * In particular, by-type lookups will then simply ignore bean definitions
   * without resolved class name, instead of loading the bean classes on
   * demand just to perform a type check.
   *
   * @see BeanDefinition#setLazyInit
   * @since 4.0
   */
  public void setAllowEagerClassLoading(boolean allowEagerClassLoading) {
    this.allowEagerClassLoading = allowEagerClassLoading;
  }

  /**
   * Return whether the factory is allowed to eagerly load bean classes
   * even for bean definitions that are marked as "lazy-init".
   *
   * @since 4.0
   */
  public boolean isAllowEagerClassLoading() {
    return this.allowEagerClassLoading;
  }

  /**
   * Set a {@link java.util.Comparator} for dependency Lists and arrays.
   *
   * @see OrderComparator
   * @see AnnotationAwareOrderComparator
   * @since 4.0
   */
  public void setDependencyComparator(@Nullable Comparator<Object> dependencyComparator) {
    this.dependencyComparator = dependencyComparator;
  }

  /**
   * Return the dependency comparator for this BeanFactory (may be {@code null}.
   *
   * @since 4.0
   */
  @Nullable
  public Comparator<Object> getDependencyComparator() {
    return this.dependencyComparator;
  }

  /**
   * Set a custom autowire candidate resolver for this BeanFactory to use
   * when deciding whether a bean definition should be considered as a
   * candidate for autowiring.
   *
   * @since 4.0
   */
  public void setAutowireCandidateResolver(AutowireCandidateResolver autowireCandidateResolver) {
    Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver must not be null");
    if (autowireCandidateResolver instanceof BeanFactoryAware) {
      ((BeanFactoryAware) autowireCandidateResolver).setBeanFactory(this);
    }
    this.autowireCandidateResolver = autowireCandidateResolver;
  }

  /**
   * Return the autowire candidate resolver for this BeanFactory (never {@code null}).
   *
   * @since 4.0
   */
  public AutowireCandidateResolver getAutowireCandidateResolver() {
    return this.autowireCandidateResolver;
  }

  //---------------------------------------------------------------------
  // Implementation of DefaultSingletonBeanRegistry
  //---------------------------------------------------------------------

  @Override
  public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
    super.registerSingleton(beanName, singletonObject);
    updateManualSingletonNames(set -> set.add(beanName), set -> !this.beanDefinitionMap.containsKey(beanName));
    clearByTypeCache();
  }

  @Override
  public void destroySingletons() {
    super.destroySingletons();
    updateManualSingletonNames(Set::clear, set -> !set.isEmpty());
    clearByTypeCache();
  }

  @Override
  public void removeSingleton(String name) {
    super.removeSingleton(name);
    manualSingletonNames.remove(name);
  }

  @Override
  public void destroySingleton(String beanName) {
    super.destroySingleton(beanName);
    removeManualSingletonName(beanName);
    clearByTypeCache();
  }

  /**
   * Only allows alias overriding if bean definition overriding is allowed.
   */
  @Override
  protected boolean allowAliasOverriding() {
    return isAllowBeanDefinitionOverriding();
  }

  /**
   * Also checks for an alias overriding a bean definition of the same name.
   */
  @Override
  protected void checkForAliasCircle(String name, String alias) {
    super.checkForAliasCircle(name, alias);
    if (!isAllowBeanDefinitionOverriding() && containsBeanDefinition(alias)) {
      throw new IllegalStateException("Cannot register alias '" + alias +
              "' for name '" + name + "': Alias would override bean definition '" + alias + "'");
    }
  }

  @Override
  public void freezeConfiguration() {
    this.configurationFrozen = true;
    this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
  }

  @Override
  public void clearMetadataCache() {
    clearByTypeCache();
  }

  private void removeManualSingletonName(String beanName) {
    updateManualSingletonNames(set -> set.remove(beanName), set -> set.contains(beanName));
  }

  /**
   * Update the factory's internal set of manual singleton names.
   *
   * @param action the modification action
   * @param condition a precondition for the modification action
   * (if this condition does not apply, the action can be skipped)
   */
  private void updateManualSingletonNames(Consumer<Set<String>> action, Predicate<Set<String>> condition) {
    // Still in startup registration phase
    if (condition.test(this.manualSingletonNames)) {
      action.accept(this.manualSingletonNames);
    }
  }

  /**
   * Remove any assumptions about by-type mappings.
   */
  private void clearByTypeCache() {
    this.allBeanNamesByType.clear();
    this.singletonBeanNamesByType.clear();
  }

  @Override
  public boolean isConfigurationFrozen() {
    return this.configurationFrozen;
  }

  /**
   * Considers all beans as eligible for metadata caching
   * if the factory's configuration has been marked as frozen.
   *
   * @see #freezeConfiguration()
   */
  @Override
  protected boolean isBeanEligibleForMetadataCaching(String beanName) {
    return this.configurationFrozen || super.isBeanEligibleForMetadataCaching(beanName);
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistry interface @since 4.0
  //---------------------------------------------------------------------

  @Override
  public Map<String, BeanDefinition> getBeanDefinitions() {
    return beanDefinitionMap;
  }

  @Override
  public void registerBeanDefinition(BeanDefinition def) {
    registerBeanDefinition(def.getBeanName(), def);
  }

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition def) {
    if (def.getBeanName() == null) {
      def.setBeanName(beanName);
    }
    try {
      def.validate();
    }
    catch (BeanDefinitionValidationException ex) {
      throw new BeanDefinitionStoreException(def.getResourceDescription(), beanName,
              "Validation of bean definition failed", ex);
    }

    BeanDefinition existBeanDef = getBeanDefinition(beanName);
    if (existBeanDef != null) {
      if (!isAllowBeanDefinitionOverriding()) {
        throw new BeanDefinitionOverrideException(beanName, def, existBeanDef);
      }
      else if (existBeanDef.getRole() < def.getRole()) {
        // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
        if (log.isInfoEnabled()) {
          log.info("Overriding user-defined bean definition for bean '{}' with a " +
                  "framework-generated bean definition: replacing [{}] with [{}]", beanName, existBeanDef, def);
        }
      }
      else if (!def.equals(existBeanDef)) {
        if (log.isDebugEnabled()) {
          log.debug("Overriding bean definition for bean '{}' with a different definition: replacing [{}] with [{}]",
                  beanName, existBeanDef, def);
        }
      }
      else {
        if (log.isTraceEnabled()) {
          log.trace("Overriding bean definition for bean '{}' with an equivalent definition: replacing [{}] with [{}]",
                  beanName, existBeanDef, def);
        }
      }
      this.beanDefinitionMap.put(beanName, def);
    }
    else {
      if (isAlias(beanName)) {
        if (!isAllowBeanDefinitionOverriding()) {
          String aliasedName = canonicalName(beanName);
          if (containsBeanDefinition(aliasedName)) {  // alias for existing bean definition
            throw new BeanDefinitionOverrideException(
                    beanName, def, getBeanDefinition(aliasedName));
          }
          else {  // alias pointing to non-existing bean definition
            throw new BeanDefinitionStoreException(def.getResourceDescription(), beanName,
                    "Cannot register bean definition for bean '" + beanName +
                            "' since there is already an alias for bean '" + aliasedName + "' bound.");
          }
        }
        else {
          removeAlias(beanName);
        }
      }

      beanDefinitionMap.put(beanName, def);
      beanDefinitionNames.add(beanName);
      this.frozenBeanDefinitionNames = null;
    }

    if (def.hasAliases()) {
      for (String alias : def.getAliases()) {
        registerAlias(beanName, alias);
      }
    }

    if (existBeanDef != null || containsSingleton(beanName)) {
      resetBeanDefinition(beanName);
    }
    else if (isConfigurationFrozen()) {
      clearByTypeCache();
    }
  }

  @Override
  public void removeBeanDefinition(String beanName) {
    Assert.hasText(beanName, "'beanName' must not be empty");
    BeanDefinition removed = beanDefinitionMap.remove(beanName);
    if (removed == null) {
      log.trace("No bean named '{}' found in {}", beanName, this);
    }
    beanDefinitionNames.remove(beanName);
    resetBeanDefinition(beanName);
    this.frozenBeanDefinitionNames = null;
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) {
    return beanDefinitionMap.get(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(Class<?> requiredType) {
    Set<String> candidateNames = getBeanNamesForType(requiredType, true, false);
    int size = candidateNames.size();
    if (size == 1) {
      return getBeanDefinition(candidateNames.iterator().next());
    }
    else if (size > 1) {
      Map<String, Object> candidates = CollectionUtils.newLinkedHashMap(size);
      for (String beanName : candidateNames) {
        if (containsSingleton(beanName)) {
          Object beanInstance = getBean(beanName);
          candidates.put(beanName, beanInstance);
        }
        else {
          candidates.put(beanName, getType(beanName));
        }
      }

      String candidateName = determinePrimaryCandidate(candidates, requiredType);
      if (candidateName == null) {
        candidateName = determineHighestPriorityCandidate(candidates, requiredType);
      }

      if (candidateName != null) {
        return getBeanDefinition(candidateName);
      }

      // fall
      throw new NoUniqueBeanDefinitionException(requiredType, candidateNames);
    }
    return null;
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type) {
    return getBeanDefinition(type) != null;
  }

  @Override
  public boolean containsBeanDefinition(Class<?> type, boolean equals) {
    if (equals) {
      for (String name : getBeanNamesForType(type, true, false)) {
        Class<?> type1 = getType(name);
        if (type1 == type) {
          return true;
        }
      }
      return false;
    }
    return getBeanDefinition(type) != null;
  }

  @Override
  public boolean containsBeanDefinition(String beanName, Class<?> type) {
    return containsBeanDefinition(beanName) && containsBeanDefinition(type);
  }

  @Override
  public boolean containsBeanDefinition(String beanName) {
    return beanDefinitionMap.containsKey(beanName);
  }

  @Override
  public String[] getBeanDefinitionNames() {
    String[] frozenNames = this.frozenBeanDefinitionNames;
    if (frozenNames != null) {
      return frozenNames.clone();
    }
    else {
      return StringUtils.toStringArray(this.beanDefinitionNames);
    }
  }

  @Override
  public Iterator<String> getBeanNamesIterator() {
    return beanDefinitionMap.keySet().iterator();
  }

  @Override
  public int getBeanDefinitionCount() {
    return beanDefinitionMap.size();
  }

  //---------------------------------------------------------------------
  // Implementation of BeanFactory interface
  //---------------------------------------------------------------------

  @Override
  public <T> T getBean(Class<T> requiredType) throws BeansException {
    return getBean(requiredType, (Object[]) null);
  }

  @Override
  public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
    Assert.notNull(requiredType, "Required type must not be null");
    return resolveBean(ResolvableType.fromRawClass(requiredType), args, false);
  }

  @Nullable
  private <T> T resolveBean(
          ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
    NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
    if (namedBean != null) {
      return namedBean.getBeanInstance();
    }

    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof StandardBeanFactory) {
      return ((StandardBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
    }
    else if (parent != null) {
      ObjectSupplier<T> parentProvider = parent.getObjectSupplier(requiredType);
      if (args != null) {
        return parentProvider.get(args);
      }
      else {
        return nonUniqueAsNull ? parentProvider.getIfUnique() : parentProvider.getIfAvailable();
      }
    }
    return null;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> NamedBeanHolder<T> resolveNamedBean(
          ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) throws BeansException {
    Assert.notNull(requiredType, "Required type must not be null");
    Set<String> candidateNames = getBeanNamesForType(requiredType);

    int size = candidateNames.size();
    if (size > 1) {
      LinkedHashSet<String> autowireCandidates = new LinkedHashSet<>(size);
      for (String beanName : candidateNames) {
        BeanDefinition beanDefinition = getBeanDefinition(beanName);
        if (beanDefinition == null || beanDefinition.isAutowireCandidate()) {
          autowireCandidates.add(beanName);
        }
      }
      if (!autowireCandidates.isEmpty()) {
        candidateNames = autowireCandidates;
      }
    }

    size = candidateNames.size();
    if (size == 1) {
      return resolveNamedBean(candidateNames.iterator().next(), requiredType, args);
    }
    else if (size > 1) {
      Map<String, Object> candidates = CollectionUtils.newLinkedHashMap(size);
      for (String beanName : candidateNames) {
        if (containsSingleton(beanName) && args == null) {
          Object beanInstance = getBean(beanName);
          candidates.put(beanName, beanInstance);
        }
        else {
          candidates.put(beanName, getType(beanName));
        }
      }
      String candidateName = determinePrimaryCandidate(candidates, requiredType.toClass());
      if (candidateName == null) {
        candidateName = determineHighestPriorityCandidate(candidates, requiredType.toClass());
      }
      if (candidateName != null) {
        Object beanInstance = candidates.get(candidateName);
        if (beanInstance == null) {
          return null;
        }
        if (beanInstance instanceof Class) {
          return resolveNamedBean(candidateName, requiredType, args);
        }
        return new NamedBeanHolder<>(candidateName, (T) beanInstance);
      }
      if (!nonUniqueAsNull) {
        throw new NoUniqueBeanDefinitionException(requiredType, candidates.keySet());
      }
    }

    return null;
  }

  @Nullable
  private <T> NamedBeanHolder<T> resolveNamedBean(
          String beanName, ResolvableType requiredType, Object[] args) throws BeansException {
    Object bean = doGetBean(beanName, null, args, false);
    if (bean == null) {
      return null;
    }
    return new NamedBeanHolder<>(beanName, adaptBeanInstance(beanName, bean, requiredType.toClass()));
  }

  @Override
  public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
    Assert.notNull(requiredType, "Required type must not be null");
    NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.fromClass(requiredType), null, false);
    if (namedBean != null) {
      return namedBean;
    }
    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof AutowireCapableBeanFactory) {
      return ((AutowireCapableBeanFactory) parent).resolveNamedBean(requiredType);
    }
    throw new NoSuchBeanDefinitionException(requiredType);
  }

  //---------------------------------------------------------------------
  // Listing Get operations for type-lookup
  //---------------------------------------------------------------------

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType) {
    return getObjectSupplier(requiredType, true);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType) {
    return getObjectSupplier(requiredType, true);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(Class<T> requiredType, boolean allowEagerInit) {
    Assert.notNull(requiredType, "Required type must not be null");
    return getObjectSupplier(ResolvableType.fromRawClass(requiredType), allowEagerInit);
  }

  @Override
  public <T> ObjectSupplier<T> getObjectSupplier(ResolvableType requiredType, boolean allowEagerInit) {
    return new BeanObjectSupplier<>() {
      @Override
      public T get() throws BeansException {
        T resolved = resolveBean(requiredType, null, false);
        if (resolved == null) {
          throw new NoSuchBeanDefinitionException(requiredType);
        }
        return resolved;
      }

      @Override
      public T get(Object... args) throws BeansException {
        T resolved = resolveBean(requiredType, args, false);
        if (resolved == null) {
          throw new NoSuchBeanDefinitionException(requiredType);
        }
        return resolved;
      }

      @Override
      @Nullable
      public T getIfAvailable() throws BeansException {
        try {
          return resolveBean(requiredType, null, false);
        }
        catch (ScopeNotActiveException ex) {
          // Ignore resolved bean in non-active scope
          return null;
        }
      }

      @Override
      public void ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
        T dependency = getIfAvailable();
        if (dependency != null) {
          try {
            dependencyConsumer.accept(dependency);
          }
          catch (ScopeNotActiveException ex) {
            // Ignore resolved bean in non-active scope, even on scoped proxy invocation
          }
        }
      }

      @Override
      @Nullable
      public T getIfUnique() throws BeansException {
        try {
          return resolveBean(requiredType, null, true);
        }
        catch (ScopeNotActiveException ex) {
          // Ignore resolved bean in non-active scope
          return null;
        }
      }

      @Override
      public void ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
        T dependency = getIfUnique();
        if (dependency != null) {
          try {
            dependencyConsumer.accept(dependency);
          }
          catch (ScopeNotActiveException ex) {
            // Ignore resolved bean in non-active scope, even on scoped proxy invocation
          }
        }
      }

      @SuppressWarnings("unchecked")
      @Override
      public Stream<T> stream() {
        return getBeanNamesForTypedStream(requiredType, allowEagerInit)
                .stream()
                .map(name -> (T) getBean(name))
                .filter(Objects::nonNull);
      }

      @SuppressWarnings("unchecked")
      @Override
      public Stream<T> orderedStream() {
        Set<String> beanNames = getBeanNamesForTypedStream(requiredType, allowEagerInit);
        if (beanNames.isEmpty()) {
          return Stream.empty();
        }
        Map<String, T> matchingBeans = CollectionUtils.newLinkedHashMap(beanNames.size());
        for (String beanName : beanNames) {
          Object beanInstance = getBean(beanName);
          if (beanInstance != null) {
            matchingBeans.put(beanName, (T) beanInstance);
          }
        }
        Stream<T> stream = matchingBeans.values().stream();
        return stream.sorted(adaptOrderComparator(matchingBeans));
      }
    };
  }

  private Set<String> getBeanNamesForTypedStream(ResolvableType requiredType, boolean allowEagerInit) {
    return BeanFactoryUtils.beanNamesForTypeIncludingAncestors(this, requiredType, true, allowEagerInit);
  }

  private Comparator<Object> adaptOrderComparator(Map<String, ?> matchingBeans) {
    Comparator<Object> dependencyComparator = getDependencyComparator();
    OrderComparator comparator =
            dependencyComparator instanceof OrderComparator
            ? (OrderComparator) dependencyComparator : OrderComparator.INSTANCE;
    return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
  }

  private OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
    return new FactoryAwareOrderSourceProvider(this, beans);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(
          Class<T> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    return getBeansOfType(ResolvableType.fromRawClass(requiredType), includeNonSingletons, allowEagerInit);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getBeansOfType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    Set<String> beanNames = getBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);
    Map<String, T> beans = CollectionUtils.newLinkedHashMap(beanNames.size());
    for (String beanName : beanNames) {
      try {
        Object beanInstance = getBean(beanName);
        if (beanInstance != null) {
          beans.put(beanName, (T) beanInstance);
        }
      }
      catch (BeanCreationException ex) {
        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause instanceof BeanCurrentlyInCreationException bce) {
          String exBeanName = bce.getBeanName();
          if (exBeanName != null && isCurrentlyInCreation(exBeanName)) {
            if (log.isTraceEnabled()) {
              log.trace("Ignoring match to currently created bean '{}': ",
                      exBeanName, ex.getMessage());
            }
            onSuppressedException(ex);
            // Ignore: indicates a circular reference when autowiring constructors.
            // We want to find matches other than the currently created bean itself.
            continue;
          }
        }
        throw ex;
      }
    }
    return beans;
  }

  // getBeanNamesOfType

  @Override
  public Set<String> getBeanNamesForType(Class<?> requiredType, boolean includeNonSingletons) {
    return getBeanNamesForType(requiredType, true, includeNonSingletons);
  }

  @Override
  public Set<String> getBeanNamesForType(
          Class<?> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {

    if (!isConfigurationFrozen() || requiredType == null || !allowEagerInit) {
      return doGetBeanNamesForType(
              ResolvableType.fromRawClass(requiredType), includeNonSingletons, allowEagerInit);
    }

    Map<Class<?>, String[]> cache =
            includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType;
    String[] resolvedBeanNames = cache.get(requiredType);
    if (resolvedBeanNames != null) {
      return Set.of(resolvedBeanNames);
    }

    Set<String> resolvedBeanNamesSet = doGetBeanNamesForType(
            ResolvableType.fromRawClass(requiredType), includeNonSingletons, true);
    if (ClassUtils.isCacheSafe(requiredType, getBeanClassLoader())) {
      cache.put(requiredType, StringUtils.toStringArray(resolvedBeanNamesSet));
    }
    return resolvedBeanNamesSet;
  }

  @Override
  public Set<String> getBeanNamesForType(ResolvableType type) {
    return getBeanNamesForType(type, true, true);
  }

  @Override
  public Set<String> getBeanNamesForType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    Class<?> resolved = requiredType.resolve();
    if (resolved != null && !requiredType.hasGenerics()) {
      return getBeanNamesForType(resolved, includeNonSingletons, allowEagerInit);
    }
    else {
      return doGetBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);
    }
  }

  private Set<String> doGetBeanNamesForType(
          ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    LinkedHashSet<String> beanNames = new LinkedHashSet<>();

    // 1. Check all bean definitions.
    for (String beanName : beanDefinitionNames) {
      // Only consider bean as eligible if the bean name is not defined as alias for some other bean.
      if (!isAlias(beanName)) {
        try {
          BeanDefinition definition = beanDefinitionMap.get(beanName);
          // Only check bean definition if it is complete.
          if (allowEagerInit || allowCheck(definition)) {
            boolean matchFound = false;
            boolean allowFactoryBeanInit = allowEagerInit || containsSingleton(beanName);
            if (isFactoryBean(definition)) {
              if (includeNonSingletons || (allowFactoryBeanInit && isSingleton(beanName))) {
                matchFound = isTypeMatch(beanName, requiredType, allowFactoryBeanInit);
              }
              if (!matchFound) {
                // In case of FactoryBean, try to match FactoryBean instance itself next.
                beanName = FACTORY_BEAN_PREFIX + beanName;
                matchFound = isTypeMatch(beanName, requiredType, allowFactoryBeanInit);
              }
            }
            else {
              if (includeNonSingletons || isSingleton(beanName)) {
                matchFound = isTypeMatch(beanName, requiredType, allowFactoryBeanInit);
              }
            }
            if (matchFound) {
              beanNames.add(beanName);
            }
          }
        }
        catch (BeanClassLoadFailedException | BeanDefinitionStoreException ex) {
          if (allowEagerInit) {
            throw ex;
          }
          // Probably a placeholder: let's ignore it for type matching purposes.
          LogMessage message =
                  (ex instanceof BeanClassLoadFailedException
                   ? LogMessage.format("Ignoring bean class loading failure for bean '{}'", beanName)
                   : LogMessage.format("Ignoring unresolvable metadata in bean definition '{}'", beanName));
          log.trace(message, ex);
          // Register exception, in case the bean was accidentally unresolvable.
          onSuppressedException(ex);
        }
        catch (NoSuchBeanDefinitionException ex) {
          // Bean definition got removed while we were iterating -> ignore.
        }
      }
    }

    // 2. Check manually registered singletons too.
    for (String beanName : this.manualSingletonNames) {
      if (beanNames.contains(beanName)) {
        continue;
      }
      try {
        // In case of FactoryBean, match object created by FactoryBean.
        if (isFactoryBean(beanName)) {
          if ((includeNonSingletons || isSingleton(beanName)) && isTypeMatch(beanName, requiredType)) {
            beanNames.add(beanName);
            // Match found for this bean: do not match FactoryBean itself anymore.
            continue;
          }
          // In case of FactoryBean, try to match FactoryBean itself next.
          beanName = FACTORY_BEAN_PREFIX + beanName;
        }
        // Match raw bean instance (might be raw FactoryBean).
        if (isTypeMatch(beanName, requiredType)) {
          beanNames.add(beanName);
        }
      }
      catch (NoSuchBeanDefinitionException ex) {
        // Shouldn't happen - probably a result of circular reference resolution...
        log.trace("Failed to check manually registered singleton with name '{}'", beanName, ex);
      }
    }
    return beanNames;
  }

  private boolean allowCheck(BeanDefinition definition) {
    return (
            definition.hasBeanClass()
                    || !definition.isLazyInit()
                    || isAllowEagerClassLoading()
    ) && !requiresEagerInitForType(definition.getFactoryBeanName());
  }

  /**
   * Check whether the specified bean would need to be eagerly initialized
   * in order to determine its type.
   *
   * @param factoryBeanName a factory-bean reference that the bean definition
   * defines a factory method for
   * @return whether eager initialization is necessary
   */
  private boolean requiresEagerInitForType(@Nullable String factoryBeanName) {
    return factoryBeanName != null && isFactoryBean(factoryBeanName) && !containsSingleton(factoryBeanName);
  }

  @Override
  public Map<String, Object> getBeansWithAnnotation(
          Class<? extends Annotation> annotationType, boolean includeNonSingletons) {
    Assert.notNull(annotationType, "annotationType must not be null");

    Set<String> beanNames = getBeanNamesForAnnotation(annotationType);
    Map<String, Object> result = CollectionUtils.newLinkedHashMap(beanNames.size());
    for (String beanName : beanNames) {
      Object beanInstance = getBean(beanName);
      result.put(beanName, beanInstance);
    }
    return result;
  }

  @Override
  public Set<String> getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
    Assert.notNull(annotationType, "annotationType must not be null");
    LinkedHashSet<String> names = new LinkedHashSet<>();

    for (String beanName : beanDefinitionNames) {
      BeanDefinition bd = beanDefinitionMap.get(beanName);
      if (bd != null && findAnnotationOnBean(beanName, annotationType).isPresent()) {
        names.add(beanName);
      }
    }

    for (String beanName : manualSingletonNames) {
      if (!names.contains(beanName) && findAnnotationOnBean(beanName, annotationType).isPresent()) {
        names.add(beanName);
      }
    }

    return names;
  }

  @Override
  public <A extends Annotation> A findSynthesizedAnnotation(String beanName, Class<A> annotationType) {
    return findAnnotationOnBean(beanName, annotationType)
            .synthesize(MergedAnnotation::isPresent).orElse(null);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(
          String beanName, Class<A> annotationType) throws NoSuchBeanDefinitionException {
    return findAnnotationOnBean(beanName, annotationType, true);
  }

  @Override
  public <A extends Annotation> MergedAnnotation<A> findAnnotationOnBean(
          String beanName, Class<A> annotationType, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {

    // find oon factory-method then find on class

    BeanDefinition definition = getBeanDefinition(beanName);
    if (definition instanceof AnnotatedBeanDefinition annotated) {
      // find on factory method
      MethodMetadata methodMetadata = annotated.getFactoryMethodMetadata();
      MergedAnnotation<A> annotation;
      if (methodMetadata != null) {
        annotation = methodMetadata.getAnnotation(annotationType);
      }
      else {
        annotation = annotated.getMetadata().getAnnotation(annotationType);
      }
      if (annotation.isPresent()) {
        return annotation;
      }
    }
    else if (definition != null) {
      String factoryMethodName = definition.getFactoryMethodName();
      if (factoryMethodName != null) {
        Class<?> factoryClass = getFactoryClass(definition);
        Method factoryMethod = getFactoryMethod(definition, factoryClass, factoryMethodName);
        if (factoryMethod != null) {
          MergedAnnotation<A> annotation =
                  MergedAnnotations.from(factoryMethod, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
          if (annotation.isPresent()) {
            return annotation;
          }
        }
      }
    }

    // find it on class
    Class<?> beanType = getType(beanName, allowFactoryBeanInit);
    if (beanType != null) {
      MergedAnnotation<A> annotation =
              MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
      if (annotation.isPresent()) {
        return annotation;
      }
    }

    // Check raw bean class, e.g. in case of a proxy.
    if (definition != null && definition.hasBeanClass()) {
      Class<?> beanClass = definition.getBeanClass();
      if (beanClass != beanType) {
        MergedAnnotation<A> annotation =
                MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
        if (annotation.isPresent()) {
          return annotation;
        }
      }
    }
    // missing
    return MergedAnnotation.missing();
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurableBeanFactory interface @since 4.0
  //---------------------------------------------------------------------

  @Override
  public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
    super.copyConfigurationFrom(otherFactory);
    if (otherFactory instanceof StandardBeanFactory std) {
      this.dependencyComparator = std.dependencyComparator;
      this.allowEagerClassLoading = std.allowEagerClassLoading;
      this.allowBeanDefinitionOverriding = std.allowBeanDefinitionOverriding;
      // A clone of the AutowireCandidateResolver since it is potentially BeanFactoryAware
      setAutowireCandidateResolver(std.getAutowireCandidateResolver().cloneIfNecessary());
    }
  }

  /**
   * Reset all bean definition caches for the given bean,
   * including the caches of beans that are derived from it.
   * <p>Called after an existing bean definition has been replaced or removed,
   * triggering {@link #destroySingleton} and {@link BeanDefinitionPostProcessor#resetBeanDefinition}
   * on the given bean.
   *
   * @param beanName the name of the bean to reset
   * @see #registerBeanDefinition
   * @see #removeBeanDefinition
   */
  protected void resetBeanDefinition(String beanName) {
    // Remove corresponding bean from singleton cache, if any. Shouldn't usually
    // be necessary, rather just meant for overriding a context's default beans
    destroySingleton(beanName);

    // Notify all post-processors that the specified bean definition has been reset.
    for (BeanDefinitionPostProcessor processor : postProcessors().definitions) {
      processor.resetBeanDefinition(beanName);
    }
  }

  public boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor)
          throws NoSuchBeanDefinitionException {

    return isAutowireCandidate(beanName, descriptor, getAutowireCandidateResolver());
  }

  /**
   * Determine whether the specified bean definition qualifies as an autowire candidate,
   * to be injected into other beans which declare a dependency of matching type.
   *
   * @param beanName the name of the bean definition to check
   * @param descriptor the descriptor of the dependency to resolve
   * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
   * @return whether the bean should be considered as autowire candidate
   */
  protected boolean isAutowireCandidate(
          String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
          throws NoSuchBeanDefinitionException {

    String bdName = BeanFactoryUtils.transformedBeanName(beanName);
    BeanDefinition definition = getBeanDefinition(bdName);
    if (definition != null) {
      return isAutowireCandidate(beanName, definition, descriptor, resolver);
    }
    else if (containsSingleton(beanName)) {
      return isAutowireCandidate(
              beanName, new BeanDefinition(beanName, getType(beanName)), descriptor, resolver);
    }

    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof StandardBeanFactory) {
      // No bean definition found in this factory -> delegate to parent.
      return ((StandardBeanFactory) parent).isAutowireCandidate(beanName, descriptor, resolver);
    }
    else if (parent instanceof AutowireCapableBeanFactory) {
      // If no StandardBeanFactory, can't pass the resolver along.
      return ((AutowireCapableBeanFactory) parent).isAutowireCandidate(beanName, descriptor);
    }
    else {
      return true;
    }
  }

  /**
   * Determine whether the specified bean definition qualifies as an autowire candidate,
   * to be injected into other beans which declare a dependency of matching type.
   *
   * @param beanName the name of the bean definition to check
   * @param definition the bean definition to check
   * @param descriptor the descriptor of the dependency to resolve
   * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
   * @return whether the bean should be considered as autowire candidate
   */
  protected boolean isAutowireCandidate(
          String beanName, BeanDefinition definition,
          DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

    resolveBeanClass(definition);
    if (definition.isFactoryMethodUnique && definition.factoryMethodToIntrospect == null) {
      new ConstructorResolver(this).resolveFactoryMethodIfPossible(definition);
    }

    String bdName = BeanFactoryUtils.transformedBeanName(beanName);
    if (!beanName.equals(bdName)) {
      definition = definition.cloneDefinition();
      definition.setBeanName(beanName);
      definition.setAliases(getAliases(bdName));
    }
    return resolver.isAutowireCandidate(definition, descriptor);
  }

  @Nullable
  @Override
  public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName) throws BeansException {
    return resolveDependency(descriptor, requestingBeanName, null, null);
  }

  @Nullable
  @Override
  public Object resolveDependency(DependencyDescriptor descriptor, @Nullable String requestingBeanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {

    descriptor.initParameterNameDiscovery(getParameterNameDiscoverer());

    Class<?> dependencyType = descriptor.getDependencyType();
    if (Optional.class == dependencyType) {
      return createOptionalDependency(descriptor, requestingBeanName);
    }
    else if (Supplier.class == dependencyType
            || ObjectProvider.class == dependencyType
            || ObjectSupplier.class == dependencyType) {
      return new DependencyObjectProvider(descriptor, requestingBeanName);
    }
    else if (injectProviderClass == dependencyType) {
      return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
    }
    else {
      Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(
              descriptor, requestingBeanName);
      if (result == null) {
        result = doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
      }
      return result;
    }
  }

  @Nullable
  public Object doResolveDependency(
          DependencyDescriptor descriptor, @Nullable String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
    InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
    try {
      Object shortcut = descriptor.resolveShortcut(this);
      if (shortcut != null) {
        return shortcut;
      }

      Class<?> type = descriptor.getDependencyType();
      Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
      if (value != null) {
        if (value instanceof String) {
          String strVal = resolveEmbeddedValue((String) value);
          BeanDefinition bd = beanName != null && containsBean(beanName)
                              ? obtainLocalBeanDefinition(beanName) : null;
          value = evaluateBeanDefinitionString(strVal, bd);
        }
        TypeConverter converter = typeConverter != null ? typeConverter : getTypeConverter();
        try {
          return converter.convertIfNecessary(value, type, descriptor.getTypeDescriptor());
        }
        catch (UnsupportedOperationException ex) {
          // A custom TypeConverter which does not support TypeDescriptor resolution...
          return descriptor.getField() != null
                 ? converter.convertIfNecessary(value, type, descriptor.getField())
                 : converter.convertIfNecessary(value, type, descriptor.getMethodParameter());
        }
      }

      Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
      if (multipleBeans != null) {
        return multipleBeans;
      }

      Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
      if (matchingBeans.isEmpty()) {
        if (isRequired(descriptor)) {
          raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
        }
        return null;
      }

      String autowiredBeanName;
      Object instanceCandidate;

      if (matchingBeans.size() > 1) {
        autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
        if (autowiredBeanName == null) {
          if (isRequired(descriptor) || !indicatesMultipleBeans(type)) {
            return descriptor.resolveNotUnique(descriptor.getResolvableType(), matchingBeans);
          }
          else {
            // In case of an optional Collection/Map, silently ignore a non-unique case:
            // possibly it was meant to be an empty collection of multiple regular beans
            // (before 4.3 in particular when we didn't even look for collection beans).
            return null;
          }
        }
        instanceCandidate = matchingBeans.get(autowiredBeanName);
      }
      else {
        // We have exactly one match.
        Map.Entry<String, Object> entry = matchingBeans.entrySet().iterator().next();
        autowiredBeanName = entry.getKey();
        instanceCandidate = entry.getValue();
      }

      if (autowiredBeanNames != null) {
        autowiredBeanNames.add(autowiredBeanName);
      }
      if (instanceCandidate instanceof Class) {
        instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
      }
      Object result = instanceCandidate;
      if (result == null) {
        if (isRequired(descriptor)) {
          raiseNoMatchingBeanFound(type, descriptor.getResolvableType(), descriptor);
        }
      }
      if (!ClassUtils.isAssignableValue(type, result)) {
        throw new BeanNotOfRequiredTypeException(
                autowiredBeanName, type, instanceCandidate != null ? instanceCandidate.getClass() : NullValue.class);
      }
      return result;
    }
    finally {
      ConstructorResolver.setCurrentInjectionPoint(previousInjectionPoint);
    }
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> T convertIfNecessary(
          @Nullable Object bean, @Nullable Class<?> requiredType, @Nullable TypeConverter converter) {
    // Check if required type matches the type of the actual bean instance.
    if (bean != null && requiredType != null && !ClassUtils.isAssignableValue(requiredType, bean)) {
      if (converter == null) {
        converter = getTypeConverter();
      }
      bean = converter.convertIfNecessary(bean, requiredType);
    }
    return (T) bean;
  }

  @Nullable
  private Object resolveMultipleBeans(DependencyDescriptor descriptor, @Nullable String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

    Class<?> type = descriptor.getDependencyType();

    if (descriptor instanceof StreamDependencyDescriptor) {
      Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
      if (autowiredBeanNames != null) {
        autowiredBeanNames.addAll(matchingBeans.keySet());
      }
      Stream<Object> stream = matchingBeans.keySet().stream()
              .map(name -> descriptor.resolveCandidate(name, type, this))
              .filter(Objects::nonNull);
      if (((StreamDependencyDescriptor) descriptor).isOrdered()) {
        stream = stream.sorted(adaptOrderComparator(matchingBeans));
      }
      return stream;
    }
    else if (type.isArray()) {
      Class<?> componentType = type.getComponentType();
      ResolvableType resolvableType = descriptor.getResolvableType();
      Class<?> resolvedArrayType = resolvableType.resolve(type);
      if (resolvedArrayType != type) {
        componentType = resolvableType.getComponentType().resolve();
      }
      if (componentType == null) {
        return null;
      }
      var matchingBeans = findAutowireCandidates(beanName, componentType, new MultiElementDescriptor(descriptor));
      if (matchingBeans.isEmpty()) {
        return null;
      }
      if (autowiredBeanNames != null) {
        autowiredBeanNames.addAll(matchingBeans.keySet());
      }
      Object result = convertIfNecessary(matchingBeans.values(), resolvedArrayType, typeConverter);
      if (result instanceof Object[]) {
        Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
        if (comparator != null) {
          Arrays.sort((Object[]) result, comparator);
        }
      }
      return result;
    }
    else if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
      Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
      if (elementType == null) {
        return null;
      }
      var matchingBeans = findAutowireCandidates(beanName, elementType, new MultiElementDescriptor(descriptor));
      if (matchingBeans.isEmpty()) {
        return null;
      }
      if (autowiredBeanNames != null) {
        autowiredBeanNames.addAll(matchingBeans.keySet());
      }
      Object result = convertIfNecessary(matchingBeans.values(), type, typeConverter);
      if (result instanceof List<?> list) {
        if (list.size() > 1) {
          Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
          if (comparator != null) {
            list.sort(comparator);
          }
        }
      }
      return result;
    }
    else if (Map.class == type) {
      ResolvableType mapType = descriptor.getResolvableType().asMap();
      Class<?> keyType = mapType.resolveGeneric(0);
      if (String.class != keyType) {
        return null;
      }
      Class<?> valueType = mapType.resolveGeneric(1);
      if (valueType == null) {
        return null;
      }
      var matchingBeans = findAutowireCandidates(beanName, valueType, new MultiElementDescriptor(descriptor));
      if (matchingBeans.isEmpty()) {
        return null;
      }
      if (autowiredBeanNames != null) {
        autowiredBeanNames.addAll(matchingBeans.keySet());
      }
      return matchingBeans;
    }
    else {
      return null;
    }
  }

  private boolean isRequired(DependencyDescriptor descriptor) {
    return getAutowireCandidateResolver().isRequired(descriptor);
  }

  private boolean indicatesMultipleBeans(Class<?> type) {
    return (type.isArray() || (type.isInterface() &&
            (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))));
  }

  @Nullable
  private Comparator<Object> adaptDependencyComparator(Map<String, ?> matchingBeans) {
    Comparator<Object> comparator = getDependencyComparator();
    if (comparator instanceof OrderComparator) {
      return ((OrderComparator) comparator).withSourceProvider(
              createFactoryAwareOrderSourceProvider(matchingBeans));
    }
    else {
      return comparator;
    }
  }

  /**
   * Find bean instances that match the required type.
   * Called during autowiring for the specified bean.
   *
   * @param beanName the name of the bean that is about to be wired
   * @param requiredType the actual type of bean to look for
   * (may be an array component type or collection element type)
   * @param descriptor the descriptor of the dependency to resolve
   * @return a Map of candidate names and candidate instances that match
   * the required type (never {@code null})
   * @throws BeansException in case of errors
   */
  protected Map<String, Object> findAutowireCandidates(
          @Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {

    Set<String> candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
            this, requiredType, true, descriptor.isEager());

    Map<String, Object> result = CollectionUtils.newLinkedHashMap(candidateNames.size());
    for (Map.Entry<Class<?>, Object> classObjectEntry : this.objectFactories.entrySet()) {
      Class<?> autowiringType = classObjectEntry.getKey();
      if (autowiringType.isAssignableFrom(requiredType)) {
        Object autowiringValue = AutowireUtils.resolveAutowiringValue(classObjectEntry.getValue(), requiredType);
        if (requiredType.isInstance(autowiringValue)) {
          result.put(ObjectUtils.identityToString(autowiringValue), autowiringValue);
          break;
        }
      }
    }
    for (String candidate : candidateNames) {
      if (!isSelfReference(beanName, candidate) && isAutowireCandidate(candidate, descriptor)) {
        addCandidateEntry(result, candidate, descriptor, requiredType);
      }
    }
    if (result.isEmpty()) {
      boolean multiple = indicatesMultipleBeans(requiredType);
      // Consider fallback matches if the first pass failed to find anything...
      DependencyDescriptor fallbackDescriptor = descriptor.forFallbackMatch();
      for (String candidate : candidateNames) {
        if (!isSelfReference(beanName, candidate)
                && isAutowireCandidate(candidate, fallbackDescriptor)
                && (!multiple || getAutowireCandidateResolver().hasQualifier(descriptor))) {
          addCandidateEntry(result, candidate, descriptor, requiredType);
        }
      }
      if (result.isEmpty() && !multiple) {
        // Consider self references as a final pass...
        // but in the case of a dependency collection, not the very same bean itself.
        for (String candidate : candidateNames) {
          if (isSelfReference(beanName, candidate)
                  && (!(descriptor instanceof MultiElementDescriptor) || !beanName.equals(candidate))
                  && isAutowireCandidate(candidate, fallbackDescriptor)) {
            addCandidateEntry(result, candidate, descriptor, requiredType);
          }
        }
      }
    }
    return result;
  }

  /**
   * Add an entry to the candidate map: a bean instance if available or just the resolved
   * type, preventing early bean initialization ahead of primary candidate selection.
   */
  private void addCandidateEntry(
          Map<String, Object> candidates, String candidateName,
          DependencyDescriptor descriptor, Class<?> requiredType) {

    if (descriptor instanceof MultiElementDescriptor) {
      Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
      if (beanInstance != null) {
        candidates.put(candidateName, beanInstance);
      }
    }
    else if (containsSingleton(candidateName)
            || (descriptor instanceof StreamDependencyDescriptor streamDescriptor && streamDescriptor.isOrdered())) {
      Object beanInstance = descriptor.resolveCandidate(candidateName, requiredType, this);
      candidates.put(candidateName, beanInstance);
    }
    else {
      candidates.put(candidateName, getType(candidateName));
    }
  }

  /**
   * Determine the autowire candidate in the given set of beans.
   * <p>Looks for {@code @Primary} and {@code @Priority} (in that order).
   *
   * @param candidates a Map of candidate names and candidate instances
   * that match the required type, as returned by {@link #findAutowireCandidates}
   * @param descriptor the target dependency to match against
   * @return the name of the autowire candidate, or {@code null} if none found
   */
  @Nullable
  protected String determineAutowireCandidate(Map<String, Object> candidates, DependencyDescriptor descriptor) {
    Class<?> requiredType = descriptor.getDependencyType();
    String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
    if (primaryCandidate != null) {
      return primaryCandidate;
    }
    String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
    if (priorityCandidate != null) {
      return priorityCandidate;
    }
    // Fallback
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
      String candidateName = entry.getKey();
      Object beanInstance = entry.getValue();
      if ((beanInstance != null && objectFactories.containsValue(beanInstance))
              || matchesBeanName(candidateName, descriptor.getDependencyName())) {
        return candidateName;
      }
    }
    return null;
  }

  /**
   * Determine the primary candidate in the given set of beans.
   *
   * @param candidates a Map of candidate names and candidate instances
   * (or candidate classes if not created yet) that match the required type
   * @param requiredType the target dependency type to match against
   * @return the name of the primary candidate, or {@code null} if none found
   * @see #isPrimary(String, Object)
   */
  @Nullable
  protected String determinePrimaryCandidate(Map<String, Object> candidates, Class<?> requiredType) {
    String primaryBeanName = null;
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
      String candidateBeanName = entry.getKey();
      Object beanInstance = entry.getValue();
      if (isPrimary(candidateBeanName, beanInstance)) {
        if (primaryBeanName != null) {
          boolean candidateLocal = containsBeanDefinition(candidateBeanName);
          boolean primaryLocal = containsBeanDefinition(primaryBeanName);
          if (candidateLocal && primaryLocal) {
            throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                    "more than one 'primary' bean found among candidates: " + candidates.keySet());
          }
          else if (candidateLocal) {
            primaryBeanName = candidateBeanName;
          }
        }
        else {
          primaryBeanName = candidateBeanName;
        }
      }
    }
    return primaryBeanName;
  }

  /**
   * Determine the candidate with the highest priority in the given set of beans.
   * <p>Based on {@code @jakarta.annotation.Priority}. As defined by the related
   * {@link Ordered} interface, the lowest value has
   * the highest priority.
   *
   * @param candidates a Map of candidate names and candidate instances
   * (or candidate classes if not created yet) that match the required type
   * @param requiredType the target dependency type to match against
   * @return the name of the candidate with the highest priority,
   * or {@code null} if none found
   * @see #getPriority(Object)
   */
  @Nullable
  protected String determineHighestPriorityCandidate(Map<String, Object> candidates, Class<?> requiredType) {
    String highestPriorityBeanName = null;
    Integer highestPriority = null;
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
      String candidateBeanName = entry.getKey();
      Object beanInstance = entry.getValue();
      if (beanInstance != null) {
        Integer candidatePriority = getPriority(beanInstance);
        if (candidatePriority != null) {
          if (highestPriorityBeanName != null) {
            if (candidatePriority.equals(highestPriority)) {
              throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                      "Multiple beans found with the same priority ('" + highestPriority +
                              "') among candidates: " + candidates.keySet());
            }
            else if (candidatePriority < highestPriority) {
              highestPriorityBeanName = candidateBeanName;
              highestPriority = candidatePriority;
            }
          }
          else {
            highestPriorityBeanName = candidateBeanName;
            highestPriority = candidatePriority;
          }
        }
      }
    }
    return highestPriorityBeanName;
  }

  /**
   * Return whether the bean definition for the given bean name has been
   * marked as a primary bean.
   *
   * @param beanName the name of the bean
   * @param beanInstance the corresponding bean instance (can be null)
   * @return whether the given bean qualifies as primary
   */
  protected boolean isPrimary(String beanName, Object beanInstance) {
    String transformedBeanName = transformedBeanName(beanName);
    BeanDefinition definition = getBeanDefinition(transformedBeanName);
    if (definition != null) {
      return definition.isPrimary();
    }
    return getParentBeanFactory() instanceof StandardBeanFactory std
            && std.isPrimary(transformedBeanName, beanInstance);
  }

  /**
   * Return the priority assigned for the given bean instance by
   * the {@code jakarta.annotation.Priority} annotation.
   * <p>The default implementation delegates to the specified
   * {@link #setDependencyComparator dependency comparator}, checking its
   * {@link OrderComparator#getPriority method} if it is an extension of
   * Framework's common {@link OrderComparator} - typically, an
   * {@link AnnotationAwareOrderComparator}.
   * If no such comparator is present, this implementation returns {@code null}.
   *
   * @param beanInstance the bean instance to check (can be {@code null})
   * @return the priority assigned to that bean or {@code null} if none is set
   */
  @Nullable
  protected Integer getPriority(Object beanInstance) {
    Comparator<Object> comparator = getDependencyComparator();
    if (comparator instanceof OrderComparator) {
      return ((OrderComparator) comparator).getPriority(beanInstance);
    }
    return null;
  }

  /**
   * Determine whether the given candidate name matches the bean name or the aliases
   * stored in this bean definition.
   */
  protected boolean matchesBeanName(String beanName, @Nullable String candidateName) {
    if (candidateName != null) {
      return candidateName.equals(beanName)
              || ObjectUtils.containsElement(getAliases(beanName), candidateName);
    }
    return false;
  }

  /**
   * Determine whether the given beanName/candidateName pair indicates a self reference,
   * i.e. whether the candidate points back to the original bean or to a factory method
   * on the original bean.
   */
  private boolean isSelfReference(@Nullable String beanName, @Nullable String candidateName) {
    if (beanName != null && candidateName != null) {
      if (beanName.equals(candidateName)) {
        return true;
      }
      BeanDefinition definition = getBeanDefinition(candidateName);
      if (definition != null) {
        return beanName.equals(definition.getFactoryBeanName());
      }
    }
    return false;
  }

  /**
   * Raise a NoSuchBeanDefinitionException or BeanNotOfRequiredTypeException
   * for an unresolvable dependency.
   */
  private void raiseNoMatchingBeanFound(
          Class<?> type, ResolvableType resolvableType, DependencyDescriptor descriptor) throws BeansException {

    checkBeanNotOfRequiredType(type, descriptor);
    throw new NoSuchBeanDefinitionException(resolvableType,
            "expected at least 1 bean which qualifies as autowire candidate. " +
                    "Dependency annotations: " + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
  }

  /**
   * Raise a BeanNotOfRequiredTypeException for an unresolvable dependency, if applicable,
   * i.e. if the target type of the bean would match but an exposed proxy doesn't.
   */
  private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
    AutowireCandidateResolver candidateResolver = getAutowireCandidateResolver();
    for (String beanName : beanDefinitionNames) {
      try {
        BeanDefinition mbd = beanDefinitionMap.get(beanName);
        Class<?> targetType = mbd.getTargetType();
        if (targetType != null && type.isAssignableFrom(targetType)
                && isAutowireCandidate(beanName, mbd, descriptor, candidateResolver)) {
          // Probably a proxy interfering with target type match -> throw meaningful exception.
          Object beanInstance = getSingleton(beanName, false);
          Class<?> beanType = (beanInstance == null || beanInstance == NullValue.INSTANCE)
                              ? predictBeanType(mbd)
                              : beanInstance.getClass();
          if (beanType != null && !type.isAssignableFrom(beanType)) {
            throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
          }
        }
      }
      catch (NoSuchBeanDefinitionException ex) {
        // Bean definition got removed while we were iterating -> ignore.
      }
    }

    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof StandardBeanFactory) {
      ((StandardBeanFactory) parent).checkBeanNotOfRequiredType(type, descriptor);
    }
  }

  /**
   * Create an {@link Optional} wrapper for the specified dependency.
   */
  private Optional<?> createOptionalDependency(
          DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {

    DependencyDescriptor descriptorToUse = new NestedDependencyDescriptor(descriptor) {
      @Override
      public boolean isRequired() {
        return false;
      }

      @Override
      public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
        return ObjectUtils.isNotEmpty(args)
               ? beanFactory.getBean(beanName, args)
               : super.resolveCandidate(beanName, requiredType, beanFactory);
      }
    };

    Object result = doResolveDependency(descriptorToUse, beanName, null, null);
    return result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result);
  }

  private interface BeanObjectSupplier<T> extends ObjectProvider<T>, Serializable { }

  /**
   * A dependency descriptor marker for nested elements.
   */
  private static class NestedDependencyDescriptor extends DependencyDescriptor {

    public NestedDependencyDescriptor(DependencyDescriptor original) {
      super(original);
      increaseNestingLevel();
    }
  }

  /**
   * A dependency descriptor for a multi-element declaration with nested elements.
   */
  private static class MultiElementDescriptor extends NestedDependencyDescriptor {

    public MultiElementDescriptor(DependencyDescriptor original) {
      super(original);
    }
  }

  /**
   * A dependency descriptor marker for stream access to multiple elements.
   */
  private static class StreamDependencyDescriptor extends DependencyDescriptor {
    private final boolean ordered;

    public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
      super(original);
      this.ordered = ordered;
    }

    public boolean isOrdered() {
      return this.ordered;
    }
  }

  /**
   * Serializable ObjectFactory/ObjectProvider for lazy resolution of a dependency.
   */
  private class DependencyObjectProvider implements BeanObjectSupplier<Object> {

    @Nullable
    private final String beanName;
    private final boolean optional;
    private final DependencyDescriptor descriptor;

    public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
      this.beanName = beanName;
      this.descriptor = new NestedDependencyDescriptor(descriptor);
      this.optional = descriptor.getDependencyType() == Optional.class;
    }

    @Override
    public Object get() throws BeansException {
      if (this.optional) {
        return createOptionalDependency(this.descriptor, this.beanName);
      }
      else {
        Object result = doResolveDependency(this.descriptor, this.beanName, null, null);
        if (result == null) {
          throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
        }
        return result;
      }
    }

    @Override
    public Object get(final Object... args) throws BeansException {
      if (this.optional) {
        return createOptionalDependency(this.descriptor, this.beanName, args);
      }
      else {
        DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
          @Override
          public Object resolveCandidate(String beanName, Class<?> requiredType, BeanFactory beanFactory) {
            return beanFactory.getBean(beanName, args);
          }
        };
        Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
        if (result == null) {
          throw new NoSuchBeanDefinitionException(this.descriptor.getResolvableType());
        }
        return result;
      }
    }

    @Override
    @Nullable
    public Object getIfAvailable() throws BeansException {
      try {
        if (this.optional) {
          return createOptionalDependency(this.descriptor, this.beanName);
        }
        else {
          DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
            @Override
            public boolean isRequired() {
              return false;
            }
          };
          return doResolveDependency(descriptorToUse, this.beanName, null, null);
        }
      }
      catch (ScopeNotActiveException ex) {
        // Ignore resolved bean in non-active scope
        return null;
      }
    }

    @Override
    public void ifAvailable(Consumer<Object> dependencyConsumer) throws BeansException {
      Object dependency = getIfAvailable();
      if (dependency != null) {
        try {
          dependencyConsumer.accept(dependency);
        }
        catch (ScopeNotActiveException ex) {
          // Ignore resolved bean in non-active scope, even on scoped proxy invocation
        }
      }
    }

    @Override
    @Nullable
    public Object getIfUnique() throws BeansException {
      DependencyDescriptor descriptorToUse = new DependencyDescriptor(this.descriptor) {
        @Override
        public boolean isRequired() {
          return false;
        }

        @Override
        @Nullable
        public Object resolveNotUnique(ResolvableType type, Map<String, Object> matchingBeans) {
          return null;
        }
      };
      try {
        if (this.optional) {
          return createOptionalDependency(descriptorToUse, this.beanName);
        }
        else {
          return doResolveDependency(descriptorToUse, this.beanName, null, null);
        }
      }
      catch (ScopeNotActiveException ex) {
        // Ignore resolved bean in non-active scope
        return null;
      }
    }

    @Override
    public void ifUnique(Consumer<Object> dependencyConsumer) throws BeansException {
      Object dependency = getIfUnique();
      if (dependency != null) {
        try {
          dependencyConsumer.accept(dependency);
        }
        catch (ScopeNotActiveException ex) {
          // Ignore resolved bean in non-active scope, even on scoped proxy invocation
        }
      }
    }

    @Nullable
    protected Object getValue() throws BeansException {
      if (this.optional) {
        return createOptionalDependency(this.descriptor, this.beanName);
      }
      else {
        return doResolveDependency(this.descriptor, this.beanName, null, null);
      }
    }

    @Override
    public Stream<Object> stream() {
      return resolveStream(false);
    }

    @Override
    public Stream<Object> orderedStream() {
      return resolveStream(true);
    }

    @SuppressWarnings("unchecked")
    private Stream<Object> resolveStream(boolean ordered) {
      DependencyDescriptor descriptorToUse = new StreamDependencyDescriptor(descriptor, ordered);
      Object result = doResolveDependency(descriptorToUse, this.beanName, null, null);
      return result instanceof Stream ? (Stream<Object>) result : Stream.of(result);
    }

  }

  /**
   * Separate inner class for avoiding a hard dependency on the {@code jakarta.inject} API.
   * Actual {@code jakarta.inject.Provider} implementation is nested here in order to make it
   * invisible for Graal's introspection of StandardBeanFactory's nested classes.
   */
  private class Jsr330Factory implements Serializable {

    public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
      return new Jsr330Provider(descriptor, beanName);
    }

    private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {

      public Jsr330Provider(DependencyDescriptor descriptor, @Nullable String beanName) {
        super(descriptor, beanName);
      }

      @Override
      @Nullable
      public Object get() throws BeansException {
        return getValue();
      }
    }
  }

}
