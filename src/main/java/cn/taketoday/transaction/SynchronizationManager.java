/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Assert;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.annotation.AnnotationAwareOrderComparator;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * @author TODAY <br>
 * 2018-10-09 10:22
 */
public abstract class SynchronizationManager {

  private static final Logger log = LoggerFactory.getLogger(SynchronizationManager.class);

  private static final ThreadLocal<SynchronizationMetaData> META_DATA = ThreadLocal.withInitial(SynchronizationMetaData::new);

  public static final class SynchronizationMetaData implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private Boolean active;
    private Boolean readOnly; //currentTransactionReadOnly

    private Integer isolationLevel;

    private transient Map<Object, Object> resources;

    private transient List<TransactionSynchronization> synchronizations;

    // ------------------------------------------------

    /**
     * Return all resources that are bound to the current thread.
     * <p>
     * Mainly for debugging purposes. Resource managers should always invoke
     * {@code hasResource} for a specific resource key that they are interested in.
     *
     * @return a Map with resource keys (usually the resource factory) and resource
     * values (usually the active resource object), or an empty Map if there
     * are currently no resources bound
     *
     * @see #hasResource
     */
    public Map<Object, Object> getResources() {
      return resources;
    }

    public void setResources(Map<Object, Object> resources) {
      this.resources = resources;
    }

    /**
     * Check if there is a resource for the given key bound to the current thread.
     *
     * @param key
     *         the key to check (usually the resource factory)
     *
     * @return if there is a value bound to the current thread
     *
     * @see ResourceTransactionManager#getResourceFactory()
     */
    public boolean hasResource(final Object key) {
      return doGetResource(key) != null;
    }

    /**
     * Retrieve a resource for the given key that is bound to the current thread.
     *
     * @param key
     *         the key to check (usually the resource factory)
     *
     * @return a value bound to the current thread (usually the active resource
     * object), or {@code null} if none
     *
     * @see ResourceTransactionManager#getResourceFactory()
     */
    public Object getResource(final Object key) {
      final Object value = doGetResource(key);
      if (value != null && log.isTraceEnabled()) {
        log.trace("Retrieved value [{}] for key [{}] bound to thread: [{}]",
                  value, key, Thread.currentThread().getName());
      }
      return value;
    }

    /**
     * Actually check the value of the resource that is bound for the given key.
     */
    private Object doGetResource(final Object key) {
      final Map<Object, Object> map = this.resources;

      if (map == null) {
        return null;
      }

      Object value = map.get(key);
      // Transparently remove ResourceHolder that was marked as void...
      if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
        map.remove(key);
        // Remove entire ThreadLocal if empty...
        if (map.isEmpty()) {
          resources = null;
        }
        value = null;
      }
      return value;
    }

    /**
     * Bind the given resource for the given key to the current thread.
     *
     * @param key
     *         the key to bind the value to (usually the resource factory)
     * @param value
     *         the value to bind (usually the active resource object)
     *
     * @throws IllegalStateException
     *         if there is already a value bound to the thread
     * @see ResourceTransactionManager#getResourceFactory()
     */
    public void bindResource(Object key, Object value) {
      Map<Object, Object> map = resources;
      if (map == null) {
        resources = map = new HashMap<>(8);
      }
      Object oldValue = map.put(key, value);
      // Transparently suppress a ResourceHolder that was marked as void...
      if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
        oldValue = null;
      }
      if (oldValue != null) {
        throw new IllegalStateException(
                "Already value [" + oldValue + "] for key [" + key + "] bound to thread ["
                        + Thread.currentThread().getName() + "]");
      }
    }

    /**
     * Unbind a resource for the given key from the current thread.
     *
     * @param key
     *         the key to unbind (usually the resource factory)
     *
     * @return the previously bound value (usually the active resource object)
     *
     * @throws IllegalStateException
     *         if there is no value bound to the thread
     * @see ResourceTransactionManager#getResourceFactory()
     */
    public Object unbindResource(Object key) {
      Object value = doUnbindResource(key);
      if (value == null) {
        throw new IllegalStateException(
                "No value for key [" + key + "] bound to thread [" + Thread.currentThread().getName() + "]");
      }
      return value;
    }

    /**
     * Unbind a resource for the given key from the current thread.
     *
     * @param key
     *         the key to unbind (usually the resource factory)
     *
     * @return the previously bound value, or {@code null} if none bound
     */
    public Object unbindResourceIfPossible(Object key) {
      return doUnbindResource(key);
    }

    /**
     * Actually remove the value of the resource that is bound for the given key.
     */
    private Object doUnbindResource(final Object key) {
      final Map<Object, Object> map = resources;

      if (CollectionUtils.isEmpty(map)) {
        return null;
      }
      Object value = map.remove(key);
      // Remove entire ThreadLocal if empty...
      if (map.isEmpty()) {
        resources = null;
      }
      // Transparently suppress a ResourceHolder that was marked as void...
      if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
        value = null;
      }
      return value;
    }

    // --------------------------------------------------

    public boolean isActive() {
      return synchronizations != null;
    }

    /**
     * Activate transaction synchronization for the current thread. Called by a
     * transaction manager on transaction begin.
     *
     * @throws IllegalStateException
     *         if synchronization is already active
     */
    public void initSynchronization() {
      if (isActive()) {
        throw new IllegalStateException("Cannot activate transaction synchronization - cause its already active");
      }
      if (log.isDebugEnabled()) {
        log.debug("Initializing transaction synchronization");
      }
      this.synchronizations = new ArrayList<>(8);
    }

    public void registerSynchronization(final TransactionSynchronization synchronization) {

      if (isActive()) {
        final List<TransactionSynchronization> list = getSynchronizations();
        list.add(synchronization);
        AnnotationAwareOrderComparator.sort(list);
      }
      else {
        throw new IllegalStateException("Transaction synchronization is not active");
      }
    }

    public void clearSynchronization() {
      if (isActive()) {
        if (log.isDebugEnabled()) {
          log.debug("Clearing transaction synchronization");
        }
        this.synchronizations = null;
      }
      else {
        throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
      }
    }

    // -------------------------

    public List<TransactionSynchronization> getSynchronizations() {
      final List<TransactionSynchronization> synchs = synchronizations;
      Assert.state(synchs != null, "Transaction synchronization is not active");
      return synchs;
    }

    public void setSynchronizations(List<TransactionSynchronization> synchronizations) {
      this.synchronizations = synchronizations;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    /**
     * Return whether the current transaction is marked as read-only. To be called
     * by resource management code when preparing a newly created resource (for
     * example, a Hibernate Session).
     * <p>
     * Note that transaction synchronizations receive the read-only flag as argument
     * for the {@code beforeCommit} callback, to be able to suppress change
     * detection on commit. The present method is meant to be used for earlier
     * read-only checks, for example to set the flush mode of a Hibernate Session to
     * "FlushMode.NEVER" upfront.
     *
     * @see TransactionDefinition#isReadOnly()
     * @see TransactionSynchronization#beforeCommit(SynchronizationMetaData, boolean)
     */
    public boolean isReadOnly() {
      return readOnly != null;
    }

    public void setReadOnly(Boolean readOnly) {
      this.readOnly = readOnly;
    }

    public Integer getIsolationLevel() {
      return isolationLevel;
    }

    public void setIsolationLevel(Integer isolationLevel) {
      this.isolationLevel = isolationLevel;
    }

    public boolean isActualActive() {
      return active != null;
    }

    public void setActualActive(boolean active) {
      this.active = active ? Boolean.TRUE : null;
    }

    public void clear() {
      if (log.isDebugEnabled()) {
        log.debug("Clear the entire transaction synchronization state for the current thread");
      }
      this.name = null;
      this.active = null;
      this.readOnly = null;
      this.isolationLevel = null;
      this.synchronizations = null;
    }

    // -----------------------------------------

    /**
     * Trigger {@code flush} callbacks on all currently registered synchronizations.
     *
     * @throws RuntimeException
     *         if thrown by a {@code flush} callback
     * @see TransactionSynchronization#flush(SynchronizationMetaData)
     */
    public void triggerFlush() {
      for (final TransactionSynchronization synchronization : getSynchronizations()) {
        synchronization.flush(this);
      }
    }

    /**
     * Trigger {@code beforeCommit} callbacks on all currently registered
     * synchronizations.
     *
     * @param readOnly
     *         whether the transaction is defined as read-only transaction
     *
     * @throws RuntimeException
     *         if thrown by a {@code beforeCommit} callback
     * @see TransactionSynchronization#beforeCommit(SynchronizationMetaData, boolean)
     */
    public void triggerBeforeCommit(final boolean readOnly) {
      for (final TransactionSynchronization synchronization : getSynchronizations()) {
        synchronization.beforeCommit(this, readOnly);
      }
    }

    /**
     * Trigger {@code beforeCompletion} callbacks on all currently registered
     * synchronizations.
     *
     * @see TransactionSynchronization#beforeCompletion(SynchronizationMetaData)
     */
    public void triggerBeforeCompletion() {
      for (final TransactionSynchronization synchronization : getSynchronizations()) {
        try {
          synchronization.beforeCompletion(this);
        }
        catch (Throwable tsex) {
          log.error("TransactionSynchronization.beforeCompletion threw exception", tsex);
        }
      }
    }

    /**
     * Trigger {@code afterCommit} callbacks on all currently registered
     * synchronizations.
     *
     * @throws RuntimeException
     *         if thrown by a {@code afterCommit} callback
     * @see SynchronizationManager#getSynchronizations()
     * @see TransactionSynchronization#afterCommit(SynchronizationMetaData,)
     */
    public void triggerAfterCommit() {
      for (final TransactionSynchronization synchronization : getSynchronizations()) {
        synchronization.afterCommit(this);
      }
    }

    /**
     * Actually invoke the {@code afterCompletion} methods of the given  TransactionSynchronization objects.
     *
     * @param completionStatus
     *         the completion status according to the constants in the
     *         TransactionSynchronization interface
     *
     * @see TransactionSynchronization#afterCompletion(SynchronizationMetaData, int)
     * @see TransactionSynchronization#STATUS_COMMITTED
     * @see TransactionSynchronization#STATUS_ROLLED_BACK
     * @see TransactionSynchronization#STATUS_UNKNOWN
     */
    public void triggerAfterCompletion(final int completionStatus) {
      for (final TransactionSynchronization synchronization : getSynchronizations()) {
        try {
          synchronization.afterCompletion(this, completionStatus);
        }
        catch (Throwable tsex) {
          log.error("TransactionSynchronization.afterCompletion threw exception", tsex);
        }
      }
    }

    public void invokeAfterCompletion(final List<TransactionSynchronization> syncs, final int completionStatus) {
      if (syncs != null) {
        for (final TransactionSynchronization synchronization : syncs) {
          try {
            synchronization.afterCompletion(this, completionStatus);
          }
          catch (Throwable tsex) {
            log.error("TransactionSynchronization.afterCompletion threw exception", tsex);
          }
        }
      }
    }

    // -------------------------------------------------------------
  }

  public static SynchronizationMetaData getMetaData() {
    return META_DATA.get();
  }

  /**
   * Return all resources that are bound to the current thread.
   * <p>
   * Mainly for debugging purposes. Resource managers should always invoke
   * {@code hasResource} for a specific resource key that they are interested in.
   *
   * @return a Map with resource keys (usually the resource factory) and resource
   * values (usually the active resource object), or an empty Map if there
   * are currently no resources bound
   *
   * @see #hasResource
   */
  public static Map<Object, Object> getResourceMap() {
    return getMetaData().resources;
  }

  /**
   * Check if there is a resource for the given key bound to the current thread.
   *
   * @param key
   *         the key to check (usually the resource factory)
   *
   * @return if there is a value bound to the current thread
   *
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public static boolean hasResource(final Object key) {
    return getMetaData().hasResource(key);
  }

  /**
   * Retrieve a resource for the given key that is bound to the current thread.
   *
   * @param key
   *         the key to check (usually the resource factory)
   *
   * @return a value bound to the current thread (usually the active resource
   * object), or {@code null} if none
   *
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public static Object getResource(final Object key) {
    return getMetaData().getResource(key);
  }

  /**
   * Bind the given resource for the given key to the current thread.
   *
   * @param key
   *         the key to bind the value to (usually the resource factory)
   * @param value
   *         the value to bind (usually the active resource object)
   *
   * @throws IllegalStateException
   *         if there is already a value bound to the thread
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public static void bindResource(Object key, Object value) {
    getMetaData().bindResource(key, value);
  }

  /**
   * Unbind a resource for the given key from the current thread.
   *
   * @param key
   *         the key to unbind (usually the resource factory)
   *
   * @return the previously bound value (usually the active resource object)
   *
   * @throws IllegalStateException
   *         if there is no value bound to the thread
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public static Object unbindResource(Object key) {
    return getMetaData().unbindResource(key);
  }

  /**
   * Unbind a resource for the given key from the current thread.
   *
   * @param key
   *         the key to unbind (usually the resource factory)
   *
   * @return the previously bound value, or {@code null} if none bound
   */
  public static Object unbindResourceIfPossible(Object key) {
    return getMetaData().doUnbindResource(key);
  }

  // -------------------------------------------------------------------------
  // Management of transaction synchronizations
  // -------------------------------------------------------------------------

  /**
   * Return if transaction synchronization is active for the current thread. Can
   * be called before register to avoid unnecessary instance creation.
   *
   * @see #registerSynchronization
   */
  public static boolean isActive() {
    return getMetaData().isActive();
  }

  public static void initSynchronization() {
    getMetaData().initSynchronization();
  }

  /**
   * Register a new transaction synchronization for the current thread. Typically
   * called by resource management code.
   * <p>
   * Note that synchronizations can implement the {@link Ordered} interface. They
   * will be executed in an order according to their order value (if any).
   *
   * @param synchronization
   *         the synchronization object to register
   *
   * @throws IllegalStateException
   *         if transaction synchronization is not active
   */
  public static void registerSynchronization(TransactionSynchronization synchronization) {
    getMetaData().registerSynchronization(synchronization);
  }

  /**
   * Return an unmodifiable snapshot list of all registered synchronizations for
   * the current thread.
   *
   * @return unmodifiable List of TransactionSynchronization instances
   *
   * @throws IllegalStateException
   *         if synchronization is not active
   * @see TransactionSynchronization
   */
  public static List<TransactionSynchronization> getSynchronizations() {
    return getMetaData().getSynchronizations();
  }

  /**
   * Deactivate transaction synchronization for the current thread. Called by the
   * transaction manager on transaction cleanup.
   *
   * @throws IllegalStateException
   *         if synchronization is not active
   */
  public static void clearSynchronization() {
    getMetaData().clearSynchronization();
  }

  /**
   * Clear the entire transaction synchronization state for the current thread:
   * registered synchronizations as well as the various transaction
   * characteristics.
   *
   * @see #clearSynchronization()
   */
  public static void clear() {
    getMetaData().clear();
  }

}
