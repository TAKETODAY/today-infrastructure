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

package cn.taketoday.jdbc;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.transaction.HeuristicCompletionException;
import cn.taketoday.transaction.IllegalTransactionStateException;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.UnexpectedRollbackException;
import cn.taketoday.util.CollectionUtils;

/**
 * Represents a connection to the database with a transaction.
 */
public final class JdbcConnection implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(JdbcConnection.class);

  private final RepositoryManager manager;
  private final DataSource dataSource;

  private Connection root;

  @Nullable
  private Integer result = null;

  private int[] batchResult = null;

  @Nullable
  private List<Object> keys;

  private boolean canGetKeys;

  final boolean autoClose;

  private boolean rollbackOnClose = true;
  private boolean rollbackOnException = true;

  private final HashSet<Statement> statements = new HashSet<>();

  @Nullable
  private TransactionStatus transaction;

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

  void onException() {
    if (rollbackOnException) {
      rollback(autoClose);
    }
  }

  /**
   * @throws DataAccessException Could not acquire a connection from data-source
   * @see DataSource#getConnection()
   */
  public Query createQuery(String queryText) {
    boolean returnGeneratedKeys = manager.isGeneratedKeys();
    return createQuery(queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  public Query createQuery(String queryText, boolean returnGeneratedKeys) {
    createConnectionIfNecessary();
    return new Query(this, queryText, returnGeneratedKeys);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  public Query createQuery(String queryText, String... columnNames) {
    createConnectionIfNecessary();
    return new Query(this, queryText, columnNames);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see DataSource#getConnection()
   */
  private void createConnectionIfNecessary() {
    try {
      if (root == null || root.isClosed()) {
        createConnection();
      }
    }
    catch (SQLException e) {
      throw translateException("Retrieves Connection status is closed", e);
    }
  }

  /**
   * use :p1, :p2, :p3 as the parameter name
   */
  public Query createQueryWithParams(String queryText, Object... paramValues) {
    // due to #146, creating a query will not create a statement anymore
    // the PreparedStatement will only be created once the query needs to be executed
    // => there is no need to handle the query closing here anymore since there is nothing to close
    return createQuery(queryText)
            .withParams(paramValues);
  }

  /**
   * Return a currently active transaction or create a new one, according to
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
   * Return a currently active transaction or create a new one, according to
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
    if (transaction != null) {
      throw new InvalidDataAccessApiUsageException("Transaction require commit or rollback");
    }
    return this.transaction = manager.getTransactionManager().getTransaction(definition);
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
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
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
   * @throws IllegalTransactionStateException if the given transaction
   * is already completed (that is, committed or rolled back)
   */
  public JdbcConnection rollback(boolean closeConnection) {
    if (transaction != null) {
      manager.getTransactionManager().rollback(transaction);
    }
    if (closeConnection) {
      closeConnection();
    }

    this.transaction = null;
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
  public RepositoryManager commit() {
    commit(true);
    return manager;
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
  public JdbcConnection commit(boolean closeConnection) {
    if (transaction != null) {
      manager.getTransactionManager().commit(transaction);
    }
    if (closeConnection) {
      closeConnection();
    }
    this.transaction = null;
    return this;
  }

  public int getResult() {
    if (result == null) {
      throw new PersistenceException(
              "It is required to call executeUpdate() method before calling getResult().");
    }
    return result;
  }

  void setResult(int result) {
    this.result = result;
  }

  public int[] getBatchResult() {
    if (batchResult == null) {
      throw new PersistenceException(
              "It is required to call executeBatch() method before calling getBatchResult().");
    }
    return batchResult;
  }

  void setBatchResult(int[] value) {
    this.batchResult = value;
  }

  // ------------------------------------------------
  // -------------------- Keys ----------------------
  // ------------------------------------------------

  void setKeys(@Nullable ResultSet rs) {
    if (rs == null) {
      this.keys = null;
    }
    else {
      try {
        ArrayList<Object> keys = new ArrayList<>();
        while (rs.next()) {
          keys.add(rs.getObject(1));
        }
        this.keys = keys;
      }
      catch (SQLException e) {
        throw translateException("Getting generated keys.", e);
      }
    }

  }

  @Nullable
  public Object getKey() {
    assertCanGetKeys();
    List<Object> keys = this.keys;
    if (CollectionUtils.isNotEmpty(keys)) {
      return keys.get(0);
    }
    return null;
  }

  /**
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> V getKey(Class<V> returnType) {
    return getKey(returnType, manager.getConversionService());
  }

  /**
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> V getKey(Class<V> returnType, ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService is required");
    Object key = getKey();
    try {
      return conversionService.convert(key, returnType);
    }
    catch (ConversionException e) {
      throw new GeneratedKeysConversionException(
              "Exception occurred while converting value from database to type " + returnType.toString(), e);
    }
  }

  public Object[] getKeys() {
    assertCanGetKeys();
    List<Object> keys = this.keys;
    if (keys != null) {
      return keys.toArray();
    }
    return null;
  }

  /**
   * @throws GeneratedKeysConversionException cannot converting value from database
   * @throws IllegalArgumentException If conversionService is null
   */
  @Nullable
  public <V> List<V> getKeys(Class<V> returnType) {
    return getKeys(returnType, manager.getConversionService());
  }

  /**
   * @throws GeneratedKeysConversionException cannot converting value from database
   * @throws IllegalArgumentException If conversionService is null
   */
  @Nullable
  public <V> List<V> getKeys(Class<V> returnType, ConversionService conversionService) {
    assertCanGetKeys();
    if (keys != null) {
      Assert.notNull(conversionService, "conversionService is required");
      try {
        ArrayList<V> convertedKeys = new ArrayList<>(keys.size());
        for (Object key : keys) {
          convertedKeys.add(conversionService.convert(key, returnType));
        }
        return convertedKeys;
      }
      catch (ConversionException e) {
        throw new GeneratedKeysConversionException(
                "Exception occurred while converting value from database to type " + returnType, e);
      }
    }
    return null;
  }

  private void assertCanGetKeys() {
    if (!canGetKeys) {
      throw new GeneratedKeysException(
              "Keys where not fetched from database." +
                      " Please set the returnGeneratedKeys parameter " +
                      "in the createQuery() method to enable fetching of generated keys.");
    }

  }

  void setCanGetKeys(boolean canGetKeys) {
    this.canGetKeys = canGetKeys;
  }

  void registerStatement(Statement statement) {
    statements.add(statement);
  }

  void removeStatement(Statement statement) {
    statements.remove(statement);
  }

  // Closeable

  @Override
  public void close() {
    boolean connectionIsClosed;
    try {
      connectionIsClosed = root.isClosed();
    }
    catch (SQLException e) {
      throw translateException("trying to determine whether the connection is closed.", e);
    }

    if (!connectionIsClosed) {
      for (Statement statement : statements) {
        try {
          statement.close();
        }
        catch (SQLException ex) {
          if (manager.isCatchResourceCloseErrors()) {
            throw translateException("Trying to close statement", ex);
          }
          else {
            log.warn("Could not close statement. statement: {}", statement, ex);
          }
        }
      }
      statements.clear();

      boolean rollback = rollbackOnClose;
      if (rollback) {
        try {
          rollback = !root.getAutoCommit();
        }
        catch (SQLException e) {
          log.warn("Could not determine connection auto commit mode.", e);
        }
      }

      // if in transaction, rollback, otherwise just close
      if (rollback) {
        rollback(true);
      }
      else {
        closeConnection();
      }
    }
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  void createConnection() {
    this.root = DataSourceUtils.getConnection(dataSource);
  }

  private void closeConnection() {
    if (DataSourceUtils.isConnectionTransactional(root, dataSource)) {
      DataSourceUtils.releaseConnection(root, dataSource);
    }
    else {
      try {
        root.close();
      }
      catch (SQLException ex) {
        if (manager.isCatchResourceCloseErrors()) {
          throw translateException("Trying to close connection", ex);
        }
        else {
          log.warn("Could not close connection: {}", root, ex);
        }
      }
    }
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
    return root;
  }

  public RepositoryManager getManager() {
    return manager;
  }

  private DataAccessException translateException(String task, SQLException ex) {
    return manager.translateException(task, null, ex);
  }

}
