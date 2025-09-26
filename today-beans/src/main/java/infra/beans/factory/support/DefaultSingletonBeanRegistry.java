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

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanCreationNotAllowedException;
import infra.beans.factory.BeanCurrentlyInCreationException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.SingletonBeanRegistry;
import infra.core.DefaultAliasRegistry;
import infra.lang.Assert;
import infra.lang.Constant;
import infra.lang.NullValue;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

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
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see DisposableBean
 * @see ConfigurableBeanFactory
 * @since 4.0 2021/10/1 22:47
 */
public class DefaultSingletonBeanRegistry extends DefaultAliasRegistry implements SingletonBeanRegistry {

  /** Maximum number of suppressed exceptions to preserve. */
  private static final int SUPPRESSED_EXCEPTIONS_LIMIT = 100;

  final ReentrantLock singletonLock = new ReentrantLock();

  /** Names of beans that are currently in creation. */
  private final Set<String> singletonsCurrentlyInCreation = ConcurrentHashMap.newKeySet(16);

  /** Names of beans currently excluded from in creation checks. */
  private final Set<String> inCreationCheckExclusions = ConcurrentHashMap.newKeySet(16);

  /** Set of registered singletons, containing the bean names in registration order. */
  private final Set<String> registeredSingletons = Collections.synchronizedSet(new LinkedHashSet<>(256));

  /** Disposable bean instances: bean name to disposable instance. */
  private final LinkedHashMap<String, DisposableBean> disposableBeans = new LinkedHashMap<>();

  /** Map between containing bean names: bean name to Set of bean names that the bean contains. */
  private final ConcurrentHashMap<String, Set<String>> containedBeanMap = new ConcurrentHashMap<>(16);

  /** Map between dependent bean names: bean name to Set of dependent bean names. */
  private final ConcurrentHashMap<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<>(64);

  /** Map between depending bean names: bean name to Set of bean names for the bean's dependencies. */
  private final ConcurrentHashMap<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<>(64);

  /** Cache of singleton objects: bean name to bean instance. */
  private final ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>(128);

  /** Cache of early singleton objects: bean name to bean instance. */
  private final ConcurrentHashMap<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(16);

  /** Cache of singleton factories: bean name to ObjectFactory. */
  private final ConcurrentHashMap<String, Supplier<?>> singletonFactories = new ConcurrentHashMap<>(16);

  /** Custom callbacks for singleton creation/registration. */
  private final ConcurrentHashMap<String, Consumer<Object>> singletonCallbacks = new ConcurrentHashMap<>(16);

  /** Specific lock for lenient creation tracking. */
  private final Lock lenientCreationLock = new ReentrantLock();

  /** Specific lock condition for lenient creation tracking. */
  private final Condition lenientCreationFinished = this.lenientCreationLock.newCondition();

  /** Names of beans that are currently in lenient creation. */
  private final HashSet<String> singletonsInLenientCreation = new HashSet<>();

  /** Map from one creation thread waiting on a lenient creation thread. */
  private final HashMap<Thread, Thread> lenientWaitingThreads = new HashMap<>();

  /** Map from bean name to actual creation thread for currently created beans. */
  private final ConcurrentHashMap<String, Thread> currentCreationThreads = new ConcurrentHashMap<>();

  /** Flag that indicates whether we're currently within destroySingletons. */
  private boolean singletonsCurrentlyInDestruction = false;

  /** Collection of suppressed Exceptions, available for associating related causes. */
  @Nullable
  private Set<Exception> suppressedExceptions;

  @Override
  public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
    Assert.notNull(beanName, "Bean name is required");
    Assert.notNull(singletonObject, "Singleton object is required");
    this.singletonLock.lock();
    try {
      addSingleton(beanName, singletonObject);
    }
    finally {
      this.singletonLock.unlock();
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
    Object oldObject = this.singletonObjects.putIfAbsent(beanName, singletonObject);
    if (oldObject != null) {
      throw new IllegalStateException("Could not register object [%s] under bean name '%s': there is already object [%s] bound"
              .formatted(singletonObject, beanName, oldObject));
    }
    this.singletonFactories.remove(beanName);
    this.earlySingletonObjects.remove(beanName);
    this.registeredSingletons.add(beanName);

    Consumer<Object> callback = this.singletonCallbacks.get(beanName);
    if (callback != null) {
      callback.accept(singletonObject);
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
    Assert.notNull(singletonFactory, "Singleton factory is required");
    this.singletonFactories.put(beanName, singletonFactory);
    this.earlySingletonObjects.remove(beanName);
    this.registeredSingletons.add(beanName);
  }

  @Override
  public void addSingletonCallback(String beanName, Consumer<Object> singletonConsumer) {
    this.singletonCallbacks.put(beanName, singletonConsumer);
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
   * @see NullValue#INSTANCE
   */
  @Nullable
  protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    // Quick check for existing instance without full singleton lock
    Object singletonObject = this.singletonObjects.get(beanName);
    if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
      singletonObject = this.earlySingletonObjects.get(beanName);
      if (singletonObject == null && allowEarlyReference) {
        if (!this.singletonLock.tryLock()) {
          // Avoid early singleton inference outside of original creation thread.
          return null;
        }
        try {
          // Consistent creation of early reference within full singleton lock.
          singletonObject = this.singletonObjects.get(beanName);
          if (singletonObject == null) {
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null) {
              Supplier<?> singletonFactory = this.singletonFactories.get(beanName);
              if (singletonFactory != null) {
                singletonObject = singletonFactory.get();
                // Singleton could have been added or removed in the meantime.
                if (this.singletonFactories.remove(beanName) != null) {
                  this.earlySingletonObjects.put(beanName, singletonObject);
                }
                else {
                  singletonObject = this.singletonObjects.get(beanName);
                }
              }
            }
          }
        }
        finally {
          this.singletonLock.unlock();
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
   * @param singletonFactory the Supplier to lazily create the singleton
   * with, if necessary
   * @return the registered singleton object
   */
  public Object getSingleton(String beanName, Supplier<?> singletonFactory) {
    Assert.notNull(beanName, "Bean name is required");

    Thread currentThread = Thread.currentThread();
    Boolean lockFlag = isCurrentThreadAllowedToHoldSingletonLock();
    boolean acquireLock = !Boolean.FALSE.equals(lockFlag);
    boolean locked = (acquireLock && this.singletonLock.tryLock());

    try {
      Object singletonObject = this.singletonObjects.get(beanName);
      if (singletonObject == null) {
        if (acquireLock && !locked) {
          if (Boolean.TRUE.equals(lockFlag)) {
            // Another thread is busy in a singleton factory callback, potentially blocked.
            // Fallback as of 5.0: process given singleton bean outside of singleton lock.
            // Thread-safe exposure is still guaranteed, there is just a risk of collisions
            // when triggering creation of other beans as dependencies of the current bean.
            if (log.isInfoEnabled()) {
              log.info("Obtaining singleton bean '{}' in thread \"{}\" while other thread holds singleton lock for other beans {}",
                      beanName, Thread.currentThread().getName(), this.singletonsCurrentlyInCreation);
            }
            this.lenientCreationLock.lock();
            try {
              if (log.isInfoEnabled()) {
                Set<String> lockedBeans = new HashSet<>(this.singletonsCurrentlyInCreation);
                lockedBeans.removeAll(this.singletonsInLenientCreation);
                log.info("Obtaining singleton bean '{}' in thread \"{}\" while other thread holds singleton " +
                        "lock for other beans {}", beanName, currentThread.getName(), lockedBeans);
              }
              this.singletonsInLenientCreation.add(beanName);
            }
            finally {
              this.lenientCreationLock.unlock();
            }
          }
          else {
            // No specific locking indication (outside a coordinated bootstrap) and
            // singleton lock currently held by some other creation method -> wait.
            this.singletonLock.lock();
            locked = true;
            // Singleton object might have possibly appeared in the meantime.
            singletonObject = this.singletonObjects.get(beanName);
            if (singletonObject != null) {
              return singletonObject;
            }
          }
        }

        if (this.singletonsCurrentlyInDestruction) {
          throw new BeanCreationNotAllowedException(beanName,
                  "Singleton bean creation not allowed while singletons of this factory are in destruction " +
                          "(Do not request a bean from a BeanFactory in a destroy method implementation!)");
        }
        if (log.isDebugEnabled()) {
          log.debug("Creating shared instance of singleton bean '{}'", beanName);
        }

        try {
          beforeSingletonCreation(beanName);
        }
        catch (BeanCurrentlyInCreationException ex) {
          this.lenientCreationLock.lock();
          try {
            while ((singletonObject = this.singletonObjects.get(beanName)) == null) {
              Thread otherThread = this.currentCreationThreads.get(beanName);
              if (otherThread != null && (otherThread == currentThread ||
                      checkDependentWaitingThreads(otherThread, currentThread))) {
                throw ex;
              }
              if (!this.singletonsInLenientCreation.contains(beanName)) {
                break;
              }
              if (otherThread != null) {
                this.lenientWaitingThreads.put(currentThread, otherThread);
              }
              try {
                this.lenientCreationFinished.await();
              }
              catch (InterruptedException ie) {
                currentThread.interrupt();
              }
              finally {
                if (otherThread != null) {
                  this.lenientWaitingThreads.remove(currentThread);
                }
              }
            }
          }
          finally {
            this.lenientCreationLock.unlock();
          }
          if (singletonObject != null) {
            return singletonObject;
          }
          if (locked) {
            throw ex;
          }
          // Try late locking for waiting on specific bean to be finished.
          this.singletonLock.lock();
          locked = true;
          // Lock-created singleton object should have appeared in the meantime.
          singletonObject = this.singletonObjects.get(beanName);
          if (singletonObject != null) {
            return singletonObject;
          }
          beforeSingletonCreation(beanName);
        }

        boolean newSingleton = false;
        boolean recordSuppressedExceptions = (locked && this.suppressedExceptions == null);
        if (recordSuppressedExceptions) {
          this.suppressedExceptions = new LinkedHashSet<>();
        }
        try {
          // Leniently created singleton object could have appeared in the meantime.
          singletonObject = this.singletonObjects.get(beanName);
          if (singletonObject == null) {
            this.currentCreationThreads.put(beanName, currentThread);
            try {
              singletonObject = singletonFactory.get();
            }
            finally {
              this.currentCreationThreads.remove(beanName);
            }
            newSingleton = true;
          }
        }
        catch (IllegalStateException ex) {
          // Has the singleton object implicitly appeared in the meantime ->
          // if yes, proceed with it since the exception indicates that state.
          singletonObject = this.singletonObjects.get(beanName);
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
          try {
            addSingleton(beanName, singletonObject);
          }
          catch (IllegalStateException ex) {
            // Leniently accept same instance if implicitly appeared.
            Object object = this.singletonObjects.get(beanName);
            if (singletonObject != object) {
              throw ex;
            }
          }
        }
      }
      return singletonObject;
    }
    finally {
      if (locked) {
        this.singletonLock.unlock();
      }
      this.lenientCreationLock.lock();
      try {
        this.singletonsInLenientCreation.remove(beanName);
        this.lenientWaitingThreads.entrySet().removeIf(
                entry -> entry.getValue() == currentThread);
        this.lenientCreationFinished.signalAll();
      }
      finally {
        this.lenientCreationLock.unlock();
      }
    }
  }

  private boolean checkDependentWaitingThreads(Thread waitingThread, Thread candidateThread) {
    Thread threadToCheck = waitingThread;
    while ((threadToCheck = this.lenientWaitingThreads.get(threadToCheck)) != null) {
      if (threadToCheck == candidateThread) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determine whether the current thread is allowed to hold the singleton lock.
   * <p>By default, all threads are forced to hold a full lock through {@code null}.
   * {@link StandardBeanFactory} overrides this to specifically handle its
   * threads during the pre-instantiation phase: {@code true} for the main thread,
   * {@code false} for managed background threads, and configuration-dependent
   * behavior for unmanaged threads.
   *
   * @return {@code true} if the current thread is explicitly allowed to hold the
   * lock but also accepts lenient fallback behavior, {@code false} if it is
   * explicitly not allowed to hold the lock and therefore forced to use lenient
   * fallback behavior, or {@code null} if there is no specific indication
   * (traditional behavior: forced to always hold a full lock)
   * @since 5.0
   */
  @Nullable
  protected Boolean isCurrentThreadAllowedToHoldSingletonLock() {
    return null;
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
    if (suppressedExceptions != null && suppressedExceptions.size() < SUPPRESSED_EXCEPTIONS_LIMIT) {
      suppressedExceptions.add(ex);
    }
  }

  /**
   * Remove the bean with the given name from the singleton registry, either on
   * regular destruction or on cleanup after early exposure when creation failed.
   *
   * @param beanName the name of the bean
   */
  public void removeSingleton(String beanName) {
    this.singletonObjects.remove(beanName);
    this.singletonFactories.remove(beanName);
    this.earlySingletonObjects.remove(beanName);
    this.registeredSingletons.remove(beanName);
  }

  @Override
  public boolean containsSingleton(String beanName) {
    return singletonObjects.containsKey(beanName);
  }

  @Override
  public String[] getSingletonNames() {
    return StringUtils.toStringArray(registeredSingletons);
  }

  @Override
  public int getSingletonCount() {
    return registeredSingletons.size();
  }

  public void setCurrentlyInCreation(String beanName, boolean inCreation) {
    Assert.notNull(beanName, "Bean name is required");
    if (!inCreation) {
      inCreationCheckExclusions.add(beanName);
    }
    else {
      inCreationCheckExclusions.remove(beanName);
    }
  }

  public boolean isCurrentlyInCreation(String beanName) {
    Assert.notNull(beanName, "Bean name is required");
    return (!inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
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
    return singletonsCurrentlyInCreation.contains(beanName);
  }

  /**
   * Callback before singleton creation.
   * <p>The default implementation register the singleton as currently in creation.
   *
   * @param beanName the name of the singleton about to be created
   * @see #isSingletonCurrentlyInCreation
   */
  protected void beforeSingletonCreation(String beanName) {
    if (!inCreationCheckExclusions.contains(beanName) && !singletonsCurrentlyInCreation.add(beanName)) {
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
    if (!inCreationCheckExclusions.contains(beanName) && !singletonsCurrentlyInCreation.remove(beanName)) {
      throw new IllegalStateException("Singleton '%s' isn't currently in creation".formatted(beanName));
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
    synchronized(disposableBeans) {
      disposableBeans.put(beanName, bean);
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
    synchronized(containedBeanMap) {
      Set<String> containedBeans = containedBeanMap.computeIfAbsent(containingBeanName, k -> new LinkedHashSet<>(8));
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

    synchronized(dependentBeanMap) {
      Set<String> dependentBeans = dependentBeanMap.computeIfAbsent(canonicalName,
              k -> new LinkedHashSet<>(8));
      if (!dependentBeans.add(dependentBeanName)) {
        return;
      }
    }

    synchronized(dependenciesForBeanMap) {
      Set<String> dependenciesForBean = dependenciesForBeanMap.computeIfAbsent(dependentBeanName,
              k -> new LinkedHashSet<>(8));
      dependenciesForBean.add(canonicalName);
    }
  }

  /**
   * Determine whether the specified dependent bean has been registered as
   * dependent on the given bean or on any of its transitive dependencies.
   *
   * @param beanName the name of the bean to check
   * @param dependentBeanName the name of the dependent bean
   */
  protected boolean isDependent(String beanName, String dependentBeanName) {
    synchronized(dependentBeanMap) {
      return isDependent(beanName, dependentBeanName, null);
    }
  }

  private boolean isDependent(String beanName, String dependentBeanName, @Nullable Set<String> alreadySeen) {
    if (alreadySeen != null && alreadySeen.contains(beanName)) {
      return false;
    }
    String canonicalName = canonicalName(beanName);
    Set<String> dependentBeans = dependentBeanMap.get(canonicalName);
    if (CollectionUtils.isEmpty(dependentBeans)) {
      return false;
    }
    if (dependentBeans.contains(dependentBeanName)) {
      return true;
    }
    if (alreadySeen == null) {
      alreadySeen = new HashSet<>();
    }
    alreadySeen.add(beanName);

    for (String transitiveDependency : dependentBeans) {
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
    return dependentBeanMap.containsKey(beanName);
  }

  /**
   * Return the names of all beans which depend on the specified bean, if any.
   *
   * @param beanName the name of the bean
   * @return the array of dependent bean names, or an empty array if none
   */
  public String[] getDependentBeans(String beanName) {
    Set<String> dependentBeans = dependentBeanMap.get(beanName);
    if (dependentBeans == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    synchronized(dependentBeanMap) {
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
    Set<String> dependenciesForBean = dependenciesForBeanMap.get(beanName);
    if (dependenciesForBean == null) {
      return Constant.EMPTY_STRING_ARRAY;
    }
    synchronized(dependenciesForBeanMap) {
      return StringUtils.toStringArray(dependenciesForBean);
    }
  }

  public void destroySingletons() {
    if (log.isTraceEnabled()) {
      log.trace("Destroying singletons in {}", this);
    }
    this.singletonsCurrentlyInDestruction = true;

    String[] disposableBeanNames;
    synchronized(disposableBeans) {
      disposableBeanNames = StringUtils.toStringArray(disposableBeans.keySet());
    }
    for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
      destroySingleton(disposableBeanNames[i]);
    }

    containedBeanMap.clear();
    dependentBeanMap.clear();
    dependenciesForBeanMap.clear();

    singletonLock.lock();
    try {
      clearSingletonCache();
    }
    finally {
      singletonLock.unlock();
    }
  }

  /**
   * Clear all cached singleton instances in this registry.
   */
  protected void clearSingletonCache() {
    this.singletonObjects.clear();
    this.singletonFactories.clear();
    this.earlySingletonObjects.clear();
    this.registeredSingletons.clear();
    this.singletonsCurrentlyInDestruction = false;
  }

  /**
   * Destroy the given bean. Delegates to {@code destroyBean}
   * if a corresponding disposable bean instance is found.
   *
   * @param beanName the name of the bean
   * @see #destroyBean
   */
  public void destroySingleton(String beanName) {
    // Destroy the corresponding DisposableBean instance.
    // This also triggers the destruction of dependent beans.
    DisposableBean disposableBean;
    synchronized(this.disposableBeans) {
      disposableBean = this.disposableBeans.remove(beanName);
    }
    destroyBean(beanName, disposableBean);

    // destroySingletons() removes all singleton instances at the end,
    // leniently tolerating late retrieval during the shutdown phase.
    if (!this.singletonsCurrentlyInDestruction) {
      // For an individual destruction, remove the registered instance now.
      // this happens after the current bean's destruction step,
      // allowing for late bean retrieval by on-demand suppliers etc.
      if (this.currentCreationThreads.get(beanName) == Thread.currentThread()) {
        // Local remove after failed creation step -> without singleton lock
        // since bean creation may have happened leniently without any lock.
        removeSingleton(beanName);
      }
      else {
        this.singletonLock.lock();
        try {
          removeSingleton(beanName);
        }
        finally {
          this.singletonLock.unlock();
        }
      }
    }
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
    synchronized(dependentBeanMap) {
      // Within full synchronization in order to guarantee a disconnected Set
      dependencies = dependentBeanMap.remove(beanName);
    }
    if (dependencies != null) {
      if (log.isDebugEnabled()) {
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
    synchronized(containedBeanMap) {
      // Within full synchronization in order to guarantee a disconnected Set
      containedBeans = containedBeanMap.remove(beanName);
    }
    if (containedBeans != null) {
      for (String containedBeanName : containedBeans) {
        destroySingleton(containedBeanName);
      }
    }

    // Remove destroyed bean from other beans' dependencies.
    synchronized(dependentBeanMap) {
      Iterator<Map.Entry<String, Set<String>>> iterator = dependentBeanMap.entrySet().iterator();
      while (iterator.hasNext()) {
        Map.Entry<String, Set<String>> entry = iterator.next();
        Set<String> dependenciesToClean = entry.getValue();
        dependenciesToClean.remove(beanName);
        if (dependenciesToClean.isEmpty()) {
          iterator.remove();
        }
      }
    }

    // Remove destroyed bean's prepared dependency information.
    dependenciesForBeanMap.remove(beanName);
  }

  @Override
  public void registerSingleton(Object bean) {
    registerSingleton(BeanDefinitionBuilder.defaultBeanName(bean.getClass()), bean);
  }

}
