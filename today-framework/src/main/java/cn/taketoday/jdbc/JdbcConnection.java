package cn.taketoday.jdbc;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.jdbc.support.ConnectionSource;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;

/**
 * Represents a connection to the database with a transaction.
 */
public final class JdbcConnection implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(JdbcConnection.class);

  private final JdbcOperations operations;
  private final ConnectionSource connectionSource;

  private Connection root;

  @Nullable
  private Integer result = null;

  private int[] batchResult = null;

  @Nullable
  private List<Object> keys;

  private boolean canGetKeys;

  final boolean autoClose;

  @Nullable
  private Boolean originalAutoCommit;
  private boolean rollbackOnClose = true;
  private boolean rollbackOnException = true;

  private final HashSet<Statement> statements = new HashSet<>();

  public JdbcConnection(JdbcOperations operations, boolean autoClose) {
    this(operations, operations.getConnectionSource(), autoClose);
  }

  public JdbcConnection(JdbcOperations operations, ConnectionSource connectionSource, boolean autoClose) {
    this.autoClose = autoClose;
    this.operations = operations;
    this.connectionSource = connectionSource;
    createConnection();
  }

  public JdbcConnection(JdbcOperations operations, Connection connection, boolean autoClose) {
    this.root = connection;
    this.autoClose = autoClose;
    this.operations = operations;
    this.connectionSource = ConnectionSource.join(connection);
  }

  void onException() {
    if (isRollbackOnException()) {
      rollback(autoClose);
    }
  }

  /**
   * @throws PersistenceException Could not acquire a connection from connection-source
   * @see ConnectionSource#getConnection()
   */
  public Query createQuery(String queryText) {
    boolean returnGeneratedKeys = operations.isGeneratedKeys();
    return createQuery(queryText, returnGeneratedKeys);
  }

  /**
   * @throws PersistenceException Could not acquire a connection from connection-source
   * @see ConnectionSource#getConnection()
   */
  public Query createQuery(String queryText, boolean returnGeneratedKeys) {
    createConnectionIfNecessary();
    return new Query(this, queryText, returnGeneratedKeys);
  }

  /**
   * @throws PersistenceException Could not acquire a connection from connection-source
   * @see ConnectionSource#getConnection()
   */
  public Query createQuery(String queryText, String... columnNames) {
    createConnectionIfNecessary();
    return new Query(this, queryText, columnNames);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   * @see ConnectionSource#getConnection()
   */
  private void createConnectionIfNecessary() {
    try {
      if (root.isClosed()) {
        createConnection();
      }
    }
    catch (SQLException e) {
      throw new PersistenceException("Database access error occurs", e);
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
   * Undoes all changes made in the current transaction
   * and releases any database locks currently held
   * by this <code>Connection</code> object. This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws PersistenceException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   */
  public JdbcOperations rollback() {
    rollback(true);
    return operations;
  }

  /**
   * Undoes all changes made in the current transaction
   * and releases any database locks currently held
   * by this <code>Connection</code> object. This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws PersistenceException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   */
  public JdbcConnection rollback(boolean closeConnection) {
    try {
      root.rollback();
    }
    catch (SQLException e) {
      log.warn("Could not roll back transaction. message: {}", e);
    }
    finally {
      if (closeConnection) {
        closeConnection();
      }
    }
    return this;
  }

  /**
   * Makes all changes made since the previous
   * commit/rollback permanent and releases any database locks
   * currently held by this <code>Connection</code> object.
   * This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @throws PersistenceException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * if this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   */
  public JdbcOperations commit() {
    commit(true);
    return operations;
  }

  /**
   * Makes all changes made since the previous
   * commit/rollback permanent and releases any database locks
   * currently held by this <code>Connection</code> object.
   * This method should be
   * used only when auto-commit mode has been disabled.
   *
   * @param closeConnection close connection
   * @throws PersistenceException if a database access error occurs,
   * this method is called while participating in a distributed transaction,
   * if this method is called on a closed connection or this
   * <code>Connection</code> object is in auto-commit mode
   */
  public JdbcConnection commit(boolean closeConnection) {
    try {
      root.commit();
    }
    catch (SQLException e) {
      throw new PersistenceException("Commit error", e);
    }
    finally {
      if (closeConnection) {
        closeConnection();
      }
    }
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

  protected void setBatchResult(int[] value) {
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
        final ArrayList<Object> keys = new ArrayList<>();
        while (rs.next()) {
          keys.add(rs.getObject(1));
        }
        this.keys = keys;
      }
      catch (SQLException e) {
        throw new GeneratedKeysException("Cannot get generated keys", e);
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
  public <V> V getKey(final Class<V> returnType) {
    return getKey(returnType, operations.getConversionService());
  }

  /**
   * @throws GeneratedKeysConversionException Generated Keys conversion failed
   * @throws IllegalArgumentException If conversionService is null
   */
  public <V> V getKey(final Class<V> returnType, final ConversionService conversionService) {
    Assert.notNull(conversionService, "conversionService must not be null");
    final Object key = getKey();
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
    final List<Object> keys = this.keys;
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
    return getKeys(returnType, operations.getConversionService());
  }

  /**
   * @throws GeneratedKeysConversionException cannot converting value from database
   * @throws IllegalArgumentException If conversionService is null
   */
  @Nullable
  public <V> List<V> getKeys(Class<V> returnType, ConversionService conversionService) {
    assertCanGetKeys();
    if (keys != null) {
      Assert.notNull(conversionService, "conversionService must not be null");
      try {
        final ArrayList<V> convertedKeys = new ArrayList<>(keys.size());
        for (final Object key : keys) {
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
      throw new PersistenceException(
              "encountered a problem while trying to determine whether the connection is closed.", e);
    }

    if (!connectionIsClosed) {
      for (Statement statement : statements) {
        try {
          JdbcUtils.close(statement);
        }
        catch (SQLException e) {
          log.warn("Could not close statement.", e);
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
  private void createConnection() {
    try {
      this.root = connectionSource.getConnection();
      // if a database access error occurs or this method is called on a closed connection
      this.originalAutoCommit = root.getAutoCommit();
    }
    catch (SQLException ex) {
      throw new CannotGetJdbcConnectionException(
              "Could not acquire a connection from connection-source: " + connectionSource, ex);
    }
  }

  private void closeConnection() {
    // resets the AutoCommit state to make sure that the connection
    // has been reset before reuse (if a connection pool is used)
    if (originalAutoCommit != null) {
      try {
        this.root.setAutoCommit(originalAutoCommit);
      }
      catch (SQLException e) {
        log.warn("Could not reset autocommit state for connection to {}.", originalAutoCommit, e);
      }
    }

    JdbcUtils.closeQuietly(root);
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

  public JdbcOperations getOperations() {
    return operations;
  }

}
