/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.jdbc;

import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import infra.dao.DataAccessException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.jdbc.datasource.DataSourceUtils;
import infra.transaction.HeuristicCompletionException;
import infra.transaction.IllegalTransactionStateException;
import infra.transaction.TransactionDefinition;
import infra.transaction.TransactionException;
import infra.transaction.TransactionStatus;
import infra.transaction.TransactionSystemException;
import infra.transaction.UnexpectedRollbackException;

/**
 * Manages a JDBC connection and optional transaction scope. Transactions started
 * by this instance are completed through the repository's transaction manager.
 * Borrowed connections retain their owner's transaction lifecycle.
 *
 * <p>Instances are not thread-safe. Closing is idempotent; a closed instance
 * cannot create new queries or start transactions.
 */
public final class JdbcConnection implements Closeable, QueryProducer {

  private final RepositoryManager manager;

  private final DataSource dataSource;

  private @Nullable Connection root;

  final boolean autoClose;

  private boolean rollbackOnClose = true;

  private boolean rollbackOnException = true;

  private final HashSet<Statement> statements = new HashSet<>();

  private final Map<Statement, String> statementSql = new HashMap<>();

  private @Nullable TransactionStatus transaction;

  private boolean closed;

  public JdbcConnection(RepositoryManager manager, DataSource dataSource, boolean autoClose) {
    this.manager = manager;
    this.autoClose = autoClose;
    this.dataSource = dataSource;
    createConnection();
  }

  public JdbcConnection(RepositoryManager manager, DataSource dataSource) {
    this.manager = manager;
    this.autoClose = false;
    this.dataSource = dataSource;
  }

  void onException(Throwable failure) {
    if (rollbackOnException && transaction != null && !transaction.isCompleted()) {
      try {
        completeTransaction(false);
      }
      catch (Throwable ex) {
        if (ex != failure) {
          failure.addSuppressed(ex);
        }
      }
    }
    if (autoClose) {
      try {
        close(failure);
      }
      catch (Throwable ex) {
        if (ex != failure) {
          failure.addSuppressed(ex);
        }
      }
    }
  }

  /**
   * @throws DataAccessException Could not acquire a connection from data-source
   * @see DataSource#getConnection()
   * @since 4.0
   */
  @Override
  public Query createQuery(String queryText) {
    return createQuery(queryText, false);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   * @since 4.0
   */
  @Override
  public Query createQuery(String queryText, boolean returnGeneratedKeys) {
    createConnectionIfNecessary();
    return new Query(this, queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   * @since 4.0
   */
  public Query createQuery(String queryText, String... columnNames) {
    createConnectionIfNecessary();
    return new Query(this, queryText, columnNames);
  }

  /**
   * @throws DataAccessException Could not acquire a connection from data-source
   * @see DataSource#getConnection()
   */
  @Override
  public NamedQuery createNamedQuery(String queryText) {
    return createNamedQuery(queryText, false);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  @Override
  public NamedQuery createNamedQuery(String queryText, boolean returnGeneratedKeys) {
    createConnectionIfNecessary();
    return new NamedQuery(this, queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  public NamedQuery createNamedQuery(String queryText, String... columnNames) {
    createConnectionIfNecessary();
    return new NamedQuery(this, queryText, columnNames);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  private void createConnectionIfNecessary() {
    assertOpen();
    try {
      if (root == null) {
        createConnection();
      }
      else if (root.isClosed()) {
        throw new InvalidDataAccessApiUsageException("JDBC connection is closed");
      }
    }
    catch (SQLException e) {
      throw translateException("Retrieves Connection status is closed", e);
    }
  }

  /**
   * use :p1, :p2, :p3 as the parameter name
   */
  public NamedQuery createNamedQueryWithParams(String queryText, Object... paramValues) {
    // due to #146, creating a query will not create a statement anymore
    // the PreparedStatement will only be created once the query needs to be executed
    // => there is no need to handle the query closing here anymore since there is nothing to close
    return createNamedQuery(queryText)
            .withParams(paramValues);
  }

  /**
   * Start a transaction scope before acquiring the JDBC connection, according to
   * the specified propagation behavior.
   * <p>Note that parameters like isolation level or timeout will only be applied
   * to new transactions, and thus be ignored when participating in active ones.
   * <p>Furthermore, not all transaction definition settings will be supported
   * by every transaction manager: A proper transaction manager implementation
   * should throw an exception when unsupported settings are encountered.
   * <p>An exception to the above rule is the read-only flag, which should be
   * ignored if no explicit read-only mode is supported. Essentially, the
   * read-only flag is just a hint for potential optimization.
   *
   * @return transaction status object representing the new or current transaction
   * @throws TransactionException in case of lookup, creation, or system errors
   * @throws IllegalTransactionStateException if the given transaction definition
   * cannot be executed (for example, if a currently active transaction is in
   * conflict with the specified propagation behavior)
   * @see TransactionDefinition#getPropagationBehavior
   * @see TransactionDefinition#getIsolationLevel
   * @see TransactionDefinition#getTimeout
   * @see TransactionDefinition#isReadOnly
   */
  public TransactionStatus beginTransaction() {
    return beginTransaction(TransactionDefinition.withDefaults());
  }

  /**
   * Start a transaction scope before acquiring the JDBC connection, according to
   * the specified propagation behavior.
   * <p>Note that parameters like isolation level or timeout will only be applied
   * to new transactions, and thus be ignored when participating in active ones.
   * <p>Furthermore, not all transaction definition settings will be supported
   * by every transaction manager: A proper transaction manager implementation
   * should throw an exception when unsupported settings are encountered.
   * <p>An exception to the above rule is the read-only flag, which should be
   * ignored if no explicit read-only mode is supported. Essentially, the
   * read-only flag is just a hint for potential optimization.
   *
   * @param definition the TransactionDefinition instance (can be {@code null} for defaults),
   * describing propagation behavior, isolation level, timeout etc.
   * @return transaction status object representing the new or current transaction
   * @throws TransactionException in case of lookup, creation, or system errors
   * @throws IllegalTransactionStateException if the given transaction definition
   * cannot be executed (for example, if a currently active transaction is in
   * conflict with the specified propagation behavior)
   * @see TransactionDefinition#getPropagationBehavior
   * @see TransactionDefinition#getIsolationLevel
   * @see TransactionDefinition#getTimeout
   * @see TransactionDefinition#isReadOnly
   */
  public TransactionStatus beginTransaction(@Nullable TransactionDefinition definition) {
    assertOpen();
    if (dataSource != manager.getDataSource()) {
      throw new InvalidDataAccessApiUsageException("Transaction DataSource must match the RepositoryManager DataSource");
    }
    if (transaction != null) {
      throw new InvalidDataAccessApiUsageException("Transaction require commit or rollback");
    }
    if (root != null) {
      throw new InvalidDataAccessApiUsageException("Start the transaction before acquiring a JDBC connection");
    }
    TransactionStatus status = manager.getTransactionManager().getTransaction(definition);
    this.transaction = status;
    try {
      createConnection();
      return status;
    }
    catch (RuntimeException | Error ex) {
      try {
        completeTransaction(false);
      }
      catch (Throwable rollbackFailure) {
        if (rollbackFailure != ex) {
          ex.addSuppressed(rollbackFailure);
        }
      }
      throw ex;
    }
  }

  @Nullable
  public TransactionStatus getTransaction() {
    return transaction;
  }

  /**
   * Undoes all changes made in the current transaction
   * and releases any database locks currently held
   * by this <code>Connection</code> object. This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws DataAccessException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   * @throws TransactionSystemException in case of rollback or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if no active transaction scope exists
   */
  public RepositoryManager rollback() {
    rollback(true);
    return manager;
  }

  /**
   * Undoes all changes made in the current transaction
   * and releases any database locks currently held
   * by this <code>Connection</code> object. This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws DataAccessException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   * @throws TransactionSystemException in case of rollback or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if no active transaction scope exists
   */
  public JdbcConnection rollback(boolean closeConnection) {
    finishTransaction(false, closeConnection);
    return this;
  }

  /**
   * Makes all changes made since the previous
   * commit/rollback permanent and releases any database locks
   * currently held by this <code>Connection</code> object.
   * This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws DataAccessException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * if this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   */
  public void commit() {
    commit(true);
  }

  /**
   * Makes all changes made since the previous
   * commit/rollback permanent and releases any database locks
   * currently held by this <code>Connection</code> object.
   * This method should be
   * used only when auto-commit mode has been disabled.
   *
   * <p>Requires an active transaction scope started by this instance. Transaction
   * completion is delegated to the configured transaction manager, including
   * participation in an enclosing transaction.
   *
   * @param closeConnection close connection
   * @throws DataAccessException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * if this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   * @throws UnexpectedRollbackException in case of an unexpected rollback
   * that the transaction coordinator initiated
   * @throws HeuristicCompletionException in case of a transaction failure
   * caused by a heuristic decision on the side of the transaction coordinator
   * @throws TransactionSystemException in case of commit or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if no active transaction scope exists
   * @see TransactionStatus#setRollbackOnly
   */
  public void commit(boolean closeConnection) {
    finishTransaction(true, closeConnection);
  }

  private void finishTransaction(boolean commit, boolean closeConnection) {
    try {
      completeTransaction(commit);
    }
    catch (RuntimeException | Error failure) {
      if (closeConnection) {
        try {
          close(failure);
        }
        catch (Throwable ex) {
          if (failure != ex) {
            failure.addSuppressed(ex);
          }
        }
      }
      throw failure;
    }
    if (closeConnection) {
      close();
    }
  }

  private void completeTransaction(boolean commit) {
    assertOpen();
    TransactionStatus status = transaction;
    if (status == null || status.isCompleted()) {
      throw new IllegalTransactionStateException("No active transaction scope");
    }
    try {
      if (commit) {
        manager.getTransactionManager().commit(status);
      }
      else {
        manager.getTransactionManager().rollback(status);
      }
      transaction = null;
    }
    finally {
      if (status.isCompleted()) {
        transaction = null;
      }
    }
  }

  private void assertOpen() {
    if (closed) {
      throw new InvalidDataAccessApiUsageException("JdbcConnection is closed");
    }
  }

  void registerStatement(Statement statement) {
    statements.add(statement);
  }

  void registerStatement(Statement statement, String sql) {
    registerStatement(statement);
    statementSql.put(statement, sql);
  }

  void removeStatement(Statement statement) {
    statements.remove(statement);
    statementSql.remove(statement);
  }

  // Closeable

  /**
   * Close all registered statements, roll back this instance's unfinished
   * transaction scope, and release the connection. Cleanup continues after
   * failures and aggregates subsequent exceptions as suppressed exceptions.
   *
   * @throws IllegalTransactionStateException if a transaction remains active and
   * automatic rollback on close is disabled
   */
  @Override
  public void close() {
    close(null);
  }

  private void close(@Nullable Throwable operationFailure) {
    if (closed) {
      return;
    }
    if (!rollbackOnClose && transaction != null && !transaction.isCompleted()) {
      throw new IllegalTransactionStateException("Complete the transaction before closing when rollbackOnClose is disabled");
    }
    Throwable failure = null;
    for (Statement statement : statements) {
      try {
        manager.closeResource(statement, statementSql.get(statement), operationFailure);
      }
      catch (Error error) {
        failure = aggregate(failure, error);
      }
    }
    statements.clear();
    statementSql.clear();
    TransactionStatus status = transaction;
    if (status != null && !status.isCompleted()) {
      try {
        completeTransaction(false);
      }
      catch (Throwable ex) {
        failure = aggregate(failure, ex);
      }
    }
    try {
      DataSourceUtils.doReleaseConnection(root, dataSource);
    }
    catch (Throwable ex) {
      if (ex instanceof Error) {
        failure = aggregate(failure, ex);
      }
      else {
        try {
          manager.reportResourceCloseFailure(new ResourceCloseFailure(
                  ResourceCloseFailure.ResourceType.CONNECTION, null, ex, operationFailure));
        }
        catch (Error error) {
          failure = aggregate(failure, error);
        }
      }
    }
    finally {
      if (status == null || status.isCompleted()) {
        transaction = null;
      }
      closed = true;
    }
    if (failure instanceof RuntimeException ex) {
      throw ex;
    }
    if (failure instanceof Error ex) {
      throw ex;
    }
  }

  private static Throwable aggregate(@Nullable Throwable failure, Throwable ex) {
    if (failure == null) {
      return ex;
    }
    if (failure != ex) {
      failure.addSuppressed(ex);
    }
    return failure;
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  void createConnection() {
    assertOpen();
    this.root = DataSourceUtils.getConnection(dataSource);
  }

  //
  public boolean isRollbackOnException() {
    return rollbackOnException;
  }

  public void setRollbackOnException(boolean rollbackOnException) {
    this.rollbackOnException = rollbackOnException;
  }

  public boolean isRollbackOnClose() {
    return rollbackOnClose;
  }

  public void setRollbackOnClose(boolean rollbackOnClose) {
    this.rollbackOnClose = rollbackOnClose;
  }

  public Connection getJdbcConnection() {
    Connection connection = root;
    if (connection == null) {
      createConnectionIfNecessary();
      connection = root;
      if (connection == null) {
        throw new IllegalStateException("JDBC connection has not been acquired");
      }
    }
    return connection;
  }

  public RepositoryManager getManager() {
    return manager;
  }

  private DataAccessException translateException(String task, SQLException ex) {
    return manager.translateException(task, null, ex);
  }

}
