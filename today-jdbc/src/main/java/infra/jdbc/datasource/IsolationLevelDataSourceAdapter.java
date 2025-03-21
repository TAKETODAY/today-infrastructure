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

package infra.jdbc.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import infra.lang.Assert;
import infra.lang.Nullable;
import infra.transaction.TransactionDefinition;
import infra.transaction.support.TransactionSynchronizationManager;

/**
 * An adapter for a target {@link javax.sql.DataSource}, applying the current
 * Framework transaction's isolation level (and potentially specified user credentials)
 * to every {@code getConnection} call. Also applies the read-only flag,
 * if specified.
 *
 * <p>Can be used to proxy a target JNDI DataSource that does not have the
 * desired isolation level (and user credentials) configured. Client code
 * can work with this DataSource as usual, not worrying about such settings.
 *
 * <p>Inherits the capability to apply specific user credentials from its superclass
 * {@link UserCredentialsDataSourceAdapter}; see the latter's javadoc for details
 * on that functionality (e.g. {@link #setCredentialsForCurrentThread}).
 *
 * <p><b>WARNING:</b> This adapter simply calls
 * {@link Connection#setTransactionIsolation} and/or
 * {@link Connection#setReadOnly} for every Connection obtained from it.
 * It does, however, <i>not</i> reset those settings; it rather expects the target
 * DataSource to perform such resetting as part of its connection pool handling.
 * <b>Make sure that the target DataSource properly cleans up such transaction state.</b>
 *
 * @author Juergen Hoeller
 * @see #setIsolationLevel
 * @see #setIsolationLevelName
 * @see #setUsername
 * @see #setPassword
 * @since 4.0
 */
public class IsolationLevelDataSourceAdapter extends UserCredentialsDataSourceAdapter {

  /**
   * Map of constant names to constant values for the isolation constants
   * defined in {@link TransactionDefinition}.
   */
  static final Map<String, Integer> constants = Map.of(
          "ISOLATION_DEFAULT", TransactionDefinition.ISOLATION_DEFAULT,
          "ISOLATION_READ_UNCOMMITTED", TransactionDefinition.ISOLATION_READ_UNCOMMITTED,
          "ISOLATION_READ_COMMITTED", TransactionDefinition.ISOLATION_READ_COMMITTED,
          "ISOLATION_REPEATABLE_READ", TransactionDefinition.ISOLATION_REPEATABLE_READ,
          "ISOLATION_SERIALIZABLE", TransactionDefinition.ISOLATION_SERIALIZABLE
  );

  @Nullable
  private Integer isolationLevel;

  /**
   * Set the default isolation level by the name of the corresponding constant
   * in {@link infra.transaction.TransactionDefinition}, e.g.
   * "ISOLATION_SERIALIZABLE".
   * <p>If not specified, the target DataSource's default will be used.
   * Note that a transaction-specific isolation value will always override
   * any isolation setting specified at the DataSource level.
   *
   * @param constantName name of the constant
   * @see infra.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
   * @see infra.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
   * @see infra.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
   * @see infra.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
   * @see #setIsolationLevel
   */
  public final void setIsolationLevelName(String constantName) throws IllegalArgumentException {
    Assert.hasText(constantName, "'constantName' must not be null or blank");
    Integer isolationLevel = constants.get(constantName);
    Assert.notNull(isolationLevel, "Only isolation constants allowed");
    setIsolationLevel(isolationLevel);
  }

  /**
   * Specify the default isolation level to use for Connection retrieval,
   * according to the JDBC {@link Connection} constants
   * (equivalent to the corresponding Framework
   * {@link infra.transaction.TransactionDefinition} constants).
   * <p>If not specified, the target DataSource's default will be used.
   * Note that a transaction-specific isolation value will always override
   * any isolation setting specified at the DataSource level.
   *
   * @see Connection#TRANSACTION_READ_UNCOMMITTED
   * @see Connection#TRANSACTION_READ_COMMITTED
   * @see Connection#TRANSACTION_REPEATABLE_READ
   * @see Connection#TRANSACTION_SERIALIZABLE
   * @see infra.transaction.TransactionDefinition#ISOLATION_READ_UNCOMMITTED
   * @see infra.transaction.TransactionDefinition#ISOLATION_READ_COMMITTED
   * @see infra.transaction.TransactionDefinition#ISOLATION_REPEATABLE_READ
   * @see infra.transaction.TransactionDefinition#ISOLATION_SERIALIZABLE
   * @see infra.transaction.TransactionDefinition#getIsolationLevel()
   * @see infra.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
   */
  public void setIsolationLevel(int isolationLevel) {
    Assert.isTrue(constants.containsValue(isolationLevel), "Only values of isolation constants allowed");
    this.isolationLevel = (isolationLevel != TransactionDefinition.ISOLATION_DEFAULT ? isolationLevel : null);
  }

  /**
   * Return the statically specified isolation level,
   * or {@code null} if none.
   */
  @Nullable
  protected Integer getIsolationLevel() {
    return this.isolationLevel;
  }

  /**
   * Applies the current isolation level value and read-only flag
   * to the returned Connection.
   *
   * @see #getCurrentIsolationLevel()
   * @see #getCurrentReadOnlyFlag()
   */
  @Override
  protected Connection doGetConnection(@Nullable String username, @Nullable String password) throws SQLException {
    Connection con = super.doGetConnection(username, password);
    Boolean readOnlyToUse = getCurrentReadOnlyFlag();
    if (readOnlyToUse != null) {
      con.setReadOnly(readOnlyToUse);
    }
    Integer isolationLevelToUse = getCurrentIsolationLevel();
    if (isolationLevelToUse != null) {
      con.setTransactionIsolation(isolationLevelToUse);
    }
    return con;
  }

  /**
   * Determine the current isolation level: either the transaction's
   * isolation level or a statically defined isolation level.
   *
   * @return the current isolation level, or {@code null} if none
   * @see infra.transaction.support.TransactionSynchronizationManager#getCurrentTransactionIsolationLevel()
   * @see #setIsolationLevel
   */
  @Nullable
  protected Integer getCurrentIsolationLevel() {
    Integer isolationLevelToUse = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
    if (isolationLevelToUse == null) {
      isolationLevelToUse = getIsolationLevel();
    }
    return isolationLevelToUse;
  }

  /**
   * Determine the current read-only flag: by default,
   * the transaction's read-only hint.
   *
   * @return whether there is a read-only hint for the current scope
   * @see infra.transaction.support.TransactionSynchronizationManager#isCurrentTransactionReadOnly()
   */
  @Nullable
  protected Boolean getCurrentReadOnlyFlag() {
    boolean txReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    return txReadOnly ? Boolean.TRUE : null;
  }

}
