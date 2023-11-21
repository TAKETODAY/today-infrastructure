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

package cn.taketoday.transaction.support;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.core.OrderComparator;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/5 11:13
 */
public class SynchronizationInfo implements Serializable {
  private static final Logger log = LoggerFactory.getLogger(SynchronizationInfo.class);

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  private String currentTransactionName; // Current transaction name

  @Nullable
  private Boolean actualTransactionActive; // Actual transaction active

  @Nullable
  private Boolean currentTransactionReadOnly; // Current transaction read-only status

  @Nullable
  private Integer currentTransactionIsolationLevel; // Current transaction isolation level

  // Transactional resources

  @Nullable
  private transient Map<Object, Object> resourceMap;

  @Nullable
  private transient LinkedHashSet<TransactionSynchronization> synchronizations; // Transaction synchronizations

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
   * @see #hasResource
   */
  public Map<Object, Object> getResourceMap() {
    return resourceMap != null ? Collections.unmodifiableMap(resourceMap) : Collections.emptyMap();
  }

  /**
   * Check if there is a resource for the given key bound to the current thread.
   *
   * @param key the key to check (usually the resource factory)
   * @return if there is a value bound to the current thread
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public boolean hasResource(final Object key) {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    return doGetResource(actualKey) != null;
  }

  /**
   * Retrieve a resource for the given key that is bound to the current thread.
   *
   * @param key the key to check (usually the resource factory)
   * @return a value bound to the current thread (usually the active resource
   * object), or {@code null} if none
   * @see ResourceTransactionManager#getResourceFactory()
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T getResource(final Object key) {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    final Object value = doGetResource(actualKey);
    if (value != null && log.isTraceEnabled()) {
      log.trace("Retrieved value [{}] for key [{}] bound to thread: [{}]",
              value, key, Thread.currentThread().getName());
    }
    return (T) value;
  }

  /**
   * Actually check the value of the resource that is bound for the given key.
   */
  @Nullable
  private Object doGetResource(final Object actualKey) {
    if (resourceMap == null) {
      return null;
    }

    Object value = resourceMap.get(actualKey);
    // Transparently remove ResourceHolder that was marked as void...
    if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
      resourceMap.remove(actualKey);
      // Remove entire ThreadLocal if empty...
      if (resourceMap.isEmpty()) {
        resourceMap = null;
      }
      value = null;
    }
    return value;
  }

  /**
   * Bind the given resource for the given key to the current thread.
   *
   * @param key the key to bind the value to (usually the resource factory)
   * @param value the value to bind (usually the active resource object)
   * @throws IllegalStateException if there is already a value bound to the thread
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public void bindResource(Object key, Object value) throws IllegalStateException {
    Assert.notNull(value, "Value is required");
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    Map<Object, Object> map = resourceMap;
    if (map == null) {
      resourceMap = map = new HashMap<>(8);
    }
    Object oldValue = map.put(actualKey, value);
    // Transparently suppress a ResourceHolder that was marked as void...
    if (oldValue instanceof ResourceHolder && ((ResourceHolder) oldValue).isVoid()) {
      oldValue = null;
    }
    if (oldValue != null) {
      throw new IllegalStateException(
              "Already value [" + oldValue + "] for key [" + actualKey + "] bound to thread ["
                      + Thread.currentThread().getName() + "]");
    }
  }

  /**
   * Unbind a resource for the given key from the current thread.
   *
   * @param key the key to unbind (usually the resource factory)
   * @return the previously bound value (usually the active resource object)
   * @throws IllegalStateException if there is no value bound to the thread
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public Object unbindResource(Object key) {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    Object value = doUnbindResource(actualKey);
    if (value == null) {
      throw new IllegalStateException("No value for key [" + actualKey + "] bound to thread");
    }
    return value;
  }

  /**
   * Unbind a resource for the given key from the current thread.
   *
   * @param key the key to unbind (usually the resource factory)
   * @return the previously bound value, or {@code null} if none bound
   */
  @Nullable
  public Object unbindResourceIfPossible(Object key) {
    Object actualKey = TransactionSynchronizationUtils.unwrapResourceIfNecessary(key);
    return doUnbindResource(actualKey);
  }

  /**
   * Actually remove the value of the resource that is bound for the given key.
   */
  @Nullable
  private Object doUnbindResource(final Object key) {
    final Map<Object, Object> map = resourceMap;

    if (CollectionUtils.isEmpty(map)) {
      return null;
    }
    Object value = map.remove(key);
    // Remove entire ThreadLocal if empty...
    if (map.isEmpty()) {
      resourceMap = null;
    }
    // Transparently suppress a ResourceHolder that was marked as void...
    if (value instanceof ResourceHolder && ((ResourceHolder) value).isVoid()) {
      value = null;
    }
    return value;
  }

  // --------------------------------------------------

  /**
   * Return if transaction synchronization is active for the current thread.
   * Can be called before register to avoid unnecessary instance creation.
   *
   * @see #registerSynchronization
   */
  public boolean isSynchronizationActive() {
    return synchronizations != null;
  }

  /**
   * Activate transaction synchronization for the current thread. Called by a
   * transaction manager on transaction begin.
   *
   * @throws IllegalStateException if synchronization is already active
   */
  public void initSynchronization() {
    if (isSynchronizationActive()) {
      throw new IllegalStateException("Cannot activate transaction synchronization - already active");
    }
    if (log.isDebugEnabled()) {
      log.debug("Initializing transaction synchronization");
    }
    this.synchronizations = new LinkedHashSet<>(8);
  }

  /**
   * Register a new transaction synchronization for the current thread.
   * Typically called by resource management code.
   * <p>Note that synchronizations can implement the
   * {@link cn.taketoday.core.Ordered} interface.
   * They will be executed in an order according to their order value (if any).
   *
   * @param synchronization the synchronization object to register
   * @throws IllegalStateException if transaction synchronization is not active
   * @see cn.taketoday.core.Ordered
   */
  public void registerSynchronization(TransactionSynchronization synchronization) throws IllegalStateException {
    Assert.notNull(synchronization, "TransactionSynchronization is required");
    if (synchronizations == null) {
      throw new IllegalStateException("Transaction synchronization is not active");
    }
    synchronizations.add(synchronization);
  }

  /**
   * Deactivate transaction synchronization for the current thread.
   * Called by the transaction manager on transaction cleanup.
   *
   * @throws IllegalStateException if synchronization is not active
   */
  public void clearSynchronization() throws IllegalStateException {
    if (isSynchronizationActive()) {
      if (log.isDebugEnabled()) {
        log.debug("Clearing transaction synchronization");
      }
      this.synchronizations = null;
    }
    else {
      throw new IllegalStateException("Cannot deactivate transaction synchronization - not active");
    }
  }

  /**
   * Return an unmodifiable snapshot list of all registered synchronizations
   * for the current thread.
   *
   * @return unmodifiable List of TransactionSynchronization instances
   * @throws IllegalStateException if synchronization is not active
   * @see TransactionSynchronization
   */
  public List<TransactionSynchronization> getSynchronizations() {
    Set<TransactionSynchronization> synchs = synchronizations;
    if (synchs == null) {
      throw new IllegalStateException("Transaction synchronization is not active");
    }
    // Return unmodifiable snapshot, to avoid ConcurrentModificationExceptions
    // while iterating and invoking synchronization callbacks that in turn
    // might register further synchronizations.
    if (synchs.isEmpty()) {
      return Collections.emptyList();
    }
    else if (synchs.size() == 1) {
      return Collections.singletonList(synchs.iterator().next());
    }
    else {
      // Sort lazily here, not in registerSynchronization.
      ArrayList<TransactionSynchronization> sortedSynchs = new ArrayList<>(synchs);
      OrderComparator.sort(sortedSynchs);
      return Collections.unmodifiableList(sortedSynchs);
    }
  }

  /**
   * Return the name of the current transaction, or {@code null} if none set.
   * To be called by resource management code for optimizations per use case,
   * for example to optimize fetch strategies for specific named transactions.
   *
   * @see cn.taketoday.transaction.TransactionDefinition#getName()
   */
  @Nullable
  public String getCurrentTransactionName() {
    return currentTransactionName;
  }

  /**
   * Expose the name of the current transaction, if any.
   * Called by the transaction manager on transaction begin and on cleanup.
   *
   * @param name the name of the transaction, or {@code null} to reset it
   * @see cn.taketoday.transaction.TransactionDefinition#getName()
   */
  public void setCurrentTransactionName(@Nullable String name) {
    this.currentTransactionName = name;
  }

  /**
   * Return whether the current transaction is marked as read-only.
   * To be called by resource management code when preparing a newly
   * created resource (for example, a Hibernate Session).
   * <p>Note that transaction synchronizations receive the read-only flag
   * as argument for the {@code beforeCommit} callback, to be able
   * to suppress change detection on commit. The present method is meant
   * to be used for earlier read-only checks, for example to set the
   * flush mode of a Hibernate Session to "FlushMode.MANUAL" upfront.
   *
   * @see cn.taketoday.transaction.TransactionDefinition#isReadOnly()
   * @see TransactionSynchronization#beforeCommit(boolean)
   */
  public boolean isCurrentTransactionReadOnly() {
    return currentTransactionReadOnly != null;
  }

  /**
   * Expose a read-only flag for the current transaction.
   * Called by the transaction manager on transaction begin and on cleanup.
   *
   * @param readOnly {@code true} to mark the current transaction
   * as read-only; {@code false} to reset such a read-only marker
   * @see cn.taketoday.transaction.TransactionDefinition#isReadOnly()
   */
  public void setCurrentTransactionReadOnly(Boolean readOnly) {
    this.currentTransactionReadOnly = readOnly ? Boolean.TRUE : null;
  }

  /**
   * Return the isolation level for the current transaction, if any.
   * To be called by resource management code when preparing a newly
   * created resource (for example, a JDBC Connection).
   *
   * @return the currently exposed isolation level, according to the
   * JDBC Connection constants (equivalent to the corresponding Framework
   * TransactionDefinition constants), or {@code null} if none
   * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
   * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
   * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
   * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
   * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
   * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
   * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
   * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
   * @see cn.taketoday.transaction.TransactionDefinition#getIsolationLevel()
   */
  @Nullable
  public Integer getCurrentTransactionIsolationLevel() {
    return currentTransactionIsolationLevel;
  }

  /**
   * Expose an isolation level for the current transaction.
   * Called by the transaction manager on transaction begin and on cleanup.
   *
   * @param isolationLevel the isolation level to expose, according to the
   * JDBC Connection constants (equivalent to the corresponding Framework
   * TransactionDefinition constants), or {@code null} to reset it
   * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
   * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
   * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
   * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
   * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
   * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
   * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
   * @see cn.taketoday.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
   * @see cn.taketoday.transaction.TransactionDefinition#getIsolationLevel()
   */
  public void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
    this.currentTransactionIsolationLevel = isolationLevel;
  }

  /**
   * Return whether there currently is an actual transaction active.
   * This indicates whether the current thread is associated with an actual
   * transaction rather than just with active transaction synchronization.
   * <p>To be called by resource management code that wants to discriminate
   * between active transaction synchronization (with or without backing
   * resource transaction; also on PROPAGATION_SUPPORTS) and an actual
   * transaction being active (with backing resource transaction;
   * on PROPAGATION_REQUIRED, PROPAGATION_REQUIRES_NEW, etc).
   *
   * @see #isSynchronizationActive()
   */
  public boolean isActualTransactionActive() {
    return actualTransactionActive != null;
  }

  /**
   * Expose whether there currently is an actual transaction active.
   * Called by the transaction manager on transaction begin and on cleanup.
   *
   * @param active {@code true} to mark the current thread as being associated
   * with an actual transaction; {@code false} to reset that marker
   */
  public void setActualTransactionActive(boolean active) {
    this.actualTransactionActive = active ? Boolean.TRUE : null;
  }

  /**
   * Clear the entire transaction synchronization state for the current thread:
   * registered synchronizations as well as the various transaction
   * characteristics.
   *
   * @see #clearSynchronization()
   */
  public void clear() {
    if (log.isDebugEnabled()) {
      log.debug("Clear the entire transaction synchronization state for the current thread");
    }
    this.synchronizations = null;
    this.currentTransactionName = null;
    this.actualTransactionActive = null;
    this.currentTransactionReadOnly = null;
    this.currentTransactionIsolationLevel = null;
  }

  // -----------------------------------------

  /**
   * Trigger {@code flush} callbacks on all currently registered synchronizations.
   *
   * @throws RuntimeException if thrown by a {@code flush} callback
   * @see TransactionSynchronization#flush()
   */
  public void triggerFlush() {
    for (TransactionSynchronization synchronization : getSynchronizations()) {
      synchronization.flush();
    }
  }

  /**
   * Trigger {@code beforeCommit} callbacks on all currently registered
   * synchronizations.
   *
   * @param readOnly whether the transaction is defined as read-only transaction
   * @throws RuntimeException if thrown by a {@code beforeCommit} callback
   * @see TransactionSynchronization#beforeCommit(boolean)
   */
  public void triggerBeforeCommit(final boolean readOnly) {
    for (TransactionSynchronization synchronization : getSynchronizations()) {
      synchronization.beforeCommit(readOnly);
    }
  }

  /**
   * Trigger {@code beforeCompletion} callbacks on all currently registered
   * synchronizations.
   *
   * @see TransactionSynchronization#beforeCompletion()
   */
  public void triggerBeforeCompletion() {
    for (TransactionSynchronization synchronization : getSynchronizations()) {
      try {
        synchronization.beforeCompletion();
      }
      catch (Throwable tsex) {
        log.error("TransactionSynchronization.beforeCompletion threw exception", tsex);
      }
    }
  }

}
