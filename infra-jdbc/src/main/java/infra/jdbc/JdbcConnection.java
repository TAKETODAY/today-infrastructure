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

import static infra.util.ExceptionUtils.aggregate;

/**
 * Manages a JDBC connection and optional transaction scope. Transactions started
 * by this instance are completed through the repository's transaction manager.
 * Borrowed connections retain their owner's transaction lifecycle.
 *
 * <p>Start a transaction through {@link #beginTransaction()} before acquiring
 * the connection or creating queries. An unfinished transaction scope is rolled
 * back on {@link #close()}; successful completion requires an explicit commit.
 * Transaction propagation and physical connection ownership are handled by the
 * configured transaction manager and {@link DataSourceUtils}.
 *
 * <p>Instances are not thread-safe. Closing is idempotent; a closed instance
 * cannot create or execute queries or start transactions.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public final class JdbcConnection implements Closeable, QueryProducer {

  private final RepositoryManager manager;

  private final DataSource dataSource;

  final boolean autoClose;

  private final boolean borrowed;

  private final HashSet<Statement> statements = new HashSet<>();

  private boolean rollbackOnException = true;

  private @Nullable Connection root;

  private @Nullable TransactionStatus transaction;

  private boolean closed;

  /**
   * Create a wrapper and immediately acquire a JDBC connection.
   *
   * @param manager the repository providing query configuration and transaction management
   * @param dataSource the data source from which to acquire the connection
   * @param autoClose whether query operations automatically close this wrapper
   * when their resource lifecycle ends
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @see RepositoryManager#open(boolean)
   */
  public JdbcConnection(RepositoryManager manager, DataSource dataSource, boolean autoClose) {
    this.manager = manager;
    this.autoClose = autoClose;
    this.borrowed = false;
    this.dataSource = dataSource;
    createConnection();
  }

  /**
   * Create a wrapper with lazy connection acquisition and automatic closing disabled.
   * Uses the repository manager's data source.
   * Use this constructor to start a transaction before acquiring its connection.
   *
   * @param manager the repository providing query configuration and transaction management
   * @see #beginTransaction(TransactionDefinition)
   */
  public JdbcConnection(RepositoryManager manager) {
    this.manager = manager;
    this.autoClose = false;
    this.borrowed = false;
    this.dataSource = manager.getDataSource();
  }

  /**
   * Create a wrapper borrowing an existing connection. Only statements registered
   * with this wrapper are cleaned up on close; the caller retains responsibility
   * for the supplied connection and its transaction lifecycle.
   *
   * @param manager the repository providing query configuration
   * @param connection the existing connection to borrow
   */
  JdbcConnection(RepositoryManager manager, Connection connection) {
    this.manager = manager;
    this.dataSource = manager.getDataSource();
    this.autoClose = false;
    this.borrowed = true;
    this.root = connection;
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
   * Create a query using JDBC positional parameters, without requesting generated keys.
   *
   * @param queryText the SQL to execute, using {@code ?} placeholders
   * @return a query associated with this wrapper
   * @throws InvalidDataAccessApiUsageException if this wrapper or its connection is closed
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @see DataSource#getConnection()
   * @since 4.0
   */
  @Override
  public Query createQuery(String queryText) {
    return createQuery(queryText, false);
  }

  /**
   * Create a positional query, acquiring the connection if necessary.
   * The prepared statement is created lazily when the query needs it.
   *
   * @param queryText the SQL to execute, using {@code ?} placeholders
   * @param returnGeneratedKeys whether to request generated keys
   * @return a query associated with this wrapper
   * @throws InvalidDataAccessApiUsageException if this wrapper or its connection is closed
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @see DataSource#getConnection()
   * @since 4.0
   */
  @Override
  public Query createQuery(String queryText, boolean returnGeneratedKeys) {
    createConnectionIfNecessary();
    return new Query(this, queryText, returnGeneratedKeys);
  }

  /**
   * Create a positional query requesting generated keys for the specified columns.
   *
   * @param queryText the SQL to execute, using {@code ?} placeholders
   * @param columnNames the generated-key columns to request
   * @return a query associated with this wrapper
   * @throws InvalidDataAccessApiUsageException if this wrapper or its connection is closed
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @see DataSource#getConnection()
   * @since 4.0
   */
  public Query createQuery(String queryText, String... columnNames) {
    createConnectionIfNecessary();
    return new Query(this, queryText, columnNames);
  }

  /**
   * Create a named-parameter query without requesting generated keys.
   *
   * @param queryText the SQL to execute, using named placeholders such as {@code :id}
   * @return a named query associated with this wrapper
   * @throws InvalidDataAccessApiUsageException if this wrapper or its connection is closed
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @see DataSource#getConnection()
   */
  @Override
  public NamedQuery createNamedQuery(String queryText) {
    return createNamedQuery(queryText, false);
  }

  /**
   * Create a named-parameter query, acquiring the connection if necessary.
   * The prepared statement is created lazily when the query needs it.
   *
   * @param queryText the SQL to execute, using named placeholders such as {@code :id}
   * @param returnGeneratedKeys whether to request generated keys
   * @return a named query associated with this wrapper
   * @throws InvalidDataAccessApiUsageException if this wrapper or its connection is closed
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @see DataSource#getConnection()
   */
  @Override
  public NamedQuery createNamedQuery(String queryText, boolean returnGeneratedKeys) {
    createConnectionIfNecessary();
    return new NamedQuery(this, queryText, returnGeneratedKeys);
  }

  /**
   * Create a named-parameter query requesting generated keys for the specified columns.
   *
   * @param queryText the SQL to execute, using named placeholders such as {@code :id}
   * @param columnNames the generated-key columns to request
   * @return a named query associated with this wrapper
   * @throws InvalidDataAccessApiUsageException if this wrapper or its connection is closed
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
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
   * Create a named query and bind values to {@code :p1}, {@code :p2}, etc.
   * Generated keys are not requested.
   *
   * @param queryText the SQL containing sequentially named parameters
   * @param paramValues the values in parameter-name order, starting with {@code p1}
   * @return the configured named query
   * @throws InvalidDataAccessApiUsageException if this wrapper or its connection is closed
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @see NamedQuery#withParams(Object...)
   */
  public NamedQuery createNamedQueryWithParams(String queryText, Object... paramValues) {
    // due to #146, creating a query will not create a statement anymore
    // the PreparedStatement will only be created once the query needs to be executed
    // => there is no need to handle the query closing here anymore since there is nothing to close
    return createNamedQuery(queryText)
            .withParams(paramValues);
  }

  /**
   * Start a transaction scope using the default transaction definition, then
   * acquire its JDBC connection. May participate in an existing transaction.
   *
   * @return transaction status object representing the new or current transaction
   * @throws InvalidDataAccessApiUsageException if this wrapper is closed, a scope
   * already exists, the connection has been acquired, or the data sources differ
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @throws TransactionException in case of lookup, creation, or system errors
   * @throws IllegalTransactionStateException if the given transaction definition
   * cannot be executed (for example, if a currently active transaction is in
   * conflict with the specified propagation behavior)
   * @see TransactionDefinition#getPropagationBehavior
   * @see TransactionDefinition#getIsolationLevel
   * @see TransactionDefinition#getTimeout
   * @see TransactionDefinition#isReadOnly
   * @see #beginTransaction(TransactionDefinition)
   */
  public TransactionStatus beginTransaction() {
    return beginTransaction(TransactionDefinition.withDefaults());
  }

  /**
   * Start a transaction scope according to the specified propagation behavior,
   * then acquire its JDBC connection. Requires an open wrapper with no existing
   * scope or acquired connection, using the repository manager's data source.
   * If connection acquisition fails, rollback is attempted and any rollback
   * failure is suppressed on the acquisition failure.
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
   * @throws InvalidDataAccessApiUsageException if this wrapper is closed, a scope
   * already exists, the connection has been acquired, or the data sources differ
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
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

  /**
   * Return the transaction status retained by this wrapper.
   * Normally cleared after completion; may remain available after a failed
   * completion if the transaction manager has not marked it completed.
   *
   * @return the retained status, or {@code null} if no scope is retained
   */
  public @Nullable TransactionStatus getTransaction() {
    return transaction;
  }

  /**
   * Roll back this wrapper's transaction scope and close the wrapper.
   *
   * @return the associated repository manager
   * @throws InvalidDataAccessApiUsageException if this wrapper is closed
   * @throws TransactionSystemException in case of rollback or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if no active transaction scope exists
   * @see #rollback(boolean)
   */
  public RepositoryManager rollback() {
    rollback(true);
    return manager;
  }

  /**
   * Roll back an active transaction scope started through this wrapper.
   * Delegates to the transaction manager: a participating scope may mark the
   * enclosing transaction rollback-only, and a nested scope may roll back to
   * its savepoint, according to the manager's semantics.
   *
   * <p>If requested, closing is attempted even when rollback fails. Cleanup
   * failures are suppressed on the rollback failure.
   *
   * @param closeConnection whether to close this wrapper after completion
   * @return this wrapper
   * @throws InvalidDataAccessApiUsageException if this wrapper is closed
   * @throws TransactionSystemException in case of rollback or system errors
   * (typically caused by fundamental resource failures)
   * @throws IllegalTransactionStateException if no active transaction scope exists
   */
  public JdbcConnection rollback(boolean closeConnection) {
    finishTransaction(false, closeConnection);
    return this;
  }

  /**
   * Commit this wrapper's transaction scope and close the wrapper.
   *
   * @throws InvalidDataAccessApiUsageException if this wrapper is closed
   * @throws IllegalTransactionStateException if no active transaction scope exists
   * @throws TransactionException if transaction completion fails
   * @see #commit(boolean)
   */
  public void commit() {
    commit(true);
  }

  /**
   * Commit an active transaction scope started through this wrapper.
   * Delegates to the configured transaction manager; completing a participating
   * scope does not necessarily physically commit the enclosing transaction.
   * A rollback-only scope may be rolled back instead of committed.
   *
   * <p>If requested, closing is attempted even when commit fails. Cleanup
   * failures are suppressed on the commit failure.
   *
   * @param closeConnection whether to close this wrapper after completion
   * @throws InvalidDataAccessApiUsageException if this wrapper is closed
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

  void assertOpen() {
    if (closed) {
      throw new InvalidDataAccessApiUsageException("JdbcConnection is closed");
    }
  }

  void registerStatement(Statement statement) {
    assertOpen();
    statements.add(statement);
  }

  void removeStatement(Statement statement) {
    statements.remove(statement);
  }

  // Closeable

  /**
   * Close all registered statements, roll back this instance's unfinished
   * transaction scope, and release the connection. Resource close failures are
   * reported through the repository manager. Transaction rollback failures are
   * propagated, or suppressed on an existing operation failure.
   *
   * <p>Repeated calls have no effect. Releasing a connection respects external
   * transaction ownership and data source close policies; it does not necessarily
   * close the physical connection. Merely borrowing an external transaction's
   * connection does not cause that transaction to be rolled back.
   *
   * @throws TransactionException if rollback of an unfinished scope fails
   * @see RepositoryManager#getResourceCloseFailureListener()
   */
  @Override
  public void close() {
    close(null);
  }

  private void close(@Nullable Throwable operationFailure) {
    if (closed) {
      return;
    }
    Throwable failure = operationFailure;
    for (Statement statement : statements) {
      manager.closeResource(statement, null, failure);
    }
    statements.clear();
    TransactionStatus status = transaction;
    if (status != null && !status.isCompleted()) {
      try {
        completeTransaction(false);
      }
      catch (Throwable ex) {
        failure = aggregate(failure, ex);
      }
    }
    if (status == null || status.isCompleted()) {
      transaction = null;
    }
    closed = true;
    if (!borrowed) {
      manager.releaseConnection(root, dataSource, null, failure);
    }
    // The caller remains responsible for propagating an existing operation failure.
    if (operationFailure == null) {
      if (failure instanceof RuntimeException ex) {
        throw ex;
      }
      if (failure instanceof Error ex) {
        throw ex;
      }
    }
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  void createConnection() {
    assertOpen();
    this.root = DataSourceUtils.getConnection(dataSource);
  }

  /**
   * Return whether query failures reported to this wrapper trigger immediate
   * rollback of its active transaction scope. Defaults to {@code true}.
   *
   * @return whether immediate rollback on reported query failures is enabled
   */
  public boolean isRollbackOnException() {
    return rollbackOnException;
  }

  /**
   * Configure immediate rollback for query failures reported to this wrapper.
   * A successful rollback completes and clears the current transaction scope;
   * rollback failures are suppressed on the original query failure.
   *
   * <p>Disabling this option leaves transaction completion to the caller. It does
   * not disable rollback of an unfinished scope on {@link #close()}, or guarantee
   * that the database permits further work after a failed statement.
   * This option does not affect an external transaction merely borrowed by this
   * wrapper and is not a general exception policy for arbitrary application code.
   *
   * @param rollbackOnException whether to attempt immediate rollback
   */
  public void setRollbackOnException(boolean rollbackOnException) {
    this.rollbackOnException = rollbackOnException;
  }

  /**
   * Return the acquired JDBC connection, acquiring it lazily if necessary.
   * The returned connection may be a data source proxy rather than an unwrapped
   * driver connection. Acquiring it prevents a subsequent {@link #beginTransaction()}.
   *
   * <p>After this wrapper is closed, an already acquired connection is still
   * returned for inspection, but must not be used for further work through this
   * wrapper. Acquired connections have been released; borrowed connections remain
   * under the caller's ownership. A released connection may remain physically open
   * if owned by an external transaction.
   * If no connection was acquired before closing, this method rejects acquisition.
   *
   * @return the acquired connection
   * @throws CannotGetJdbcConnectionException if connection acquisition fails
   * @throws InvalidDataAccessApiUsageException if acquisition is required and
   * this wrapper is closed
   */
  public Connection getNativeConnection() {
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

  /**
   * Return the repository manager associated with this wrapper.
   *
   * @return the repository manager
   */
  public RepositoryManager getManager() {
    return manager;
  }

  private DataAccessException translateException(String task, SQLException ex) {
    return manager.translateException(task, null, ex);
  }

}
