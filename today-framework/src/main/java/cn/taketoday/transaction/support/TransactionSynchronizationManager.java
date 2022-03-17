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

package cn.taketoday.transaction.support;

import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Nullable;

/**
 * Central delegate that manages resources and transaction synchronizations per thread.
 * To be used by resource management code but not by typical application code.
 *
 * <p>Supports one resource per key without overwriting, that is, a resource needs
 * to be removed before a new one can be set for the same key.
 * Supports a list of transaction synchronizations if synchronization is active.
 *
 * <p>Resource management code should check for thread-bound resources, e.g. JDBC
 * Connections or Hibernate Sessions, via {@code getResource}. Such code is
 * normally not supposed to bind resources to threads, as this is the responsibility
 * of transaction managers. A further option is to lazily bind on first use if
 * transaction synchronization is active, for performing transactions that span
 * an arbitrary number of resources.
 *
 * <p>Transaction synchronization must be activated and deactivated by a transaction
 * manager via {@link #initSynchronization()} and {@link #clearSynchronization()}.
 * This is automatically supported by {@link AbstractPlatformTransactionManager},
 * and thus by all standard Framework transaction managers, such as
 * {@link cn.taketoday.transaction.jta.JtaTransactionManager} and
 * {@link cn.taketoday.jdbc.datasource.DataSourceTransactionManager}.
 *
 * <p>Resource management code should only register synchronizations when this
 * manager is active, which can be checked via {@link #isSynchronizationActive};
 * it should perform immediate resource cleanup else. If transaction synchronization
 * isn't active, there is either no current transaction, or the transaction manager
 * doesn't support transaction synchronization.
 *
 * <p>Synchronization is for example used to always return the same resources
 * within a JTA transaction, e.g. a JDBC Connection or a Hibernate Session for
 * any given DataSource or SessionFactory, respectively.
 *
 * @author Juergen Hoeller
 * @see #isSynchronizationActive
 * @see #registerSynchronization
 * @see TransactionSynchronization
 * @see AbstractPlatformTransactionManager#setTransactionSynchronization
 * @see cn.taketoday.transaction.jta.JtaTransactionManager
 * @see cn.taketoday.jdbc.datasource.DataSourceTransactionManager
 * @see cn.taketoday.jdbc.datasource.DataSourceUtils#getConnection
 * @since 4.0
 */
public abstract class TransactionSynchronizationManager {

  private static final ThreadLocal<SynchronizationInfo> META_DATA = ThreadLocal.withInitial(SynchronizationInfo::new);

  //-------------------------------------------------------------------------
  // Management of transaction-associated resource handles
  //-------------------------------------------------------------------------

  public static SynchronizationInfo getSynchronizationInfo() {
    return META_DATA.get();
  }

  /**
   * Return all resources that are bound to the current thread.
   * <p>Mainly for debugging purposes. Resource managers should always invoke
   * {@code hasResource} for a specific resource key that they are interested in.
   *
   * @return a Map with resource keys (usually the resource factory) and resource
   * values (usually the active resource object), or an empty Map if there are
   * currently no resources bound
   * @see #hasResource
   */
  public static Map<Object, Object> getResourceMap() {
    return getSynchronizationInfo().getResourceMap();
  }

  /**
   * Check if there is a resource for the given key bound to the current thread.
   *
   * @param key the key to check (usually the resource factory)
   * @return if there is a value bound to the current thread
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public static boolean hasResource(Object key) {
    return getSynchronizationInfo().hasResource(key);
  }

  /**
   * Retrieve a resource for the given key that is bound to the current thread.
   *
   * @param key the key to check (usually the resource factory)
   * @return a value bound to the current thread (usually the active
   * resource object), or {@code null} if none
   * @see ResourceTransactionManager#getResourceFactory()
   */
  @Nullable
  public static Object getResource(Object key) {
    return getSynchronizationInfo().getResource(key);
  }

  /**
   * Bind the given resource for the given key to the current thread.
   *
   * @param key the key to bind the value to (usually the resource factory)
   * @param value the value to bind (usually the active resource object)
   * @throws IllegalStateException if there is already a value bound to the thread
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public static void bindResource(Object key, Object value) throws IllegalStateException {
    getSynchronizationInfo().bindResource(key, value);
  }

  /**
   * Unbind a resource for the given key from the current thread.
   *
   * @param key the key to unbind (usually the resource factory)
   * @return the previously bound value (usually the active resource object)
   * @throws IllegalStateException if there is no value bound to the thread
   * @see ResourceTransactionManager#getResourceFactory()
   */
  public static Object unbindResource(Object key) throws IllegalStateException {
    return getSynchronizationInfo().unbindResource(key);
  }

  /**
   * Unbind a resource for the given key from the current thread.
   *
   * @param key the key to unbind (usually the resource factory)
   * @return the previously bound value, or {@code null} if none bound
   */
  @Nullable
  public static Object unbindResourceIfPossible(Object key) {
    return getSynchronizationInfo().unbindResourceIfPossible(key);
  }

  //-------------------------------------------------------------------------
  // Management of transaction synchronizations
  //-------------------------------------------------------------------------

  /**
   * Return if transaction synchronization is active for the current thread.
   * Can be called before register to avoid unnecessary instance creation.
   *
   * @see #registerSynchronization
   */
  public static boolean isSynchronizationActive() {
    return getSynchronizationInfo().isSynchronizationActive();
  }

  /**
   * Activate transaction synchronization for the current thread.
   * Called by a transaction manager on transaction begin.
   *
   * @throws IllegalStateException if synchronization is already active
   */
  public static void initSynchronization() throws IllegalStateException {
    getSynchronizationInfo().initSynchronization();
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
  public static void registerSynchronization(TransactionSynchronization synchronization) throws IllegalStateException {
    getSynchronizationInfo().registerSynchronization(synchronization);
  }

  /**
   * Return an unmodifiable snapshot list of all registered synchronizations
   * for the current thread.
   *
   * @return unmodifiable List of TransactionSynchronization instances
   * @throws IllegalStateException if synchronization is not active
   * @see TransactionSynchronization
   */
  public static List<TransactionSynchronization> getSynchronizations() throws IllegalStateException {
    return getSynchronizationInfo().getSynchronizations();
  }

  /**
   * Deactivate transaction synchronization for the current thread.
   * Called by the transaction manager on transaction cleanup.
   *
   * @throws IllegalStateException if synchronization is not active
   */
  public static void clearSynchronization() throws IllegalStateException {
    getSynchronizationInfo().clearSynchronization();
  }

  //-------------------------------------------------------------------------
  // Exposure of transaction characteristics
  //-------------------------------------------------------------------------

  /**
   * Expose the name of the current transaction, if any.
   * Called by the transaction manager on transaction begin and on cleanup.
   *
   * @param name the name of the transaction, or {@code null} to reset it
   * @see cn.taketoday.transaction.TransactionDefinition#getName()
   */
  public static void setCurrentTransactionName(@Nullable String name) {
    getSynchronizationInfo().setCurrentTransactionName(name);
  }

  /**
   * Return the name of the current transaction, or {@code null} if none set.
   * To be called by resource management code for optimizations per use case,
   * for example to optimize fetch strategies for specific named transactions.
   *
   * @see cn.taketoday.transaction.TransactionDefinition#getName()
   */
  @Nullable
  public static String getCurrentTransactionName() {
    return getSynchronizationInfo().getCurrentTransactionName();
  }

  /**
   * Expose a read-only flag for the current transaction.
   * Called by the transaction manager on transaction begin and on cleanup.
   *
   * @param readOnly {@code true} to mark the current transaction
   * as read-only; {@code false} to reset such a read-only marker
   * @see cn.taketoday.transaction.TransactionDefinition#isReadOnly()
   */
  public static void setCurrentTransactionReadOnly(boolean readOnly) {
    getSynchronizationInfo().setCurrentTransactionReadOnly(readOnly);
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
  public static boolean isCurrentTransactionReadOnly() {
    return getSynchronizationInfo().isCurrentTransactionReadOnly();
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
  public static void setCurrentTransactionIsolationLevel(@Nullable Integer isolationLevel) {
    getSynchronizationInfo().setCurrentTransactionIsolationLevel(isolationLevel);
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
  public static Integer getCurrentTransactionIsolationLevel() {
    return getSynchronizationInfo().getCurrentTransactionIsolationLevel();
  }

  /**
   * Expose whether there currently is an actual transaction active.
   * Called by the transaction manager on transaction begin and on cleanup.
   *
   * @param active {@code true} to mark the current thread as being associated
   * with an actual transaction; {@code false} to reset that marker
   */
  public static void setActualTransactionActive(boolean active) {
    getSynchronizationInfo().setActualTransactionActive(active);
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
  public static boolean isActualTransactionActive() {
    return getSynchronizationInfo().isActualTransactionActive();
  }

  /**
   * Clear the entire transaction synchronization state for the current thread:
   * registered synchronizations as well as the various transaction characteristics.
   *
   * @see #clearSynchronization()
   * @see #setCurrentTransactionName
   * @see #setCurrentTransactionReadOnly
   * @see #setCurrentTransactionIsolationLevel
   * @see #setActualTransactionActive
   */
  public static void clear() {
    getSynchronizationInfo().clear();
  }

}
