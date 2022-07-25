/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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
package cn.taketoday.transaction;

import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.support.SynchronizationInfo;
import cn.taketoday.transaction.support.TransactionSynchronization;
import cn.taketoday.transaction.support.TransactionSynchronizationManager;

/**
 * Interface that defines transaction properties.
 * Based on the propagation behavior definitions analogous to EJB CMT attributes.
 *
 * <p>Note that isolation level and timeout settings will not get applied unless
 * an actual new transaction gets started. As only {@link #PROPAGATION_REQUIRED},
 * {@link #PROPAGATION_REQUIRES_NEW} and {@link #PROPAGATION_NESTED} can cause
 * that, it usually doesn't make sense to specify those settings in other cases.
 * Furthermore, be aware that not all transaction managers will support those
 * advanced features and thus might throw corresponding exceptions when given
 * non-default values.
 *
 * <p>The {@link #isReadOnly() read-only flag} applies to any transaction context,
 * whether backed by an actual resource transaction or operating non-transactionally
 * at the resource level. In the latter case, the flag will only apply to managed
 * resources within the application, such as a Hibernate {@code Session}.
 *
 * @author Juergen Hoeller
 * @author TODAY 2018-10-09 11:51
 * @see PlatformTransactionManager#getTransaction(TransactionDefinition)
 */
public interface TransactionDefinition {

  /**
   * Support a current transaction; create a new one if none exists.
   * Analogous to the EJB transaction attribute of the same name.
   * <p>This is typically the default setting of a transaction definition,
   * and typically defines a transaction synchronization scope.
   */
  int PROPAGATION_REQUIRED = 0;

  /**
   * Support a current transaction; execute non-transactionally if none exists.
   * Analogous to the EJB transaction attribute of the same name.
   * <p><b>NOTE:</b> For transaction managers with transaction synchronization,
   * {@code PROPAGATION_SUPPORTS} is slightly different from no transaction
   * at all, as it defines a transaction scope that synchronization might apply to.
   * As a consequence, the same resources (a JDBC {@code Connection}, a
   * Hibernate {@code Session}, etc) will be shared for the entire specified
   * scope. Note that the exact behavior depends on the actual synchronization
   * configuration of the transaction manager!
   * <p>In general, use {@code PROPAGATION_SUPPORTS} with care! In particular, do
   * not rely on {@code PROPAGATION_REQUIRED} or {@code PROPAGATION_REQUIRES_NEW}
   * <i>within</i> a {@code PROPAGATION_SUPPORTS} scope (which may lead to
   * synchronization conflicts at runtime). If such nesting is unavoidable, make sure
   * to configure your transaction manager appropriately (typically switching to
   * "synchronization on actual transaction").
   *
   * @see cn.taketoday.transaction.support.AbstractPlatformTransactionManager#setTransactionSynchronization
   * @see cn.taketoday.transaction.support.AbstractPlatformTransactionManager#SYNCHRONIZATION_ON_ACTUAL_TRANSACTION
   */
  int PROPAGATION_SUPPORTS = 1;

  /**
   * Support a current transaction; throw an exception if no current transaction
   * exists. Analogous to the EJB transaction attribute of the same name.
   * <p>Note that transaction synchronization within a {@code PROPAGATION_MANDATORY}
   * scope will always be driven by the surrounding transaction.
   */
  int PROPAGATION_MANDATORY = 2;

  /**
   * Create a new transaction, suspending the current transaction if one exists.
   * Analogous to the EJB transaction attribute of the same name.
   * <p>A {@code PROPAGATION_REQUIRES_NEW} scope always defines its own
   * transaction synchronizations. Existing synchronizations will be suspended
   * and resumed appropriately.
   */
  int PROPAGATION_REQUIRES_NEW = 3;

  /**
   * Do not support a current transaction; rather always execute non-transactionally.
   * Analogous to the EJB transaction attribute of the same name.
   * <p>Note that transaction synchronization is <i>not</i> available within a
   * {@code PROPAGATION_NOT_SUPPORTED} scope. Existing synchronizations
   * will be suspended and resumed appropriately.
   */
  int PROPAGATION_NOT_SUPPORTED = 4;

  /**
   * Do not support a current transaction; throw an exception if a current transaction
   * exists. Analogous to the EJB transaction attribute of the same name.
   * <p>Note that transaction synchronization is <i>not</i> available within a
   * {@code PROPAGATION_NEVER} scope.
   */
  int PROPAGATION_NEVER = 5;

  /**
   * Execute within a nested transaction if a current transaction exists,
   * behave like {@link #PROPAGATION_REQUIRED} otherwise. There is no
   * analogous feature in EJB.
   * <p><b>NOTE:</b> Actual creation of a nested transaction will only work on
   * specific transaction managers. Out of the box, this only applies to the JDBC
   * {@link cn.taketoday.jdbc.datasource.DataSourceTransactionManager}
   * when working on a JDBC 3.0 driver. Some JTA providers might support
   * nested transactions as well.
   *
   * @see cn.taketoday.jdbc.datasource.DataSourceTransactionManager
   */
  int PROPAGATION_NESTED = 6;

  /**
   * Use the default isolation level of the underlying datastore.
   * All other levels correspond to the JDBC isolation levels.
   *
   * @see java.sql.Connection
   */
  int ISOLATION_DEFAULT = -1;

  /**
   * Indicates that dirty reads, non-repeatable reads and phantom reads
   * can occur.
   * <p>This level allows a row changed by one transaction to be read by another
   * transaction before any changes in that row have been committed (a "dirty read").
   * If any of the changes are rolled back, the second transaction will have
   * retrieved an invalid row.
   *
   * @see java.sql.Connection#TRANSACTION_READ_UNCOMMITTED
   */
  int ISOLATION_READ_UNCOMMITTED = 1;  // same as java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;

  /**
   * Indicates that dirty reads are prevented; non-repeatable reads and
   * phantom reads can occur.
   * <p>This level only prohibits a transaction from reading a row
   * with uncommitted changes in it.
   *
   * @see java.sql.Connection#TRANSACTION_READ_COMMITTED
   */
  int ISOLATION_READ_COMMITTED = 2;  // same as java.sql.Connection.TRANSACTION_READ_COMMITTED;

  /**
   * Indicates that dirty reads and non-repeatable reads are prevented;
   * phantom reads can occur.
   * <p>This level prohibits a transaction from reading a row with uncommitted changes
   * in it, and it also prohibits the situation where one transaction reads a row,
   * a second transaction alters the row, and the first transaction re-reads the row,
   * getting different values the second time (a "non-repeatable read").
   *
   * @see java.sql.Connection#TRANSACTION_REPEATABLE_READ
   */
  int ISOLATION_REPEATABLE_READ = 4;  // same as java.sql.Connection.TRANSACTION_REPEATABLE_READ;

  /**
   * Indicates that dirty reads, non-repeatable reads and phantom reads
   * are prevented.
   * <p>This level includes the prohibitions in {@link #ISOLATION_REPEATABLE_READ}
   * and further prohibits the situation where one transaction reads all rows that
   * satisfy a {@code WHERE} condition, a second transaction inserts a row
   * that satisfies that {@code WHERE} condition, and the first transaction
   * re-reads for the same condition, retrieving the additional "phantom" row
   * in the second read.
   *
   * @see java.sql.Connection#TRANSACTION_SERIALIZABLE
   */
  int ISOLATION_SERIALIZABLE = 8;  // same as java.sql.Connection.TRANSACTION_SERIALIZABLE;

  /**
   * Use the default timeout of the underlying transaction system,
   * or none if timeouts are not supported.
   */
  int TIMEOUT_DEFAULT = -1;

  /**
   * Return the propagation behavior.
   * <p>Must return one of the {@code PROPAGATION_XXX} constants
   * defined on {@link TransactionDefinition this interface}.
   * <p>The default is {@link #PROPAGATION_REQUIRED}.
   *
   * @return the propagation behavior
   * @see #PROPAGATION_REQUIRED
   * @see TransactionSynchronizationManager#isSynchronizationActive()
   */
  default int getPropagationBehavior() {
    return PROPAGATION_REQUIRED;
  }

  /**
   * Return the isolation level.
   * <p>Must return one of the {@code ISOLATION_XXX} constants defined on
   * {@link TransactionDefinition this interface}. Those constants are designed
   * to match the values of the same constants on {@link java.sql.Connection}.
   * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
   * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
   * transactions. Consider switching the "validateExistingTransactions" flag to
   * "true" on your transaction manager if you'd like isolation level declarations
   * to get rejected when participating in an existing transaction with a different
   * isolation level.
   * <p>The default is {@link #ISOLATION_DEFAULT}. Note that a transaction manager
   * that does not support custom isolation levels will throw an exception when
   * given any other level than {@link #ISOLATION_DEFAULT}.
   *
   * @return the isolation level
   * @see #ISOLATION_DEFAULT
   * @see cn.taketoday.transaction.support.AbstractPlatformTransactionManager#setValidateExistingTransaction
   */
  default int getIsolationLevel() {
    return ISOLATION_DEFAULT;
  }

  /**
   * Return the transaction timeout.
   * <p>Must return a number of seconds, or {@link #TIMEOUT_DEFAULT}.
   * <p>Exclusively designed for use with {@link #PROPAGATION_REQUIRED} or
   * {@link #PROPAGATION_REQUIRES_NEW} since it only applies to newly started
   * transactions.
   * <p>Note that a transaction manager that does not support timeouts will throw
   * an exception when given any other timeout than {@link #TIMEOUT_DEFAULT}.
   * <p>The default is {@link #TIMEOUT_DEFAULT}.
   *
   * @return the transaction timeout
   */
  default int getTimeout() {
    return TIMEOUT_DEFAULT;
  }

  /**
   * Return whether to optimize as a read-only transaction.
   * <p>The read-only flag applies to any transaction context, whether backed
   * by an actual resource transaction ({@link #PROPAGATION_REQUIRED}/
   * {@link #PROPAGATION_REQUIRES_NEW}) or operating non-transactionally at
   * the resource level ({@link #PROPAGATION_SUPPORTS}). In the latter case,
   * the flag will only apply to managed resources within the application,
   * such as a Hibernate {@code Session}.
   * <p>This just serves as a hint for the actual transaction subsystem;
   * it will <i>not necessarily</i> cause failure of write access attempts.
   * A transaction manager which cannot interpret the read-only hint will
   * <i>not</i> throw an exception when asked for a read-only transaction.
   *
   * @return {@code true} if the transaction is to be optimized as read-only
   * ({@code false} by default)
   * @see TransactionSynchronization#beforeCommit(boolean)
   * @see SynchronizationInfo#isCurrentTransactionReadOnly() ()
   */
  default boolean isReadOnly() {
    return false;
  }

  /**
   * Return the name of this transaction. Can be {@code null}.
   * <p>This will be used as the transaction name to be shown in a
   * transaction monitor, if applicable (for example, WebLogic's).
   * <p>In case of declarative transactions, the exposed name will be
   * the {@code fully-qualified class name + "." + method name} (by default).
   *
   * @return the name of this transaction ({@code null} by default}
   * @see SynchronizationInfo#getCurrentTransactionName()
   */
  @Nullable
  default String getName() {
    return null;
  }

  // Static builder methods

  /**
   * Return an unmodifiable {@code TransactionDefinition} with defaults.
   * <p>For customization purposes, use the modifiable
   * {@link cn.taketoday.transaction.support.DefaultTransactionDefinition}
   * instead.
   *
   * @since 4.0
   */
  static TransactionDefinition withDefaults() {
    return StaticTransactionDefinition.INSTANCE;
  }

}
