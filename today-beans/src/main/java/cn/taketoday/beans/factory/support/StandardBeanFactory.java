/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.beans.factory.support;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serial;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.TypeConverter;
import cn.taketoday.beans.factory.BeanClassLoadFailedException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.BeanDefinitionValidationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.beans.factory.BeanFactoryUtils;
import cn.taketoday.beans.factory.BeanNotOfRequiredTypeException;
import cn.taketoday.beans.factory.InjectionPoint;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.NoUniqueBeanDefinitionException;
import cn.taketoday.beans.factory.ObjectProvider;
import cn.taketoday.beans.factory.SmartFactoryBean;
import cn.taketoday.beans.factory.SmartInitializingSingleton;
import cn.taketoday.beans.factory.annotation.AnnotatedBeanDefinition;
import cn.taketoday.beans.factory.config.AutowireCapableBeanFactory;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.config.NamedBeanHolder;
import cn.taketoday.core.NamedThreadLocal;
import cn.taketoday.core.OrderComparator;
import cn.taketoday.core.OrderSourceProvider;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Modifiable;
import cn.taketoday.lang.NullValue;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.CompositeIterator;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ReflectionUtils;
import cn.taketoday.util.StringUtils;
import jakarta.inject.Provider;

/**
 * Standard implementation of the {@link ConfigurableBeanFactory}
 * and {@link BeanDefinitionRegistry} interfaces: a full-fledged bean factory
 * based on bean definition metadata, extensible through post-processors.
 *
 * <p>Typical usage is registering all bean definitions first (possibly read
 * from a bean definition file), before accessing beans. Bean lookup by name
 * is therefore an inexpensive operation in a local bean definition table,
 * operating on pre-resolved bean definition metadata objects.
 *
 * <p>Note that readers for specific bean definition formats are typically
 * implemented separately rather than as bean factory subclasses: see for example
 * {@link cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #registerBeanDefinition
 * @see #addBeanPostProcessor
 * @see #getBean
 * @see #resolveDependency
 * @since 2019-03-23 15:00
 */
public class StandardBeanFactory extends AbstractAutowireCapableBeanFactory
        implements ConfigurableBeanFactory, BeanDefinitionRegistry, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private static final Class<?> injectProviderClass = // JSR-330 API not available - Provider interface simply not supported then.
          ClassUtils.load("jakarta.inject.Provider", StandardBeanFactory.class.getClassLoader());

  /** Map from serialized id to factory instance. @since 4.0 */
  private static final Map<String, Reference<StandardBeanFactory>> serializableFactories =
          new ConcurrentHashMap<>(8);

  /** Map of bean definition objects, keyed by bean name */
  private final ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(64);

  /** Map of singleton and non-singleton bean names, keyed by dependency type. */
  private final ConcurrentHashMap<Class<?>, String[]> allBeanNamesByType = new ConcurrentHashMap<>(64);

  /** Map of singleton-only bean names, keyed by dependency type. */
  private final ConcurrentHashMap<Class<?>, String[]> singletonBeanNamesByType = new ConcurrentHashMap<>(64);

  /** Map from bean name to merged BeanDefinitionHolder. @since 4.0 */
  private final Map<String, BeanDefinitionHolder> mergedBeanDefinitionHolders = new ConcurrentHashMap<>(256);

  // Set of bean definition names with a primary marker. */
  private final Set<String> primaryBeanNames = ConcurrentHashMap.newKeySet(16);

  /** Optional id for this factory, for serialization purposes. @since 4.0 */
  @Nullable
  private String serializationId;

  /** Whether to allow re-registration of a different definition with the same name. */
  @Nullable
  private Boolean allowBeanDefinitionOverriding;

  /** Whether to allow eager class loading even for lazy-init beans. */
  private boolean allowEagerClassLoading = true;

  /** Optional OrderComparator for dependency Lists and arrays. */
  @Nullable
  private Comparator<Object> dependencyComparator;

  @Nullable
  private Executor bootstrapExecutor;

  /** Resolver to use for checking if a bean definition is an autowire candidate. */
  private AutowireCandidateResolver autowireCandidateResolver = new SimpleAutowireCandidateResolver();

  /** Whether bean definition metadata may be cached for all beans. */
  private volatile boolean configurationFrozen;

  /** Cached array of bean definition names in case of frozen configuration. */
  @Nullable
  private volatile String[] frozenBeanDefinitionNames;

  /** List of bean definition names, in registration order. */
  private volatile ArrayList<String> beanDefinitionNames = new ArrayList<>(256);

  /** List of names of manually registered singletons, in registration order. */
  private volatile LinkedHashSet<String> manualSingletonNames = new LinkedHashSet<>(16);

  private final NamedThreadLocal<PreInstantiation> preInstantiationThread =
          new NamedThreadLocal<>("Pre-instantiation thread marker");

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
   * Specify an id for serialization purposes, allowing this BeanFactory to be
   * deserialized from this id back into the BeanFactory object, if needed.
   *
   * @since 4.0
   */
  public void setSerializationId(@Nullable String serializationId) {
    if (serializationId != null) {
      serializableFactories.put(serializationId, new WeakReference<>(this));
    }
    else if (this.serializationId != null) {
      serializableFactories.remove(this.serializationId);
    }
    this.serializationId = serializationId;
  }

  /**
   * Return an id for serialization purposes, if specified, allowing this BeanFactory
   * to be deserialized from this id back into the BeanFactory object, if needed.
   *
   * @since 4.0
   */
  @Nullable
  public String getSerializationId() {
    return this.serializationId;
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
    return !Boolean.FALSE.equals(this.allowBeanDefinitionOverriding);
  }

  /**
   * This implementation returns {@code true} if bean definition overriding
   * is generally allowed.
   *
   * @see #setAllowBeanDefinitionOverriding
   */
  @Override
  public boolean isBeanDefinitionOverridable(String beanName) {
    return isAllowBeanDefinitionOverriding();
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

  @Override
  public void setBootstrapExecutor(@Nullable Executor bootstrapExecutor) {
    this.bootstrapExecutor = bootstrapExecutor;
  }

  @Override
  @Nullable
  public Executor getBootstrapExecutor() {
    return this.bootstrapExecutor;
  }

  /**
   * Set a custom autowire candidate resolver for this BeanFactory to use
   * when deciding whether a bean definition should be considered as a
   * candidate for autowiring.
   *
   * @since 4.0
   */
  public void setAutowireCandidateResolver(AutowireCandidateResolver autowireCandidateResolver) {
    Assert.notNull(autowireCandidateResolver, "AutowireCandidateResolver is required");
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
    updateManualSingletonNames(set -> set.add(beanName), set -> !beanDefinitionMap.containsKey(beanName));
    clearByTypeCache();
  }

  @Override
  public void destroySingletons() {
    super.destroySingletons();
    updateManualSingletonNames(Set::clear, set -> !set.isEmpty());
    clearByTypeCache();
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
    if (!isBeanDefinitionOverridable(name) && containsBeanDefinition(alias)) {
      throw new IllegalStateException("Cannot register alias '%s' for name '%s': Alias would override bean definition '%s'"
              .formatted(alias, name, alias));
    }
  }

  @Override
  public void freezeConfiguration() {
    this.configurationFrozen = true;
    this.frozenBeanDefinitionNames = StringUtils.toStringArray(this.beanDefinitionNames);
  }

  @Override
  protected void clearMergedBeanDefinition(String beanName) {
    super.clearMergedBeanDefinition(beanName);
    this.mergedBeanDefinitionHolders.remove(beanName);
  }

  @Override
  public void clearMetadataCache() {
    super.clearMetadataCache();
    this.mergedBeanDefinitionHolders.clear();
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
    if (hasBeanCreationStarted()) {
      // Cannot modify startup-time collection elements anymore (for stable iteration)
      synchronized(beanDefinitionMap) {
        if (condition.test(manualSingletonNames)) {
          LinkedHashSet<String> updatedSingletons = new LinkedHashSet<>(manualSingletonNames);
          action.accept(updatedSingletons);
          this.manualSingletonNames = updatedSingletons;
        }
      }
    }
    else {
      // Still in startup registration phase
      if (condition.test(manualSingletonNames)) {
        action.accept(manualSingletonNames);
      }
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

  @Override
  protected void checkMergedBeanDefinition(RootBeanDefinition mbd, String beanName, @Nullable Object[] args) {
    super.checkMergedBeanDefinition(mbd, beanName, args);

    if (mbd.isBackgroundInit()) {
      if (preInstantiationThread.get() == PreInstantiation.MAIN && getBootstrapExecutor() != null) {
        throw new BeanCurrentlyInCreationException(beanName, "Bean marked for background " +
                "initialization but requested in mainline thread - declare ObjectProvider " +
                "or lazy injection point in dependent mainline beans");
      }
    }
    else {
      // Bean intended to be initialized in main bootstrap thread
      if (preInstantiationThread.get() == PreInstantiation.BACKGROUND) {
        throw new BeanCurrentlyInCreationException(beanName, """
                Bean marked for mainline initialization \
                but requested in background thread - enforce early instantiation in mainline thread \
                through depends-on '%s' declaration for dependent background beans""".formatted(beanName));
      }
    }
  }

  @Override
  protected boolean isCurrentThreadAllowedToHoldSingletonLock() {
    return preInstantiationThread.get() != PreInstantiation.BACKGROUND;
  }

  @Override
  public void preInstantiateSingletons() {
    if (log.isTraceEnabled()) {
      log.trace("Pre-instantiating singletons in {}", this);
    }
    // Iterate over a copy to allow for init methods which in turn register new bean definitions.
    // While this may not be part of the regular factory bootstrap, it does otherwise work fine.

    ArrayList<String> beanNames = new ArrayList<>(this.beanDefinitionNames);

    // Trigger initialization of all non-lazy singleton beans...
    List<CompletableFuture<?>> futures = new ArrayList<>();
    this.preInstantiationThread.set(PreInstantiation.MAIN);
    try {
      for (String beanName : beanNames) {
        RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
        if (!mbd.isAbstract() && mbd.isSingleton()) {
          CompletableFuture<?> future = preInstantiateSingleton(beanName, mbd);
          if (future != null) {
            futures.add(future);
          }
        }
      }
    }
    finally {
      this.preInstantiationThread.remove();
    }
    if (!futures.isEmpty()) {
      try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
      }
      catch (CompletionException ex) {
        ReflectionUtils.rethrowRuntimeException(ex.getCause());
      }
    }

    // Trigger post-initialization callback for all applicable beans...
    for (String beanName : beanNames) {
      Object singletonInstance = getSingleton(beanName, false);
      if (singletonInstance instanceof SmartInitializingSingleton smartSingleton) {
        smartSingleton.afterSingletonsInstantiated(this);
      }
    }

  }

  @Nullable
  private CompletableFuture<?> preInstantiateSingleton(String beanName, RootBeanDefinition mbd) {
    if (mbd.isBackgroundInit()) {
      Executor executor = getBootstrapExecutor();
      if (executor != null) {
        String[] dependsOn = mbd.getDependsOn();
        if (dependsOn != null) {
          for (String dep : dependsOn) {
            getBean(dep);
          }
        }
        CompletableFuture<?> future = CompletableFuture.runAsync(
                () -> instantiateSingletonInBackgroundThread(beanName), executor);
        addSingletonFactory(beanName, () -> {
          try {
            future.join();
          }
          catch (CompletionException ex) {
            ReflectionUtils.rethrowRuntimeException(ex.getCause());
          }
          return future;  // not to be exposed, just to lead to ClassCastException in case of mismatch
        });
        return (!mbd.isLazyInit() ? future : null);
      }
      else if (log.isInfoEnabled()) {
        log.info("Bean '{}' marked for background initialization " +
                "without bootstrap executor configured - falling back to mainline initialization", beanName);
      }
    }
    if (!mbd.isLazyInit()) {
      instantiateSingleton(beanName);
    }
    return null;
  }

  private void instantiateSingletonInBackgroundThread(String beanName) {
    preInstantiationThread.set(PreInstantiation.BACKGROUND);
    try {
      instantiateSingleton(beanName);
    }
    catch (RuntimeException | Error ex) {
      if (log.isWarnEnabled()) {
        log.warn("Failed to instantiate singleton bean '{}' in background thread", beanName, ex);
      }
      throw ex;
    }
    finally {
      preInstantiationThread.remove();
    }
  }

  private void instantiateSingleton(String beanName) {
    if (isFactoryBean(beanName)) {
      Object bean = getBean(FACTORY_BEAN_PREFIX + beanName);
      if (bean instanceof SmartFactoryBean<?> smartFactoryBean && smartFactoryBean.isEagerInit()) {
        getBean(beanName);
      }
    }
    else {
      getBean(beanName);
    }
  }

  //---------------------------------------------------------------------
  // Implementation of BeanDefinitionRegistry interface @since 4.0
  //---------------------------------------------------------------------

  @Override
  public void registerBeanDefinition(String beanName, BeanDefinition def) {
    Assert.hasText(beanName, "Bean name must not be empty");
    Assert.notNull(def, "BeanDefinition is required");

    if (def instanceof AbstractBeanDefinition abd) {
      try {
        abd.validate();
      }
      catch (BeanDefinitionValidationException ex) {
        throw new BeanDefinitionStoreException(def.getResourceDescription(), beanName,
                "Validation of bean definition failed", ex);
      }
    }

    BeanDefinition existBeanDef = beanDefinitionMap.get(beanName);
    if (existBeanDef != null) {
      if (!isBeanDefinitionOverridable(beanName)) {
        throw new BeanDefinitionOverrideException(beanName, def, existBeanDef);
      }
      else {
        logBeanDefinitionOverriding(beanName, def, existBeanDef);
      }

      beanDefinitionMap.put(beanName, def);
    }
    else {
      if (isAlias(beanName)) {
        String aliasedName = canonicalName(beanName);
        if (!isBeanDefinitionOverridable(aliasedName)) {
          if (containsBeanDefinition(aliasedName)) {  // alias for existing bean definition
            throw new BeanDefinitionOverrideException(
                    beanName, def, getBeanDefinition(aliasedName));
          }
          else {  // alias pointing to non-existing bean definition
            throw new BeanDefinitionStoreException(def.getResourceDescription(), beanName,
                    "Cannot register bean definition for bean '%s' since there is already an alias for bean '%s' bound."
                            .formatted(beanName, aliasedName));
          }
        }
        else {
          removeAlias(beanName);
        }
      }

      if (hasBeanCreationStarted()) {
        // Cannot modify startup-time collection elements anymore (for stable iteration)
        synchronized(beanDefinitionMap) {
          beanDefinitionMap.put(beanName, def);
          ArrayList<String> updatedDefinitions = new ArrayList<>(beanDefinitionNames.size() + 1);
          updatedDefinitions.addAll(beanDefinitionNames);
          updatedDefinitions.add(beanName);
          this.beanDefinitionNames = updatedDefinitions;
          removeManualSingletonName(beanName);
        }
      }
      else {
        // Still in startup registration phase
        beanDefinitionMap.put(beanName, def);
        beanDefinitionNames.add(beanName);
        removeManualSingletonName(beanName);
      }
      this.frozenBeanDefinitionNames = null;

    }

    if (existBeanDef != null || containsSingleton(beanName)) {
      resetBeanDefinition(beanName);
    }
    else if (isConfigurationFrozen()) {
      clearByTypeCache();
    }

    // Cache a primary marker for the given bean.
    if (def.isPrimary()) {
      primaryBeanNames.add(beanName);
    }
  }

  private void logBeanDefinitionOverriding(String beanName, BeanDefinition beanDefinition,
          BeanDefinition existingDefinition) {

    boolean explicitBeanOverride = (this.allowBeanDefinitionOverriding != null);
    if (existingDefinition.getRole() < beanDefinition.getRole()) {
      // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
      if (log.isInfoEnabled()) {
        log.info("Overriding user-defined bean definition for bean '{}' with a framework-generated bean definition: replacing [{}] with [{}]",
                beanName, existingDefinition, beanDefinition);
      }
    }
    else if (!beanDefinition.equals(existingDefinition)) {
      if (explicitBeanOverride && log.isInfoEnabled()) {
        log.info("Overriding bean definition for bean '{}' with a different definition: replacing [{}] with [{}]",
                beanName, existingDefinition, beanDefinition);
      }
      if (log.isDebugEnabled()) {
        log.debug("Overriding bean definition for bean '{}' with a different definition: replacing [{}] with [{}]",
                beanName, existingDefinition, beanDefinition);
      }
    }
    else {
      if (explicitBeanOverride && log.isInfoEnabled()) {
        log.info("Overriding bean definition for bean '{}' with an equivalent definition: replacing [{}] with [{}]",
                beanName, existingDefinition, beanDefinition);
      }
      if (log.isTraceEnabled()) {
        log.trace("Overriding bean definition for bean '{}' with an equivalent definition: replacing [{}] with [{}]",
                beanName, existingDefinition, beanDefinition);
      }
    }
  }

  @Override
  public void removeBeanDefinition(String beanName) {
    Assert.hasText(beanName, "'beanName' must not be empty");
    BeanDefinition bd = beanDefinitionMap.remove(beanName);
    if (bd == null) {
      if (log.isDebugEnabled()) {
        log.trace("No bean named '{}' found in {}", beanName, this);
      }
      throw new NoSuchBeanDefinitionException(beanName);
    }

    if (hasBeanCreationStarted()) {
      // Cannot modify startup-time collection elements anymore (for stable iteration)
      synchronized(beanDefinitionMap) {
        ArrayList<String> updatedDefinitions = new ArrayList<>(beanDefinitionNames);
        updatedDefinitions.remove(beanName);
        this.beanDefinitionNames = updatedDefinitions;
      }
    }
    else {
      // Still in startup registration phase
      beanDefinitionNames.remove(beanName);
    }
    this.frozenBeanDefinitionNames = null;

    resetBeanDefinition(beanName);
  }

  @Override
  public BeanDefinition getBeanDefinition(String beanName) throws NoSuchBeanDefinitionException {
    BeanDefinition bd = beanDefinitionMap.get(beanName);
    if (bd == null) {
      log.trace("No bean named '{}' found in {}", beanName, this);
      throw new NoSuchBeanDefinitionException(beanName);
    }
    return bd;
  }

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
    CompositeIterator<String> iterator = new CompositeIterator<>();
    iterator.add(this.beanDefinitionNames.iterator());
    iterator.add(this.manualSingletonNames.iterator());
    return iterator;
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
  @SuppressWarnings("unchecked")
  public <T> T getBean(Class<T> requiredType, @Nullable Object... args) throws BeansException {
    Assert.notNull(requiredType, "Required type is required");
    Object resolved = resolveBean(ResolvableType.forRawClass(requiredType), args, false);
    if (resolved == null) {
      throw new NoSuchBeanDefinitionException(requiredType);
    }
    return (T) resolved;
  }

  @Nullable
  private <T> T resolveBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull) {
    NamedBeanHolder<T> namedBean = resolveNamedBean(requiredType, args, nonUniqueAsNull);
    if (namedBean != null) {
      return namedBean.getBeanInstance();
    }

    BeanFactory parent = getParentBeanFactory();
    if (parent instanceof StandardBeanFactory) {
      return ((StandardBeanFactory) parent).resolveBean(requiredType, args, nonUniqueAsNull);
    }
    else if (parent != null) {
      ObjectProvider<T> parentProvider = parent.getBeanProvider(requiredType);
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
  private <T> NamedBeanHolder<T> resolveNamedBean(ResolvableType requiredType, @Nullable Object[] args, boolean nonUniqueAsNull)
          throws BeansException //
  {
    Assert.notNull(requiredType, "Required type is required");
    Set<String> candidateNames = getBeanNamesForType(requiredType);

    int size = candidateNames.size();
    if (size > 1) {
      LinkedHashSet<String> autowireCandidates = new LinkedHashSet<>(size);
      for (String beanName : candidateNames) {
        if (!containsBeanDefinition(beanName) || getBeanDefinition(beanName).isAutowireCandidate()) {
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
  private <T> NamedBeanHolder<T> resolveNamedBean(String beanName, ResolvableType requiredType, Object[] args) throws BeansException {
    Object bean = doGetBean(beanName, null, args, false);
    if (bean == null) {
      return null;
    }
    return new NamedBeanHolder<>(beanName, adaptBeanInstance(beanName, bean, requiredType.toClass()));
  }

  @Override
  public <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException {
    Assert.notNull(requiredType, "Required type is required");
    NamedBeanHolder<T> namedBean = resolveNamedBean(ResolvableType.forClass(requiredType), null, false);
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
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
    return getBeanProvider(requiredType, true);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType) {
    return getBeanProvider(requiredType, true);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType, boolean allowEagerInit) {
    Assert.notNull(requiredType, "Required type is required");
    return getBeanProvider(ResolvableType.forRawClass(requiredType), allowEagerInit);
  }

  @Override
  public <T> ObjectProvider<T> getBeanProvider(ResolvableType requiredType, boolean allowEagerInit) {
    return new BeanObjectProvider<>() {
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
      public boolean ifAvailable(Consumer<T> dependencyConsumer) throws BeansException {
        T dependency = getIfAvailable();
        if (dependency != null) {
          try {
            dependencyConsumer.accept(dependency);
            return true;
          }
          catch (ScopeNotActiveException ex) {
            // Ignore resolved bean in non-active scope, even on scoped proxy invocation
          }
        }
        return false;
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
      public boolean ifUnique(Consumer<T> dependencyConsumer) throws BeansException {
        T dependency = getIfUnique();
        if (dependency != null) {
          try {
            dependencyConsumer.accept(dependency);
            return true;
          }
          catch (ScopeNotActiveException ex) {
            // Ignore resolved bean in non-active scope, even on scoped proxy invocation
          }
        }
        return false;
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
    OrderComparator comparator = dependencyComparator instanceof OrderComparator
            ? (OrderComparator) dependencyComparator : OrderComparator.INSTANCE;
    return comparator.withSourceProvider(createFactoryAwareOrderSourceProvider(matchingBeans));
  }

  private OrderSourceProvider createFactoryAwareOrderSourceProvider(Map<String, ?> beans) {
    return new FactoryAwareOrderSourceProvider(beans);
  }

  @Override
  public <T> Map<String, T> getBeansOfType(Class<T> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    return getBeansOfType(ResolvableType.forRawClass(requiredType), includeNonSingletons, allowEagerInit);
  }

  @Override
  @Modifiable
  @SuppressWarnings("unchecked")
  public <T> Map<String, T> getBeansOfType(ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
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
            if (log.isDebugEnabled()) {
              log.trace("Ignoring match to currently created bean '{}': ", exBeanName, ex.getMessage());
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
  @Modifiable
  public Set<String> getBeanNamesForType(Class<?> requiredType, boolean includeNonSingletons, boolean allowEagerInit) {
    if (!isConfigurationFrozen() || requiredType == null || !allowEagerInit) {
      return doGetBeanNamesForType(
              ResolvableType.forRawClass(requiredType), includeNonSingletons, allowEagerInit);
    }

    Map<Class<?>, String[]> cache = includeNonSingletons ? this.allBeanNamesByType : this.singletonBeanNamesByType;
    String[] resolvedBeanNames = cache.get(requiredType);
    if (resolvedBeanNames != null) {
      return CollectionUtils.newLinkedHashSet(resolvedBeanNames);
    }

    Set<String> resolvedBeanNamesSet = doGetBeanNamesForType(
            ResolvableType.forRawClass(requiredType), includeNonSingletons, true);
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
  @Modifiable
  public Set<String> getBeanNamesForType(ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {

    Class<?> resolved = requiredType.resolve();
    if (resolved != null && !requiredType.hasGenerics()) {
      return getBeanNamesForType(resolved, includeNonSingletons, allowEagerInit);
    }
    else {
      return doGetBeanNamesForType(requiredType, includeNonSingletons, allowEagerInit);
    }
  }

  @Modifiable
  private Set<String> doGetBeanNamesForType(ResolvableType requiredType, boolean includeNonSingletons, boolean allowEagerInit) {

    LinkedHashSet<String> beanNames = new LinkedHashSet<>();
    // 1. Check all bean definitions.
    for (String beanName : beanDefinitionNames) {
      // Only consider bean as eligible if the bean name is not defined as alias for some other bean.
      if (isAlias(beanName)) {
        continue;
      }

      try {
        RootBeanDefinition merged = getMergedLocalBeanDefinition(beanName);
        // Only check bean definition if it is complete.
        if (!merged.isAbstract() && (allowEagerInit || allowCheck(merged))) {
          boolean matchFound = false;
          BeanDefinitionHolder decorated = merged.getDecoratedDefinition();
          if (isFactoryBean(beanName, merged)) {
            boolean isNonLazyDecorated = decorated != null && !merged.isLazyInit();
            boolean allowFactoryBeanInit = allowEagerInit || containsSingleton(beanName);
            if (includeNonSingletons || isNonLazyDecorated || (allowFactoryBeanInit && isSingleton(beanName, merged, decorated))) {
              matchFound = isTypeMatch(beanName, requiredType, allowFactoryBeanInit);
            }
            if (!matchFound) {
              // In case of FactoryBean, try to match FactoryBean instance itself next.
              beanName = FACTORY_BEAN_PREFIX + beanName;
              if (includeNonSingletons || isSingleton(beanName, merged, decorated)) {
                matchFound = isTypeMatch(beanName, requiredType, allowFactoryBeanInit);
              }
            }
          }
          else {
            if (includeNonSingletons || isSingleton(beanName, merged, decorated)) {
              boolean allowFactoryBeanInit = allowEagerInit || containsSingleton(beanName);
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
        if (log.isTraceEnabled()) {
          String message =
                  (ex instanceof BeanClassLoadFailedException
                          ? "Ignoring bean class loading failure for bean '%s'".formatted(beanName)
                          : "Ignoring unresolvable metadata in bean definition '%s'".formatted(beanName));
          log.trace(message, ex);
        }
        // Register exception, in case the bean was accidentally unresolvable.
        onSuppressedException(ex);
      }
      catch (NoSuchBeanDefinitionException ex) {
        // Bean definition got removed while we were iterating -> ignore.
      }
    }

    // 2. Check manually registered singletons too.
    for (String beanName : manualSingletonNames) {
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

  private boolean allowCheck(RootBeanDefinition definition) {
    return (
            definition.hasBeanClass()
                    || !definition.isLazyInit()
                    || isAllowEagerClassLoading()
    ) && !requiresEagerInitForType(definition.getFactoryBeanName());
  }

  private boolean isSingleton(String beanName, RootBeanDefinition mbd, @Nullable BeanDefinitionHolder dbd) {
    return dbd != null ? mbd.isSingleton() : isSingleton(beanName);
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
  public Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType, boolean includeNonSingletons) {
    Assert.notNull(annotationType, "annotationType is required");

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
    Assert.notNull(annotationType, "annotationType is required");
    LinkedHashSet<String> names = new LinkedHashSet<>();

    for (String beanName : beanDefinitionNames) {
      BeanDefinition bd = beanDefinitionMap.get(beanName);
      if (bd != null && !bd.isAbstract() && findAnnotationOnBean(beanName, annotationType).isPresent()) {
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

    // find on factory-method then find on class

    boolean containsBeanDefinition = containsBeanDefinition(beanName);
    if (containsBeanDefinition) {
      BeanDefinition definition = getBeanDefinition(beanName);
      if (definition instanceof AnnotatedBeanDefinition annotated) {
        // find on factory method
        MethodMetadata methodMetadata = annotated.getFactoryMethodMetadata();
        if (methodMetadata != null) {
          MergedAnnotation<A> annotation = methodMetadata.getAnnotation(annotationType);
          if (annotation.isPresent()) {
            return annotation;
          }
          // factory-method not found
        }
        else {
          return annotated.getMetadata().getAnnotation(annotationType);
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
    if (containsBeanDefinition) {
      RootBeanDefinition merged = getMergedLocalBeanDefinition(beanName);
      // Check raw bean class, e.g. in case of a proxy.
      if (merged.hasBeanClass() && merged.getFactoryMethodName() == null) {
        Class<?> beanClass = merged.getBeanClass();
        if (beanClass != beanType) {
          MergedAnnotation<A> annotation =
                  MergedAnnotations.from(beanClass, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
          if (annotation.isPresent()) {
            return annotation;
          }
        }
      }
      // Check annotations declared on factory method, if any.
      Method factoryMethod = merged.getResolvedFactoryMethod();
      if (factoryMethod != null) {
        return MergedAnnotations.from(factoryMethod, SearchStrategy.TYPE_HIERARCHY).get(annotationType);
      }
    }
    // missing
    return MergedAnnotation.missing();
  }

  @Override
  public <A extends Annotation> Set<A> findAllAnnotationsOnBean(String beanName, Class<A> annotationType, boolean allowFactoryBeanInit)
          throws NoSuchBeanDefinitionException //
  {
    var annotations = new LinkedHashSet<A>();
    Class<?> beanType = getType(beanName, allowFactoryBeanInit);
    if (beanType != null) {
      MergedAnnotations.from(beanType, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
              .stream(annotationType)
              .filter(MergedAnnotation::isPresent)
              .forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
    }
    if (containsBeanDefinition(beanName)) {
      RootBeanDefinition bd = getMergedLocalBeanDefinition(beanName);
      // Check raw bean class, e.g. in case of a proxy.
      if (bd.hasBeanClass() && bd.getFactoryMethodName() == null) {
        Class<?> beanClass = bd.getBeanClass();
        if (beanClass != beanType) {
          MergedAnnotations.from(beanClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                  .stream(annotationType)
                  .filter(MergedAnnotation::isPresent)
                  .forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
        }
      }
      // Check annotations declared on factory method, if any.
      Method factoryMethod = bd.getResolvedFactoryMethod();
      if (factoryMethod != null) {
        MergedAnnotations.from(factoryMethod, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
                .stream(annotationType)
                .filter(MergedAnnotation::isPresent)
                .forEach(mergedAnnotation -> annotations.add(mergedAnnotation.synthesize()));
      }
    }
    return annotations;
  }

  //---------------------------------------------------------------------
  // Implementation of ConfigurableBeanFactory interface @since 4.0
  //---------------------------------------------------------------------

  @Override
  public void copyConfigurationFrom(ConfigurableBeanFactory otherFactory) {
    super.copyConfigurationFrom(otherFactory);
    if (otherFactory instanceof StandardBeanFactory std) {
      this.bootstrapExecutor = std.bootstrapExecutor;
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
   * triggering {@link #destroySingleton} and {@link MergedBeanDefinitionPostProcessor#resetBeanDefinition}
   * on the given bean.
   *
   * @param beanName the name of the bean to reset
   * @see #registerBeanDefinition
   * @see #removeBeanDefinition
   */
  protected void resetBeanDefinition(String beanName) {
    // Remove the merged bean definition for the given bean, if already created.
    clearMergedBeanDefinition(beanName);

    // Remove corresponding bean from singleton cache, if any. Shouldn't usually
    // be necessary, rather just meant for overriding a context's default beans
    destroySingleton(beanName);

    // Remove a cached primary marker for the given bean.
    primaryBeanNames.remove(beanName);

    // Notify all post-processors that the specified bean definition has been reset.
    for (MergedBeanDefinitionPostProcessor processor : postProcessors().definitions) {
      processor.resetBeanDefinition(beanName);
    }

    // Reset all bean definitions that have the given bean as parent (recursively).
    for (String bdName : this.beanDefinitionNames) {
      if (!beanName.equals(bdName)) {
        BeanDefinition bd = this.beanDefinitionMap.get(bdName);
        // Ensure bd is non-null due to potential concurrent modification of beanDefinitionMap.
        if (bd != null && beanName.equals(bd.getParentName())) {
          resetBeanDefinition(bdName);
        }
      }
    }
  }

  @Override
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
  protected boolean isAutowireCandidate(String beanName, DependencyDescriptor descriptor, AutowireCandidateResolver resolver)
          throws NoSuchBeanDefinitionException {

    String bdName = BeanFactoryUtils.transformedBeanName(beanName);
    if (containsBeanDefinition(bdName)) {
      return isAutowireCandidate(beanName, getMergedLocalBeanDefinition(bdName), descriptor, resolver);
    }
    else if (containsSingleton(beanName)) {
      return isAutowireCandidate(
              beanName, new RootBeanDefinition(getType(beanName)), descriptor, resolver);
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
   * @param merged the bean definition to check
   * @param descriptor the descriptor of the dependency to resolve
   * @param resolver the AutowireCandidateResolver to use for the actual resolution algorithm
   * @return whether the bean should be considered as autowire candidate
   */
  protected boolean isAutowireCandidate(String beanName, RootBeanDefinition merged,
          DependencyDescriptor descriptor, AutowireCandidateResolver resolver) {

    String bdName = BeanFactoryUtils.transformedBeanName(beanName);
    resolveBeanClass(bdName, merged);
    if (merged.isFactoryMethodUnique && merged.factoryMethodToIntrospect == null) {
      new ConstructorResolver(this).resolveFactoryMethodIfPossible(merged);
    }
    BeanDefinitionHolder holder = getHolder(beanName, merged, bdName);
    return resolver.isAutowireCandidate(holder, descriptor);
  }

  private BeanDefinitionHolder getHolder(String beanName, RootBeanDefinition merged, String bdName) {
    if (beanName.equals(bdName)) {
      return mergedBeanDefinitionHolders.computeIfAbsent(beanName, key -> new BeanDefinitionHolder(merged, beanName, getAliases(bdName)));
    }
    return new BeanDefinitionHolder(merged, beanName, getAliases(bdName));
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
            || ObjectProvider.class == dependencyType) {
      return new DependencyObjectProvider(descriptor, requestingBeanName);
    }
    else if (injectProviderClass == dependencyType) {
      return new Jsr330Factory().createDependencyProvider(descriptor, requestingBeanName);
    }
    else if (descriptor.supportsLazyResolution()) {
      Object result = getAutowireCandidateResolver().getLazyResolutionProxyIfNecessary(descriptor, requestingBeanName);
      if (result != null) {
        return result;
      }
    }
    return doResolveDependency(descriptor, requestingBeanName, autowiredBeanNames, typeConverter);
  }

  @Nullable
  public Object doResolveDependency(DependencyDescriptor descriptor, @Nullable String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) throws BeansException {
    InjectionPoint previousInjectionPoint = ConstructorResolver.setCurrentInjectionPoint(descriptor);
    try {
      // Step 1: pre-resolved shortcut for single bean match, e.g. from @Autowired
      Object shortcut = descriptor.resolveShortcut(this);
      if (shortcut != null) {
        return shortcut;
      }
      // Step 2: pre-defined value or expression, e.g. from @Value

      Class<?> type = descriptor.getDependencyType();
      Object value = getAutowireCandidateResolver().getSuggestedValue(descriptor);
      if (value != null) {
        if (value instanceof String) {
          String strVal = resolveEmbeddedValue((String) value);
          BeanDefinition bd = beanName != null && containsBean(beanName)
                  ? getMergedBeanDefinition(beanName) : null;
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

      // Step 3: shortcut for declared dependency name or qualifier-suggested name matching target bean name
      if (descriptor.usesStandardBeanLookup()) {
        String dependencyName = descriptor.getDependencyName();
        if (dependencyName == null || !containsBean(dependencyName)) {
          String suggestedName = getAutowireCandidateResolver().getSuggestedName(descriptor);
          dependencyName = suggestedName != null && containsBean(suggestedName) ? suggestedName : null;
        }
        if (dependencyName != null) {
          dependencyName = canonicalName(dependencyName);  // dependency name can be alias of target name
          if (isTypeMatch(dependencyName, type) && isAutowireCandidate(dependencyName, descriptor)
                  && !isFallback(dependencyName) && !hasPrimaryConflict(dependencyName, type)
                  && !isSelfReference(beanName, dependencyName)) {
            if (autowiredBeanNames != null) {
              autowiredBeanNames.add(dependencyName);
            }
            Object result = getBean(dependencyName);
            if (result == null) {
              result = getInjector().resolve(descriptor, dependencyName, autowiredBeanNames, typeConverter, null);
            }
            return resolveInstance(result, descriptor, type, dependencyName);
          }
        }
      }

      // Step 4a: multiple beans as stream / array / standard collection / plain map
      Object multipleBeans = resolveMultipleBeans(descriptor, beanName, autowiredBeanNames, typeConverter);
      if (multipleBeans != null) {
        return multipleBeans;
      }

      // Step 4b: direct bean matches, possibly direct beans of type Collection / Map
      Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
      if (matchingBeans.isEmpty()) {
        // Step 4c (fallback): custom Collection / Map declarations for collecting multiple beans
        multipleBeans = resolveMultipleBeansFallback(descriptor, beanName, autowiredBeanNames, typeConverter);
        if (multipleBeans != null) {
          return multipleBeans;
        }
        // Raise exception if nothing found for required injection point
        Object result = getInjector().resolve(descriptor, beanName, autowiredBeanNames, typeConverter, null);
        return resolveInstance(result, descriptor, type, beanName);
      }

      String autowiredBeanName;
      Object instanceCandidate;

      // Step 5: determine single candidate
      if (matchingBeans.size() > 1) {
        autowiredBeanName = determineAutowireCandidate(matchingBeans, descriptor);
        if (autowiredBeanName == null) {
          if (isRequired(descriptor) || !indicatesArrayCollectionOrMap(type)) {
            // Raise exception if no clear match found for required injection point
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

      // Step 6: validate single result
      if (autowiredBeanNames != null) {
        autowiredBeanNames.add(autowiredBeanName);
      }
      if (instanceCandidate instanceof Class) {
        instanceCandidate = descriptor.resolveCandidate(autowiredBeanName, type, this);
      }
      Object result = instanceCandidate;
      if (result == null) {
        // for user define
        result = getInjector().resolve(descriptor, autowiredBeanName, autowiredBeanNames, typeConverter, null);
        if (result == null && isRequired(descriptor)) {
          if (matchingBeans.size() == 1) {
            // factory method returns null
            BeanDefinition merged = getMergedBeanDefinition(autowiredBeanName);
            String factoryMethodName = merged.getFactoryMethodName();
            if (factoryMethodName != null) {
              String factoryBeanName = merged.getFactoryBeanName();
              if (factoryBeanName == null) {
                factoryBeanName = merged.getBeanClassName();
              }
              throw new FactoryMethodBeanException(merged, descriptor, autowiredBeanName,
                      "Only one bean which qualifies as autowire candidate, but its factory method '%s' in '%s' returns null"
                              .formatted(factoryMethodName, factoryBeanName));
            }
          }
          // Raise exception if null encountered for required injection point
          raiseNoMatchingBeanFound(type, descriptor);
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
  private Object resolveInstance(Object candidate, DependencyDescriptor descriptor, Class<?> type, String name) {
    if (candidate == null) {
      // Raise exception if null encountered for required injection point
      if (isRequired(descriptor)) {
        raiseNoMatchingBeanFound(type, descriptor);
      }
    }
    if (!ClassUtils.isAssignableValue(type, candidate)) {
      throw new BeanNotOfRequiredTypeException(
              name, type, candidate != null ? candidate.getClass() : NullValue.class);
    }
    return candidate;
  }

  @Nullable
  @SuppressWarnings("unchecked")
  private <T> T convertIfNecessary(@Nullable Object bean, @Nullable Class<?> requiredType, @Nullable TypeConverter converter) {
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

    if (descriptor instanceof StreamDependencyDescriptor streamDescriptor) {
      Map<String, Object> matchingBeans = findAutowireCandidates(beanName, type, descriptor);
      if (autowiredBeanNames != null) {
        autowiredBeanNames.addAll(matchingBeans.keySet());
      }
      Stream<Object> stream = matchingBeans.keySet().stream()
              .map(name -> descriptor.resolveCandidate(name, type, this))
              .filter(Objects::nonNull);
      if (streamDescriptor.isOrdered()) {
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
      if (result instanceof Object[] array && array.length > 1 && descriptor.isOrdered()) {
        Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
        if (comparator != null) {
          Arrays.sort(array, comparator);
        }
      }
      return result;
    }
    else if (Collection.class == type || Set.class == type || List.class == type) {
      return resolveMultipleBeanCollection(descriptor, beanName, autowiredBeanNames, typeConverter);
    }
    else if (Map.class == type) {
      return resolveMultipleBeanMap(descriptor, beanName, autowiredBeanNames, typeConverter);
    }
    else {
      return null;
    }
  }

  @Nullable
  private Object resolveMultipleBeansFallback(DependencyDescriptor descriptor, @Nullable String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

    Class<?> type = descriptor.getDependencyType();
    if (Collection.class.isAssignableFrom(type) && type.isInterface()) {
      return resolveMultipleBeanCollection(descriptor, beanName, autowiredBeanNames, typeConverter);
    }
    else if (Map.class.isAssignableFrom(type) && type.isInterface()) {
      return resolveMultipleBeanMap(descriptor, beanName, autowiredBeanNames, typeConverter);
    }
    return null;
  }

  @Nullable
  private Object resolveMultipleBeanCollection(DependencyDescriptor descriptor, @Nullable String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

    Class<?> elementType = descriptor.getResolvableType().asCollection().resolveGeneric();
    if (elementType == null) {
      return null;
    }
    Map<String, Object> matchingBeans = findAutowireCandidates(
            beanName, elementType, new MultiElementDescriptor(descriptor));
    if (matchingBeans.isEmpty()) {
      return null;
    }
    if (autowiredBeanNames != null) {
      autowiredBeanNames.addAll(matchingBeans.keySet());
    }
    Object result = convertIfNecessary(matchingBeans.values(), descriptor.getDependencyType(), typeConverter);
    if (result instanceof List<?> list && list.size() > 1 && descriptor.isOrdered()) {
      Comparator<Object> comparator = adaptDependencyComparator(matchingBeans);
      if (comparator != null) {
        list.sort(comparator);
      }
    }
    return result;
  }

  @Nullable
  private Object resolveMultipleBeanMap(DependencyDescriptor descriptor, @Nullable String beanName,
          @Nullable Set<String> autowiredBeanNames, @Nullable TypeConverter typeConverter) {

    ResolvableType mapType = descriptor.getResolvableType().asMap();
    Class<?> keyType = mapType.resolveGeneric(0);
    if (String.class != keyType) {
      return null;
    }
    Class<?> valueType = mapType.resolveGeneric(1);
    if (valueType == null) {
      return null;
    }
    Map<String, Object> matchingBeans = findAutowireCandidates(
            beanName, valueType, new MultiElementDescriptor(descriptor));
    if (matchingBeans.isEmpty()) {
      return null;
    }
    if (autowiredBeanNames != null) {
      autowiredBeanNames.addAll(matchingBeans.keySet());
    }
    TypeConverter converter = (typeConverter != null ? typeConverter : getTypeConverter());
    return converter.convertIfNecessary(matchingBeans, descriptor.getDependencyType());
  }

  boolean isRequired(DependencyDescriptor descriptor) {
    return getAutowireCandidateResolver().isRequired(descriptor);
  }

  private boolean indicatesArrayCollectionOrMap(Class<?> type) {
    return type.isArray() || (type.isInterface()
            && (Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)));
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
  protected Map<String, Object> findAutowireCandidates(@Nullable String beanName, Class<?> requiredType, DependencyDescriptor descriptor) {
    Set<String> candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(
            this, requiredType, true, descriptor.isEager());

    Map<String, Object> result = CollectionUtils.newLinkedHashMap(candidateNames.size());
    for (Map.Entry<Class<?>, Object> classObjectEntry : this.resolvableDependencies.entrySet()) {
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
      boolean multiple = indicatesArrayCollectionOrMap(requiredType);
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
  private void addCandidateEntry(Map<String, Object> candidates,
          String candidateName, DependencyDescriptor descriptor, Class<?> requiredType) {

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
    // Step 1: check primary candidate
    String primaryCandidate = determinePrimaryCandidate(candidates, requiredType);
    if (primaryCandidate != null) {
      return primaryCandidate;
    }
    // Step 2a: match bean name against declared dependency name
    String dependencyName = descriptor.getDependencyName();
    if (dependencyName != null) {
      for (String beanName : candidates.keySet()) {
        if (matchesBeanName(beanName, dependencyName)) {
          return beanName;
        }
      }
    }
    // Step 2b: match bean name against qualifier-suggested name
    String suggestedName = getAutowireCandidateResolver().getSuggestedName(descriptor);
    if (suggestedName != null) {
      for (String beanName : candidates.keySet()) {
        if (matchesBeanName(beanName, suggestedName)) {
          return beanName;
        }
      }
    }
    // Step 3: check highest priority candidate
    String priorityCandidate = determineHighestPriorityCandidate(candidates, requiredType);
    if (priorityCandidate != null) {
      return priorityCandidate;
    }
    // Step 4: pick directly registered dependency
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
      Object beanInstance = entry.getValue();
      if (beanInstance != null && resolvableDependencies.containsValue(beanInstance)) {
        return entry.getKey();
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
    // First pass: identify unique primary candidate
    for (Map.Entry<String, Object> entry : candidates.entrySet()) {
      String candidateBeanName = entry.getKey();
      Object beanInstance = entry.getValue();
      if (isPrimary(candidateBeanName, beanInstance)) {
        if (primaryBeanName != null) {
          boolean candidateLocal = containsBeanDefinition(candidateBeanName);
          boolean primaryLocal = containsBeanDefinition(primaryBeanName);
          if (candidateLocal == primaryLocal) {
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
    // Second pass: identify unique non-fallback candidate
    if (primaryBeanName == null) {
      for (String candidateBeanName : candidates.keySet()) {
        if (!isFallback(candidateBeanName)) {
          if (primaryBeanName != null) {
            return null;
          }
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
          if (highestPriority != null) {
            if (candidatePriority.equals(highestPriority)) {
              throw new NoUniqueBeanDefinitionException(requiredType, candidates.size(),
                      "Multiple beans found with the same priority ('%s') among candidates: %s"
                              .formatted(highestPriority, candidates.keySet()));
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
    if (containsBeanDefinition(transformedBeanName)) {
      return getMergedLocalBeanDefinition(transformedBeanName).isPrimary();
    }
    return getParentBeanFactory() instanceof StandardBeanFactory std
            && std.isPrimary(transformedBeanName, beanInstance);
  }

  /**
   * Return whether the bean definition for the given bean name has been
   * marked as a fallback bean.
   *
   * @param beanName the name of the bean
   */
  private boolean isFallback(String beanName) {
    String transformedBeanName = transformedBeanName(beanName);
    if (containsBeanDefinition(transformedBeanName)) {
      return getMergedLocalBeanDefinition(transformedBeanName).isFallback();
    }
    return getParentBeanFactory() instanceof StandardBeanFactory parent
            && parent.isFallback(transformedBeanName);
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
    return (beanName != null && candidateName != null &&
            (beanName.equals(candidateName) || (containsBeanDefinition(candidateName)
                    && beanName.equals(getMergedLocalBeanDefinition(candidateName).getFactoryBeanName()))));
  }

  /**
   * Determine whether there is a primary bean registered for the given dependency type,
   * not matching the given bean name.
   */
  private boolean hasPrimaryConflict(String beanName, Class<?> dependencyType) {
    for (String candidate : this.primaryBeanNames) {
      if (isTypeMatch(candidate, dependencyType) && !candidate.equals(beanName)) {
        return true;
      }
    }
    return getParentBeanFactory() instanceof StandardBeanFactory parent
            && parent.hasPrimaryConflict(beanName, dependencyType);
  }

  /**
   * Raise a NoSuchBeanDefinitionException or BeanNotOfRequiredTypeException
   * for an unresolvable dependency.
   */
  void raiseNoMatchingBeanFound(Class<?> type, DependencyDescriptor descriptor) throws BeansException {
    checkBeanNotOfRequiredType(type, descriptor);
    raiseNoMatchingBeanFound(descriptor);
  }

  static void raiseNoMatchingBeanFound(DependencyDescriptor descriptor) {
    throw new NoSuchBeanDefinitionException(descriptor.getResolvableType(),
            "expected at least 1 bean which qualifies as autowire candidate. Dependency annotations: "
                    + ObjectUtils.nullSafeToString(descriptor.getAnnotations()));
  }

  /**
   * Raise a BeanNotOfRequiredTypeException for an unresolvable dependency, if applicable,
   * i.e. if the target type of the bean would match but an exposed proxy doesn't.
   */
  private void checkBeanNotOfRequiredType(Class<?> type, DependencyDescriptor descriptor) {
    AutowireCandidateResolver candidateResolver = getAutowireCandidateResolver();
    for (String beanName : beanDefinitionNames) {
      try {
        RootBeanDefinition merged = getMergedLocalBeanDefinition(beanName);
        Class<?> targetType = merged.getTargetType();
        if (targetType != null && type.isAssignableFrom(targetType)
                && isAutowireCandidate(beanName, merged, descriptor, candidateResolver)) {
          // Probably a proxy interfering with target type match -> throw meaningful exception.
          Object beanInstance = getSingleton(beanName, false);
          Class<?> beanType = (beanInstance == null || beanInstance == NullValue.INSTANCE)
                  ? predictBeanType(beanName, merged) : beanInstance.getClass();
          if (beanType != null && !type.isAssignableFrom(beanType)) {
            throw new BeanNotOfRequiredTypeException(beanName, type, beanType);
          }
        }
      }
      catch (NoSuchBeanDefinitionException ex) {
        // Bean definition got removed while we were iterating -> ignore.
      }
    }

    if (getParentBeanFactory() instanceof StandardBeanFactory parent) {
      parent.checkBeanNotOfRequiredType(type, descriptor);
    }
  }

  /**
   * Create an {@link Optional} wrapper for the specified dependency.
   */
  private Optional<?> createOptionalDependency(DependencyDescriptor descriptor, @Nullable String beanName, final Object... args) {
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

      @Override
      public boolean usesStandardBeanLookup() {
        return ObjectUtils.isEmpty(args);
      }

    };

    Object result = doResolveDependency(descriptorToUse, beanName, null, null);
    return result instanceof Optional ? (Optional<?>) result : Optional.ofNullable(result);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(ObjectUtils.identityToString(this));
    sb.append(": defining beans [");
    sb.append(StringUtils.collectionToCommaDelimitedString(beanDefinitionNames));
    sb.append("]; ");
    BeanFactory parent = getParentBeanFactory();
    if (parent == null) {
      sb.append("root of factory hierarchy");
    }
    else {
      sb.append("parent: ").append(ObjectUtils.identityToString(parent));
    }
    return sb.toString();
  }

  //---------------------------------------------------------------------
  // Serialization support
  //---------------------------------------------------------------------

  @Serial
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    throw new NotSerializableException("StandardBeanFactory itself is not deserializable - " +
            "just a SerializedBeanFactoryReference is");
  }

  @Serial
  protected Object writeReplace() throws ObjectStreamException {
    if (this.serializationId != null) {
      return new SerializedBeanFactoryReference(this.serializationId);
    }
    else {
      throw new NotSerializableException("StandardBeanFactory has no serialization id");
    }
  }

  /**
   * Minimal id reference to the factory.
   * Resolved to the actual factory instance on deserialization.
   */
  private static class SerializedBeanFactoryReference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String id;

    public SerializedBeanFactoryReference(String id) {
      this.id = id;
    }

    @Serial
    private Object readResolve() {
      Reference<?> ref = serializableFactories.get(this.id);
      if (ref != null) {
        Object result = ref.get();
        if (result != null) {
          return result;
        }
      }
      // Lenient fallback: dummy factory in case of original factory not found...
      StandardBeanFactory dummyFactory = new StandardBeanFactory();
      dummyFactory.serializationId = this.id;
      return dummyFactory;
    }
  }

  private interface BeanObjectProvider<T> extends ObjectProvider<T>, Serializable { }

  /**
   * A dependency descriptor marker for nested elements.
   */
  private static class NestedDependencyDescriptor extends DependencyDescriptor {

    @Serial
    private static final long serialVersionUID = 1L;

    public NestedDependencyDescriptor(DependencyDescriptor original) {
      super(original);
      increaseNestingLevel();
    }

    @Override
    public boolean usesStandardBeanLookup() {
      return true;
    }

  }

  /**
   * A dependency descriptor for a multi-element declaration with nested elements.
   */
  private static class MultiElementDescriptor extends NestedDependencyDescriptor {

    @Serial
    private static final long serialVersionUID = 1L;

    public MultiElementDescriptor(DependencyDescriptor original) {
      super(original);
    }
  }

  /**
   * A dependency descriptor marker for stream access to multiple elements.
   */
  private static class StreamDependencyDescriptor extends DependencyDescriptor {

    @Serial
    private static final long serialVersionUID = 1L;

    private final boolean ordered;

    public StreamDependencyDescriptor(DependencyDescriptor original, boolean ordered) {
      super(original);
      this.ordered = ordered;
    }

    @Override
    public boolean isOrdered() {
      return this.ordered;
    }
  }

  /**
   * Serializable ObjectFactory/ObjectProvider for lazy resolution of a dependency.
   */
  private class DependencyObjectProvider implements BeanObjectProvider<Object> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Nullable
    private final String beanName;
    private final boolean optional;
    private final DependencyDescriptor descriptor;

    public DependencyObjectProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
      this.beanName = beanName;
      descriptor = new NestedDependencyDescriptor(descriptor);
      this.optional = descriptor.getDependencyType() == Optional.class;
      this.descriptor = descriptor;
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

            @Override
            public boolean usesStandardBeanLookup() {
              return true;
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
    public boolean ifAvailable(Consumer<Object> dependencyConsumer) throws BeansException {
      Object dependency = getIfAvailable();
      if (dependency != null) {
        try {
          dependencyConsumer.accept(dependency);
          return true;
        }
        catch (ScopeNotActiveException ex) {
          // Ignore resolved bean in non-active scope, even on scoped proxy invocation
        }
      }
      return false;
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

        @Override
        public boolean usesStandardBeanLookup() {
          return true;
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
    public boolean ifUnique(Consumer<Object> dependencyConsumer) throws BeansException {
      Object dependency = getIfUnique();
      if (dependency != null) {
        try {
          dependencyConsumer.accept(dependency);
          return true;
        }
        catch (ScopeNotActiveException ex) {
          // Ignore resolved bean in non-active scope, even on scoped proxy invocation
        }
      }
      return false;
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

    @Serial
    private static final long serialVersionUID = 1L;

    public Object createDependencyProvider(DependencyDescriptor descriptor, @Nullable String beanName) {
      return new Jsr330Provider(descriptor, beanName);
    }

    private class Jsr330Provider extends DependencyObjectProvider implements Provider<Object> {

      @Serial
      private static final long serialVersionUID = 1L;

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

  /**
   * An {@link cn.taketoday.core.OrderSourceProvider} implementation
   * that is aware of the bean metadata of the instances to sort.
   * <p>Lookup for the method factory of an instance to sort, if any, and let the
   * comparator retrieve the {@link Order}
   * value defined on it. This essentially allows for the following construct:
   *
   * <p>this class takes the {@link AbstractBeanDefinition#ORDER_ATTRIBUTE}
   * attribute into account.
   *
   * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
   * @since 4.0 2021/12/19 17:26
   */
  final class FactoryAwareOrderSourceProvider implements OrderSourceProvider {

    private final IdentityHashMap<Object, String> instancesToBeanNames;

    public FactoryAwareOrderSourceProvider(Map<String, ?> beans) {
      this.instancesToBeanNames = new IdentityHashMap<>();
      for (Map.Entry<String, ?> entry : beans.entrySet()) {
        instancesToBeanNames.put(entry.getValue(), entry.getKey());
      }
    }

    @Override
    @Nullable
    public Object getOrderSource(Object obj) {
      String beanName = this.instancesToBeanNames.get(obj);
      if (beanName == null) {
        return null;
      }
      try {
        var beanDefinition = (RootBeanDefinition) getMergedBeanDefinition(beanName);
        ArrayList<Object> sources = new ArrayList<>(3);
        Object orderAttribute = beanDefinition.getAttribute(AbstractBeanDefinition.ORDER_ATTRIBUTE);
        if (orderAttribute != null) {
          if (orderAttribute instanceof Integer order) {
            sources.add((Ordered) () -> order);
          }
          else {
            throw new IllegalStateException("Invalid value type for attribute '%s': %s"
                    .formatted(AbstractBeanDefinition.ORDER_ATTRIBUTE, orderAttribute.getClass().getName()));
          }
        }
        Method factoryMethod = beanDefinition.getResolvedFactoryMethod();
        if (factoryMethod != null) {
          sources.add(factoryMethod);
        }
        Class<?> targetType = beanDefinition.getTargetType();
        if (targetType != null && targetType != obj.getClass()) {
          sources.add(targetType);
        }
        return sources.toArray();
      }
      catch (NoSuchBeanDefinitionException ex) {
        return null;
      }
    }

  }

  private enum PreInstantiation {

    MAIN, BACKGROUND
  }

}
