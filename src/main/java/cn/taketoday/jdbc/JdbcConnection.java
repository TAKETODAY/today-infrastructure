package cn.taketoday.jdbc;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import cn.taketoday.context.conversion.ConversionService;
import cn.taketoday.context.conversion.support.DefaultConversionService;
import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.jdbc.support.ConnectionSource;
import cn.taketoday.jdbc.support.ConnectionSources;
import cn.taketoday.jdbc.utils.JdbcUtils;

/**
 * Represents a connection to the database with a transaction.
 */
public final class JdbcConnection implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(JdbcConnection.class);

  private final JdbcOperations session;
  private final ConnectionSource connectionSource;

  private Connection root;
  private Integer result = null;
  private int[] batchResult = null;
  private List<Object> keys;
  private boolean canGetKeys;

  final boolean autoClose;
  private Boolean originalAutoCommit;
  private boolean rollbackOnClose = true;
  private boolean rollbackOnException = true;

  private final HashSet<Statement> statements = new HashSet<>();

  JdbcConnection(JdbcOperations session, boolean autoClose) {
    this(session, session.getConnectionSource(), autoClose);
  }

  JdbcConnection(JdbcOperations session, ConnectionSource connectionSource, boolean autoClose) {
    this.session = session;
    this.autoClose = autoClose;
    this.connectionSource = connectionSource;
    createConnection();
  }

  JdbcConnection(JdbcOperations session, Connection connection, boolean autoClose) {
    this.session = session;
    this.root = connection;
    this.autoClose = autoClose;
    this.connectionSource = ConnectionSources.join(connection);
  }

  protected void onException() {
    if (isRollbackOnException()) {
      rollback(autoClose);
    }
  }

  public Query createQuery(String queryText) {
    boolean returnGeneratedKeys = session.isGeneratedKeys();
    return createQuery(queryText, returnGeneratedKeys);
  }

  public Query createQuery(String queryText, boolean returnGeneratedKeys) {
    try {
      if (root.isClosed()) {
        createConnection();
      }
    }
    catch (SQLException e) {
      throw new PersistenceException("Error creating connection", e);
    }

    return new Query(this, queryText, returnGeneratedKeys);
  }

  public Query createQuery(String queryText, String... columnNames) {
    try {
      if (root.isClosed()) {
        createConnection();
      }
    }
    catch (SQLException e) {
      throw new PersistenceException("Error creating connection", e);
    }

    return new Query(this, queryText, columnNames);
  }

  public Query createQueryWithParams(String queryText, Object... paramValues) {
    // due to #146, creating a query will not create a statement anymore;
    // the PreparedStatement will only be created once the query needs to be executed
    // => there is no need to handle the query closing here anymore since there is nothing to close
    return createQuery(queryText)
            .withParams(paramValues);
  }

  public JdbcOperations rollback() {
    rollback(true);
    return session;
  }

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

  public JdbcOperations commit() {
    commit(true);
    return session;
  }

  public JdbcConnection commit(boolean closeConnection) {
    try {
      root.commit();
    }
    catch (SQLException e) {
      throw new PersistenceException(e);
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
      throw new PersistenceException("It is required to call executeUpdate() method before calling getResult().");
    }
    return result;
  }

  void setResult(int result) {
    this.result = result;
  }

  public int[] getBatchResult() {
    if (batchResult == null) {
      throw new PersistenceException("It is required to call executeBatch() method before calling getBatchResult().");
    }
    return batchResult;
  }

  protected void setBatchResult(int[] value) {
    this.batchResult = value;
  }

  // --------------------------
  // keys

  protected void setKeys(ResultSet rs) throws SQLException {
    if (rs == null) {
      this.keys = null;
      return;
    }
    final ArrayList<Object> keys = new ArrayList<>();
    while (rs.next()) {
      keys.add(rs.getObject(1));
    }
    this.keys = keys;
  }

  public Object getKey() {
    assertCanGetKeys();
    if (!CollectionUtils.isEmpty(keys)) {
      return keys.get(0);
    }
    return null;
  }

  public <V> V getKey(Class<V> returnType) {
    return getKey(returnType, DefaultConversionService.getSharedInstance());
  }

  public <V> V getKey(Class<V> returnType, ConversionService conversionService) {
    Object key = getKey();
    try {
      return conversionService.convert(key, returnType);
    }
    catch (ConversionException e) {
      throw new PersistenceException(
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

  public <V> List<V> getKeys(Class<V> returnType) {
    return getKeys(returnType, DefaultConversionService.getSharedInstance());
  }

  public <V> List<V> getKeys(Class<V> returnType, ConversionService conversionService) {
    assertCanGetKeys();
    final List<Object> keys = this.keys;
    if (keys != null) {
      try {
        final ArrayList<V> convertedKeys = new ArrayList<>(keys.size());
        for (final Object key : keys) {
          convertedKeys.add(conversionService.convert(key, returnType));
        }
        return convertedKeys;
      }
      catch (ConversionException e) {
        throw new PersistenceException(
                "Exception occurred while converting value from database to type " + returnType, e);
      }
    }
    return null;
  }

  private void assertCanGetKeys() {
    if (!canGetKeys) {
      throw new PersistenceException(
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

  private void createConnection() {
    try {
      this.root = connectionSource.getConnection();
      this.originalAutoCommit = root.getAutoCommit();
    }
    catch (Exception ex) {
      throw new PersistenceException(
              "Could not acquire a connection from DataSource - " + ex.getMessage(), ex);
    }
  }

  private void closeConnection() {
    resetAutoCommitState();
    try {
      root.close();
    }
    catch (SQLException e) {
      log.warn("Could not close connection. message: {}", e);
    }
  }

  private void resetAutoCommitState() {
    // resets the AutoCommit state to make sure that the connection has been reset before reuse (if a connection pool is used)
    if (originalAutoCommit != null) {
      try {
        this.root.setAutoCommit(originalAutoCommit);
      }
      catch (SQLException e) {
        log.warn("Could not reset autocommit state for connection to {}.", originalAutoCommit, e);
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

  public JdbcOperations getSession() {
    return session;
  }

}
