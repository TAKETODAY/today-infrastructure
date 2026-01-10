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

import java.sql.SQLException;

import javax.sql.DataSource;

import infra.dao.DataAccessException;
import infra.transaction.HeuristicCompletionException;
import infra.transaction.IllegalTransactionStateException;
import infra.transaction.TransactionStatus;
import infra.transaction.TransactionSystemException;
import infra.transaction.UnexpectedRollbackException;

/**
 * Represents the result of a database execution and provides methods to create
 * named queries, execute queries, and manage transactions. This class acts as
 * a wrapper around a {@link JdbcConnection}, offering additional functionality
 * for query creation and transaction management.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JdbcConnection
 * @see QueryProducer
 * @since 4.0 2023/1/17 11:26
 */
public abstract class ExecutionResult implements QueryProducer {

  private final JdbcConnection connection;

  public ExecutionResult(JdbcConnection connection) {
    this.connection = connection;
  }

  /**
   * @throws DataAccessException Could not acquire a connection from data-source
   * @see DataSource#getConnection()
   */
  public NamedQuery createNamedQuery(String queryText) {
    return connection.createNamedQuery(queryText);
  }

  @Override
  public Query createQuery(String query, boolean returnGeneratedKeys) {
    return connection.createQuery(query, returnGeneratedKeys);
  }

  @Override
  public Query createQuery(String query) {
    return connection.createQuery(query);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  public NamedQuery createNamedQuery(String queryText, boolean returnGeneratedKeys) {
    return connection.createNamedQuery(queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  public NamedQuery createNamedQuery(String queryText, String... columnNames) {
    return connection.createNamedQuery(queryText, columnNames);
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
    connection.commit();
  }

  /**
   * Makes all changes made since the previous
   * commit/rollback permanent and releases any database locks
   * currently held by this <code>Connection</code> object.
   * This method should be
   * used only when auto-commit mode has been disabled.
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
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
   * @see TransactionStatus#setRollbackOnly
   */
  public void commit(boolean closeConnection) {
    connection.commit(closeConnection);
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
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
   */
  public void rollback() {
    connection.rollback();
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
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
   */
  public void rollback(boolean closeConnection) {
    connection.rollback(closeConnection);
  }

  protected DataAccessException translateException(String task, SQLException ex) {
    return getManager().translateException(task, null, ex);
  }

  public RepositoryManager getManager() {
    return connection.getManager();
  }

  public JdbcConnection getConnection() {
    return connection;
  }

}
