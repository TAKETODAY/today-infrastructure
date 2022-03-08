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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanCreationNotAllowedException;
import cn.taketoday.beans.factory.BeanCurrentlyInCreationException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.DisposableBean;
import cn.taketoday.beans.factory.config.SingletonBeanRegistry;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.core.DefaultAliasRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Constant;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of {@link DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for {@link BeanFactory}
 * implementations, factoring out the common management of singleton
 * bean instances. Note that the {@link ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link StandardBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @author TODAY 2021/10/1 22:47
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see DisposableBean
 * @see ConfigurableBeanFactory
 * @since 4.0
 */
public class DefaultSingletonBeanRegistry extends DefaultAliasRegistry implements SingletonBeanRegistry {
  private static final Logger log = LoggerFactory.getLogger(DefaultSingletonBeanRegistry.class);

  /** Maximum number of suppressed exceptions to preserve. */
  private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;

  /** Cache of singleton objects: bean name to bean instance. */
  private final ConcurrentHashMap<String, Object> singletons = new ConcurrentHashMap<>(128);

  /** Cache of singleton factories: bean name to ObjectFactory. */
  private final HashMap<String, Supplier<?>> singletonFactories = new HashMap<>(16);

  /** Cache of early singleton objects: bean name to bean instance. */
  private final ConcurrentHashMap<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

  /** Set of registered singletons, containing the bean names in registration order. */
  private final LinkedHashSet<String> registeredSingletons = new LinkedHashSet<>(256);

  /** Names of beans that are currently in creation. */
  private final Set<String> singletonsCurrentlyInCreation =
          Collections.newSetFromMap(new ConcurrentHashMap<>(16));

  /** Names of beans currently excluded from in creation checks. */
  private final Set<String> inCreationCheckExclusions =
          Collections.newSetFromMap(new ConcurrentHashMap<>(16));

  /** Collection of suppressed Exceptions, available for associating related causes. */
  @Nullable
  private Set<Exception> suppressedExceptions;

  /** Flag that indicates whether we're currently within destroySingletons. */
  private boolean singletonsCurrentlyInDestruction = false;

  /** Disposable bean instances: bean name to disposable instance. */
  private final LinkedHashMap<String, Object> disposableBeans = new LinkedHashMap<>();

  /** Map between containing bean names: bean name to Set of bean names that the bean contains. */
  private final ConcurrentHashMap<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

  /** Map between dependent bean names: bean name to Set of dependent bean names. */
  private final ConcurrentHashMap<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

  /** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
  private final ConcurrentHashMap<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);

  @Override
  public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
    Assert.notNull(beanName, "Bean name must not be null");
    Assert.notNull(singletonObject, "Singleton object must not be null");
    synchronized(this.singletons) {
      Object oldObject = this.singletons.get(beanName);
      if (oldObject != null) {
        throw new IllegalStateException(
                "Could not register object [" + singletonObject +
                        "] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
      }
      addSingleton(beanName, singletonObject);
    }
  }

  /**
   * Add the given singleton object to the singleton cache of this factory.
   * <p>To be called for eager registration of singletons.
   *
   * @param beanName the name of the bean
   * @param singletonObject the singleton object
   */
  protected void addSingleton(String beanName, Object singletonObject) {
    synchronized(this.singletons) {
      this.singletons.put(beanName, singletonObject);
      this.singletonFactories.remove(beanName);
      this.earlySingletonObjects.remove(beanName);
      this.registeredSingletons.add(beanName);
    }
  }

  /**
   * Add the given singleton factory for building the specified singleton
   * if necessary.
   * <p>To be called for eager registration of singletons, e.g. to be able to
   * resolve circular references.
   *
   * @param beanName the name of the bean
   * @param singletonFactory the factory for the singleton object
   */
  protected void addSingletonFactory(String beanName, Supplier<?> singletonFactory) {
    Assert.notNull(singletonFactory, "Singleton factory must not be null");
    synchronized(this.singletons) {
      if (!this.singletons.containsKey(beanName)) {
        this.singletonFactories.put(beanName, singletonFactory);
        this.earlySingletonObjects.remove(beanName);
        this.registeredSingletons.add(beanName);
      }
    }
  }

  @Override
  @Nullable
  public Object getSingleton(String beanName) {
    return getSingleton(beanName, true);
  }

  /**
   * Return the (raw) singleton object registered under the given name.
   * <p>Checks already instantiated singletons and also allows for an early
   * reference to a currently created singleton (resolving a circular reference).
   *
   * @param beanName the name of the bean to look for
   * @param allowEarlyReference whether early references should be created or not
   * @return the registered singleton object, or {@code null} if none found
   */
  @Nullable
  protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // Quick check for existing instance without full singleton lock
    Object singletonObject = this.singletons.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
      singletonObject = this.earlySingletonObjects.get(beanName);
      if (singletonObject == null && allowEarlyReference) {
        synchronized(this.singletons) {
          // Consistent creation of early reference within full singleton lock
          singletonObject = this.singletons.get(beanName);
          if (singletonObject == null) {
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null) {
              Supplier<?> singletonFactory = this.singletonFactories.get(beanName);
              if (singletonFactory != null) {
                singletonObject = singletonFactory.get();
                this.earlySingletonObjects.put(beanName, singletonObject);
                this.singletonFactories.remove(beanName);
              }
            }
          }
        }
      }
    }
    return singletonObject;
  }

  /**
   * Return the (raw) singleton object registered under the given name,
   * creating and registering a new one if none registered yet.
   *
   * @param beanName the name of the bean
   * @param singletonSupplier the ObjectFactory to lazily create the singleton
   * with, if necessary
   * @return the registered singleton object
   */
  public Object getSingleton(String beanName, Supplier<?> singletonSupplier) {
    Assert.notNull(beanName, "Bean name must not be null");
    synchronized(this.singletons) {
      Object singletonObject = this.singletons.get(beanName);
      if (singletonObject == null) {
        if (this.singletonsCurrentlyInDestruction) {
          throw new BeanCreationNotAllowedException(
                  beanName,
                  "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                          "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
        }
        if (log.isDebugEnabled()) {
          log.debug("Creating shared instance of singleton bean '{}'", beanName);
        }
        beforeSingletonCreation(beanName);
        boolean newSingleton = false;
        boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
        if (recordSuppressedExceptions) {
          this.suppressedExceptions = new LinkedHashSet<>();
        }
        try {
          singletonObject = singletonSupplier.get();
          newSingleton = true;
        }
        catch (IllegalStateException ex) {
          // Has the singleton object implicitly appeared in the meantime ->
          // if yes, proceed with it since the exception indicates that state.
          singletonObject = this.singletons.get(beanName);
          if (singletonObject == null) {
            throw ex;
          }
        }
        catch (BeanCreationException ex) {
          if (recordSuppressedExceptions) {
            for (Exception suppressedException : this.suppressedExceptions) {
              ex.addRelatedCause(suppressedException);
            }
          }
          throw ex;
        }
        finally {
          if (recordSuppressedExceptions) {
            this.suppressedExceptions = null;
          }
          afterSingletonCreation(beanName);
        }
        if (newSingleton) {
          addSingleton(beanName, singletonObject);
        }
      }
      return singletonObject;
    }
  }

  /**
   * Register an exception that happened to get suppressed during the creation of a
   * singleton bean instance, e.g. a temporary circular reference resolution problem.
   * <p>The default implementation preserves any given exception in this registry's
   * collection of suppressed exceptions, up to a limit of 100 exceptions, adding
   * them as related causes to an eventual top-level {@link BeanCreationException}.
   *
   * @param ex the Exception to register
   * @see BeanCreationException#getRelatedCauses()
   */
  protected void onSuppressedException(Exception ex) {
    synchronized(this.singletons) {
      if (this.suppressedExceptions != null && this.suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
        this.suppressedExceptions.add(ex);
      }
    }
  }

  /**
   * Remove the bean with the given name from the singleton cache of this factory,
   * to be able to clean up eager registration of a singleton if creation failed.
   *
   * @param beanName the name of the bean
   * @see #getSingletonMutex()
   */
  public void removeSingleton(String beanName) {
    synchronized(this.singletons) {
      this.singletons.remove(beanName);
      this.singletonFactories.remove(beanName);
      this.earlySingletonObjects.remove(beanName);
      this.registeredSingletons.remove(beanName);
    }
  }

  @Override
  public boolean containsSingleton(String beanName) {
    return this.singletons.containsKey(beanName);
  }

  @Override
  public String[] getSingletonNames() {
    synchronized(this.singletons) {
      return StringUtils.toStringArray(this.registeredSingletons);
    }
  }

  @Override
  public int getSingletonCount() {
    synchronized(this.singletons) {
      return this.registeredSingletons.size();
    }
  }

  public void setCurrentlyInCreation(String beanName, boolean inCreation) {
    Assert.notNull(beanName, "Bean name must not be null");
    if (!inCreation) {
      this.inCreationCheckExclusions.add(beanName);
    }
    else {
      this.inCreationCheckExclusions.remove(beanName);
    }
  }

  public boolean isCurrentlyInCreation(String beanName) {
    Assert.notNull(beanName, "Bean name must not be null");
    return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
  }

  protected boolean isActuallyInCreation(String beanName) {
    return isSingletonCurrentlyInCreation(beanName);
  }

  /**
   * Return whether the specified singleton bean is currently in creation
   * (within the entire factory).
   *
   * @param beanName the name of the bean
   */
  public boolean isSingletonCurrentlyInCreation(String beanName) {
    return this.singletonsCurrentlyInCreation.contains(beanName);
  }

  /**
   * Callback before singleton creation.
   * <p>The default implementation register the singleton as currently in creation.
   *
   * @param beanName the name of the singleton about to be created
   * @see #isSingletonCurrentlyInCreation
   */
  protected void beforeSingletonCreation(String beanName) {
    if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
      throw new BeanCurrentlyInCreationException(beanName);
    }
  }

  /**
   * Callback after singleton creation.
   * <p>The default implementation marks the singleton as not in creation anymore.
   *
   * @param beanName the name of the singleton that has been created
   * @see #isSingletonCurrentlyInCreation
   */
  protected void afterSingletonCreation(String beanName) {
    if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
      throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
    }
  }

  /**
   * Add the given bean to the list of disposable beans in this registry.
   * <p>Disposable beans usually correspond to registered singletons,
   * matching the bean name but potentially being a different instance
   * (for example, a DisposableBean adapter for a singleton that does not
   * naturally implement  DisposableBean interface).
   *
   * @param beanName the name of the bean
   * @param bean the bean instance
   */
  public void registerDisposableBean(String beanName, DisposableBean bean) {
    synchronized(this.disposableBeans) {
      this.disposableBeans.put(beanName, bean);
    }
  }

  /**
   * Register a containment relationship between two beans,
   * e.g. between an inner bean and its containing outer bean.
   * <p>Also registers the containing bean as dependent on the contained bean
   * in terms of destruction order.
   *
   * @param containedBeanName the name of the contained (inner) bean
   * @param containingBeanName the name of the containing (outer) bean
   * @see #registerDependentBean
   */
  public void registerContainedBean(String containedBeanName, String containingBeanName) {
    synchronized(this.containedBeanMap) {
      Set<String> containedBeans =
              this.containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
      if (!containedBeans.add(containedBeanName)) {
        return;
      }
    }
    registerDependentBean(containedBeanName, containingBeanName);
  }

  /**
   * Register a dependent bean for the given bean,
   * to be destroyed before the given bean is destroyed.
   *
   * @param beanName the name of the bean
   * @param dependentBeanName the name of the dependent bean
   */
  public void registerDependentBean(String beanName, String dependentBeanName) {
    String canonicalName = canonicalName(beanName);

    synchronized(this.dependentBeanMap) {
      Set<String> dependentBeans =
              this.dependentBeanMap.computeIfAbsent(canonicalName, k -> new LinkedHashSet<>(8));
      if (!dependentBeans.add(dependentBeanName)) {
        return;
      }
    }

    synchronized(this.dependenciesForBeanMap) {
      Set<String> dependenciesForBean =
              this.dependenciesForBeanMap.computeIfAbsent(dependentBeanName, k -> new LinkedHashSet<>(8));
      dependenciesForBean.add(canonicalName);
    }
  }

  /**
   * Determine whether the specified dependent bean has been registered as
   * dependent on the given bean or on any of its transitive dependencies.
   *
   * @param beanName the name of the bean to check
   * @param dependentBeanName the name of the dependent bean
   * @since 4.0
   */
  protected boolean isDependent(String beanName, String dependentBeanName) {
    synchronized(this.dependentBeanMap) {
      return isDependent(beanName, dependentBeanName, null);
    }
  }

  private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
    if (alreadySeen != null && alreadySeen.contains(beanName)) {
      return false;
    }
    String canonicalName = canonicalName(beanName);
    Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
    if (dependentBeans == null) {
      return false;
    }
    if (dependentBeans.contains(dependentBeanName)) {
      return true;
    }
    for (String transitiveDependency : dependentBeans) {
      if (alreadySeen == null) {
        alreadySeen = new HashSet<>();
      }
      alreadySeen.add(beanName);
      if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether a dependent bean has been registered for the given name.
   *
   * @param beanName the name of the bean to check
   */
  protected boolean hasDependentBean(String beanName) {
    return this.dependentBeanMap.containsKey(beanName);
  }

  /**
   * Return the names of all beans which depend on the specified bean, if any.
   *
   * @param beanName the name of the bean
   * @return the array of dependent bean names, or an empty array if none
   */
  public String[] getDependentBeans(String beanName) {
    Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
    if (dependentBeans == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    synchronized(this.dependentBeanMap) {
      return StringUtils.toStringArray(dependentBeans);
    }
  }

  /**
   * Return the names of all beans that the specified bean depends on, if any.
   *
   * @param beanName the name of the bean
   * @return the array of names of beans which the bean depends on,
   * or an empty array if none
   */
  public String[] getDependenciesForBean(String beanName) {
    Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
    if (dependenciesForBean == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    synchronized(this.dependenciesForBeanMap) {
      return StringUtils.toStringArray(dependenciesForBean);
    }
  }

  public void destroySingletons() {
    if (log.isTraceEnabled()) {
      log.trace("Destroying singletons in {}", this);
    }
    synchronized(this.singletons) {
      this.singletonsCurrentlyInDestruction = true;
    }

    String[] disposableBeanNames;
    synchronized(this.disposableBeans) {
      disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
    }
    for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
      destroySingleton(disposableBeanNames[i]);
    }

    this.containedBeanMap.clear();
    this.dependentBeanMap.clear();
    this.dependenciesForBeanMap.clear();

    clearSingletonCache();
  }

  /**
   * Clear all cached singleton instances in this registry.
   *
   * @since 4.0
   */
  protected void clearSingletonCache() {
    synchronized(this.singletons) {
      this.singletons.clear();
      this.singletonFactories.clear();
      this.earlySingletonObjects.clear();
      this.registeredSingletons.clear();
      this.singletonsCurrentlyInDestruction = false;
    }
  }

  /**
   * Destroy the given bean. Delegates to {@code destroyBean}
   * if a corresponding disposable bean instance is found.
   *
   * @param beanName the name of the bean
   * @see #destroyBean
   */
  public void destroySingleton(String beanName) {
    // Remove a registered singleton of the given name, if any.
    removeSingleton(beanName);

    // Destroy the corresponding DisposableBean instance.
    DisposableBean disposableBean;
    synchronized(this.disposableBeans) {
      disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
    }
    destroyBean(beanName, disposableBean);
  }

  /**
   * Destroy the given bean. Must destroy beans that depend on the given
   * bean before the bean itself. Should not throw any exceptions.
   *
   * @param beanName the name of the bean
   * @param bean the bean instance to destroy
   */
  protected void destroyBean(String beanName, @Nullable DisposableBean bean) {
    // Trigger destruction of dependent beans first...
    Set<String> dependencies;
    synchronized(this.dependentBeanMap) {
      // Within full synchronization in order to guarantee a disconnected Set
      dependencies = this.dependentBeanMap.remove(beanName);
    }
    if (dependencies != null) {
      if (log.isTraceEnabled()) {
        log.trace("Retrieved dependent beans for bean '{}': {}", beanName, dependencies);
      }
      for (String dependentBeanName : dependencies) {
        destroySingleton(dependentBeanName);
      }
    }

    // Actually destroy the bean now...
    if (bean != null) {
      try {
        bean.destroy();
      }
      catch (Throwable ex) {
        log.warn("Destruction of bean with name '{}' threw an exception", beanName, ex);
      }
    }

    // Trigger destruction of contained beans...
    Set<String> containedBeans;
    synchronized(this.containedBeanMap) {
      // Within full synchronization in order to guarantee a disconnected Set
      containedBeans = this.containedBeanMap.remove(beanName);
    }
    if (containedBeans != null) {
      for (String containedBeanName : containedBeans) {
        destroySingleton(containedBeanName);
      }
    }

    // Remove destroyed bean from other beans' dependencies.
    synchronized(this.dependentBeanMap) {
      for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext(); ) {
        Map.Entry<String, Set<String>> entry = it.next();
        Set<String> dependenciesToClean = entry.getValue();
        dependenciesToClean.remove(beanName);
        if (dependenciesToClean.isEmpty()) {
          it.remove();
        }
      }
    }

    // Remove destroyed bean's prepared dependency information.
    this.dependenciesForBeanMap.remove(beanName);
  }

  /**
   * Exposes the singleton mutex to subclasses and external collaborators.
   * <p>Subclasses should synchronize on the given Object if they perform
   * any sort of extended singleton creation phase. In particular, subclasses
   * should <i>not</i> have their own mutexes involved in singleton creation,
   * to avoid the potential for deadlocks in lazy-init situations.
   */
  @Override
  public final Object getSingletonMutex() {
    return this.singletons;
  }

  @Override
  public void registerSingleton(Object bean) {
    registerSingleton(BeanDefinitionBuilder.defaultBeanName(bean.getClass()), bean);
  }

  @Override
  public Map<String, Object> getSingletons() {
    return singletons;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getSingleton(Class<T> requiredType) {
    synchronized(singletons) {
      for (Object value : singletons.values()) {
        if (requiredType.isInstance(value)) {
          return (T) value;
        }
      }
    }
    return null;
  }

}
