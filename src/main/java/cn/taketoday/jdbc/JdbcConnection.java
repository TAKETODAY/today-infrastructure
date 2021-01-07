package cn.taketoday.jdbc;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.context.exception.ConversionException;
import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.CollectionUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.jdbc.connectionsources.ConnectionSource;
import cn.taketoday.jdbc.utils.JdbcUtils;

/**
 * Represents a connection to the database with a transaction.
 */
public class JdbcConnection implements AutoCloseable, Closeable {
  private static final Logger log = LoggerFactory.getLogger(JdbcConnection.class);

  private final DefaultSession sql2o;
  private final ConnectionSource connectionSource;

  private Connection jdbcConnection;
  private Integer result = null;
  private int[] batchResult = null;
  private List<Object> keys;
  private boolean canGetKeys;

  final boolean autoClose;
  private Boolean originalAutoCommit;
  private boolean rollbackOnClose = true;
  private boolean rollbackOnException = true;


  JdbcConnection(DefaultSession sql2o, boolean autoClose) {
    this(sql2o, null, autoClose);
  }

  JdbcConnection(DefaultSession sql2o, ConnectionSource connectionSource, boolean autoClose) {
    this.connectionSource = connectionSource != null ? connectionSource : sql2o.getConnectionSource();
    this.autoClose = autoClose;
    this.sql2o = sql2o;
    createConnection();
  }

  void onException() {
    if (isRollbackOnException()) {
      rollback(this.autoClose);
    }
  }

  public Query createQuery(String queryText) {
    boolean returnGeneratedKeys = this.sql2o.isGeneratedKeys();
    return createQuery(queryText, returnGeneratedKeys);
  }

  public Query createQuery(String queryText, boolean returnGeneratedKeys) {

    try {
      if (jdbcConnection.isClosed()) {
        createConnection();
      }
    }
    catch (SQLException e) {
      throw new Sql2oException("Error creating connection", e);
    }

    return new Query(this, queryText, returnGeneratedKeys);
  }

  public Query createQuery(String queryText, String... columnNames) {
    try {
      if (jdbcConnection.isClosed()) {
        createConnection();
      }
    }
    catch (SQLException e) {
      throw new Sql2oException("Error creating connection", e);
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

  public DefaultSession rollback() {
    return this.rollback(true).sql2o;
  }

  public JdbcConnection rollback(boolean closeConnection) {
    try {
      jdbcConnection.rollback();
    }
    catch (SQLException e) {
      log.warn("Could not roll back transaction. message: {}", e);
    }
    finally {
      if (closeConnection) {
        this.closeConnection();
      }
    }
    return this;
  }

  public DefaultSession commit() {
    return this.commit(true).sql2o;
  }

  public JdbcConnection commit(boolean closeConnection) {
    try {
      jdbcConnection.commit();
    }
    catch (SQLException e) {
      throw new Sql2oException(e);
    }
    finally {
      if (closeConnection) {
        this.closeConnection();
      }
    }
    return this;
  }

  public int getResult() {
    if (this.result == null) {
      throw new Sql2oException("It is required to call executeUpdate() method before calling getResult().");
    }
    return this.result;
  }

  void setResult(int result) {
    this.result = result;
  }

  public int[] getBatchResult() {
    if (this.batchResult == null) {
      throw new Sql2oException("It is required to call executeBatch() method before calling getBatchResult().");
    }
    return this.batchResult;
  }

  void setBatchResult(int[] value) {
    this.batchResult = value;
  }

  void setKeys(ResultSet rs) throws SQLException {
    if (rs == null) {
      this.keys = null;
      return;
    }
    this.keys = new ArrayList<>();
    while (rs.next()) {
      this.keys.add(rs.getObject(1));
    }
  }

  public Object getKey() {
    if (!this.canGetKeys) {
      throw new Sql2oException(
              "Keys were not fetched from database. Please set the returnGeneratedKeys parameter in the createQuery() method to enable fetching of generated keys.");
    }
    if (!CollectionUtils.isEmpty(keys)) {
      return keys.get(0);
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public <V> V getKey(Class<?> returnType) {
    Object key = getKey();
    try {
      return (V) ConvertUtils.convert(returnType, key);
    }
    catch (ConversionException e) {
      throw new Sql2oException("Exception occurred while converting value from database to type " + returnType.toString(), e);
    }
  }

  public Object[] getKeys() {
    if (!this.canGetKeys) {
      throw new Sql2oException(
              "Keys where not fetched from database. Please set the returnGeneratedKeys parameter in the createQuery() method to enable fetching of generated keys.");
    }
    if (this.keys != null) {
      return this.keys.toArray();
    }
    return null;
  }

  // need to change Convert
  public <V> List<V> getKeys(Class<V> returnType) {
    if (!this.canGetKeys) {
      throw new Sql2oException(
              "Keys where not fetched from database. Please set the returnGeneratedKeys parameter in the createQuery() method to enable fetching of generated keys.");
    }

    if (this.keys != null) {
      try {
        List<V> convertedKeys = new ArrayList<>(this.keys.size());
        for (Object key : this.keys) {
          convertedKeys.add(ConvertUtils.convert(returnType, key));
        }
        return convertedKeys;
      }
      catch (ConversionException e) {
        throw new Sql2oException("Exception occurred while converting value from database to type " + returnType.toString(), e);
      }
    }
    return null;
  }

  void setCanGetKeys(boolean canGetKeys) {
    this.canGetKeys = canGetKeys;
  }

  private final Set<Statement> statements = new HashSet<>();

  void registerStatement(Statement statement) {
    statements.add(statement);
  }

  void removeStatement(Statement statement) {
    statements.remove(statement);
  }

  @Override
  public void close() {
    boolean connectionIsClosed;
    try {
      connectionIsClosed = jdbcConnection.isClosed();
    }
    catch (SQLException e) {
      throw new Sql2oException("Sql2o encountered a problem while trying to determine whether the connection is closed.", e);
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
          rollback = !jdbcConnection.getAutoCommit();
        }
        catch (SQLException e) {
          log.warn("Could not determine connection auto commit mode.", e);
        }
      }

      // if in transaction, rollback, otherwise just close
      if (rollback) {
        this.rollback(true);
      }
      else {
        this.closeConnection();
      }
    }
  }

  private void createConnection() {
    try {
      this.jdbcConnection = connectionSource.getConnection();
      this.originalAutoCommit = jdbcConnection.getAutoCommit();
    }
    catch (Exception ex) {
      throw new Sql2oException("Could not acquire a connection from DataSource - " + ex.getMessage(), ex);
    }
  }

  private void closeConnection() {
    resetAutoCommitState();
    try {
      jdbcConnection.close();
    }
    catch (SQLException e) {
      log.warn("Could not close connection. message: {}", e);
    }
  }

  private void resetAutoCommitState() {
    // resets the AutoCommit state to make sure that the connection has been reset before reuse (if a connection pool is used)
    if (originalAutoCommit != null) {
      try {
        this.jdbcConnection.setAutoCommit(originalAutoCommit);
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

  public JdbcConnection setRollbackOnException(boolean rollbackOnException) {
    this.rollbackOnException = rollbackOnException;
    return this;
  }

  public boolean isRollbackOnClose() {
    return rollbackOnClose;
  }

  public JdbcConnection setRollbackOnClose(boolean rollbackOnClose) {
    this.rollbackOnClose = rollbackOnClose;
    return this;
  }

  public Connection getJdbcConnection() {
    return jdbcConnection;
  }

  public DefaultSession getSql2o() {
    return sql2o;
  }

}
