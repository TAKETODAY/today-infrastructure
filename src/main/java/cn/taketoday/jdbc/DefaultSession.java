/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.jdbc.connectionsources.ConnectionSource;
import cn.taketoday.jdbc.connectionsources.ConnectionSources;
import cn.taketoday.jdbc.connectionsources.DataSourceConnectionSource;
import cn.taketoday.jdbc.conversion.ClobToStringConverter;
import cn.taketoday.jdbc.conversion.OffsetTimeToSQLTimeConverter;
import cn.taketoday.jdbc.conversion.TimeToJodaLocalTimeConverter;
import cn.taketoday.jdbc.parsing.SqlParameterParsingStrategy;
import cn.taketoday.jdbc.parsing.impl.DefaultSqlParameterParsingStrategy;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.jdbc.utils.FeatureDetector;

/**
 * DefaultSession is the main class for the today-jdbc library.
 * <p>
 * An <code>Sql2o</code> instance represents a way of connecting to one specific
 * database. To create a new instance, one need to specify either jdbc-url,
 * username and password for the database or a data source.
 * <p>
 * Internally the Sql2o instance uses a data source to create jdbc connections
 * to the database. If url, username and password was specified in the
 * constructor, a simple data source is created, which works as a simple wrapper
 * around the jdbc driver.
 * <p>
 * Some jdbc implementations have quirks, therefore it may be necessary to use a
 * constructor with the quirks parameter. When quirks are specified, Sql2o will
 * use workarounds to avoid these quirks.
 *
 * @author Lars Aaberg
 * @author TODAY
 */
public class DefaultSession {

  private TypeHandlerRegistry typeHandlerRegistry = TypeHandlerRegistry.getSharedInstance();

  private boolean defaultCaseSensitive;
  private boolean generatedKeys = true;
  private ConnectionSource connectionSource;
  private Map<String, String> defaultColumnMappings;
  private SqlParameterParsingStrategy parsingStrategy = new DefaultSqlParameterParsingStrategy();

  static {
    final ClobToStringConverter stringConverter = new ClobToStringConverter();

    ConvertUtils.addConverter(stringConverter);

    if (FeatureDetector.isJodaTimeAvailable()) {
      final TimeToJodaLocalTimeConverter jodaLocalTimeConverter = new TimeToJodaLocalTimeConverter();
      ConvertUtils.addConverter(jodaLocalTimeConverter);
    }

    ConvertUtils.addConverter(new OffsetTimeToSQLTimeConverter());
  }

  public DefaultSession(String jndiLookup) {
    this(JndiDataSource.getJndiDatasource(jndiLookup));
  }

  /**
   * Creates a new instance of the Sql2o class. Internally this constructor will
   * create a {@link GenericDatasource}, and call the
   * {@link DefaultSession#DefaultSession(DataSource)} constructor which takes a
   * DataSource as parameter.
   *
   * @param url
   *         JDBC database url
   * @param user
   *         database username
   * @param pass
   *         database password
   */
  public DefaultSession(String url, String user, String pass) {
    this(new GenericDatasource(url, user, pass));
  }

  /**
   * Creates a new instance of the Sql2o class, which uses the given DataSource to
   * acquire connections to the database.
   *
   * @param dataSource
   *         The DataSource Sql2o uses to acquire connections to the database.
   */
  public DefaultSession(DataSource dataSource) {
    this.connectionSource = DataSourceConnectionSource.of(dataSource);
    this.defaultColumnMappings = new HashMap<>();
  }

  public DefaultSession(DataSource dataSource, boolean generatedKeys) {
    this.generatedKeys = generatedKeys;
    this.defaultColumnMappings = new HashMap<>();
    this.connectionSource = DataSourceConnectionSource.of(dataSource);
  }

  /**
   * Gets the DataSource that Sql2o uses internally to acquire database
   * connections.
   *
   * @return The DataSource instance
   *
   * @deprecated use {@link #getConnectionSource()} as more general connection
   * provider
   */
  public DataSource getDataSource() {
    if (connectionSource instanceof DataSourceConnectionSource)
      return ((DataSourceConnectionSource) connectionSource).getDataSource();
    else
      return null;
  }

  /**
   * Gets the {@link ConnectionSource} that Sql2o uses internally to acquire
   * database connections.
   *
   * @return The ConnectionSource instance
   */
  public ConnectionSource getConnectionSource() {
    return connectionSource;
  }

  /**
   * Sets the {@link ConnectionSource} that Sql2o uses internally to acquire
   * database connections.
   *
   * @param connectionSource
   *         the ConnectionSource instance to use
   */
  public void setConnectionSource(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  /**
   * Gets the default column mappings Map. column mappings added to this Map are
   * always available when Sql2o attempts to map between result sets and object
   * instances.
   *
   * @return The {@link Map<String,String>} instance, which Sql2o internally uses
   * to map column names with property names.
   */
  public Map<String, String> getDefaultColumnMappings() {
    return defaultColumnMappings;
  }

  /**
   * Sets the default column mappings Map.
   *
   * @param defaultColumnMappings
   *         A {@link Map} instance Sql2o uses internally to map between column
   *         names and property names.
   */
  public void setDefaultColumnMappings(Map<String, String> defaultColumnMappings) {
    this.defaultColumnMappings = defaultColumnMappings;
  }

  /**
   * Gets value indicating if this instance of Sql2o is case sensitive when
   * mapping between columns names and property names.
   */
  public boolean isDefaultCaseSensitive() {
    return defaultCaseSensitive;
  }

  /**
   * Sets a value indicating if this instance of Sql2o is case sensitive when
   * mapping between columns names and property names. This should almost always
   * be false, because most relational databases are not case sensitive.
   */
  public void setDefaultCaseSensitive(boolean defaultCaseSensitive) {
    this.defaultCaseSensitive = defaultCaseSensitive;
  }

  public void setGeneratedKeys(boolean generatedKeys) {
    this.generatedKeys = generatedKeys;
  }

  /**
   * @return true if queries should return generated keys by default, false
   * otherwise
   */
  public boolean isGeneratedKeys() {
    return generatedKeys;
  }

  public void setParsingStrategy(SqlParameterParsingStrategy parsingStrategy) {
    this.parsingStrategy = parsingStrategy;
  }

  public SqlParameterParsingStrategy getParsingStrategy() {
    return parsingStrategy;
  }

  //

  /**
   * Creates a {@link Query}
   *
   * @param query
   *         the sql query string
   * @param returnGeneratedKeys
   *         boolean value indicating if the database should return any
   *         generated keys.
   *
   * @return the {@link Query} instance
   *
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks <code>
   * try (Connection con = sql2o.open()) {
   * return sql2o.createQuery(query, name, returnGeneratedKeys).executeAndFetch(Pojo.class);
   * }
   * </code>
   */
  public Query createQuery(String query, boolean returnGeneratedKeys) {
    return open(true).createQuery(query, returnGeneratedKeys);
  }

  /**
   * Creates a {@link Query}
   *
   * @param query
   *         the sql query string
   *
   * @return the {@link Query} instance
   *
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>
   *     try (Connection con = sql2o.open()) {
   *         return sql2o.createQuery(query, name).executeAndFetch(Pojo.class);
   *     }
   *  </pre>
   */
  public Query createQuery(String query) {
    return open(true).createQuery(query);
  }

  /**
   * Opens a connection to the database
   *
   * @return instance of the {@link JdbcConnection} class.
   */
  public JdbcConnection open() {
    return open(false);
  }

  /**
   * Opens a connection to the database
   *
   * @return instance of the {@link JdbcConnection} class.
   */
  public JdbcConnection open(boolean autoClose) {
    return new JdbcConnection(this, connectionSource, autoClose);
  }

  /**
   * Opens a connection to the database
   *
   * @param connection
   *         the {@link Connection}
   *
   * @return instance of the {@link JdbcConnection} class.
   */
  public JdbcConnection open(Connection connection) {
    return new JdbcConnection(this, connection, false);
  }

  /**
   * Opens a connection to the database
   *
   * @param connectionSource
   *         the {@link ConnectionSource} implementation substitution, that
   *         will be used instead of one from {@link DefaultSession} instance.
   *
   * @return instance of the {@link JdbcConnection} class.
   */
  public JdbcConnection open(ConnectionSource connectionSource) {
    return new JdbcConnection(this, connectionSource, false);
  }

  /**
   * Invokes the run method on the {@link StatementRunnableWithResult}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   */
  public <V> V withConnection(StatementRunnableWithResult<V> runnable, Object argument) {
    try (JdbcConnection connection = open()) {
      return runnable.run(connection, argument);
    }
    catch (Throwable t) {
      throw new PersistenceException("An error occurred while executing StatementRunnable", t);
    }
  }

  /**
   * Invokes the run method on the {@link StatementRunnableWithResult}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   */
  public <V> V withConnection(StatementRunnableWithResult<V> runnable) {
    return withConnection(runnable, null);
  }

  /**
   * Invokes the run method on the {@link StatementRunnableWithResult}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   */
  public void withConnection(StatementRunnable runnable) {
    withConnection(runnable, null);
  }

  /**
   * Invokes the run method on the {@link StatementRunnableWithResult}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   */
  public void withConnection(StatementRunnable runnable, Object argument) {
    try (JdbcConnection connection = open()) {
      runnable.run(connection, argument);
    }
    catch (Throwable t) {
      throw new PersistenceException("An error occurred while executing StatementRunnable", t);
    }
  }

  /**
   * Begins a transaction with the given isolation level. Every statement executed
   * on the return {@link JdbcConnection} instance, will be executed in the
   * transaction. It is very important to always call either the
   * {@link JdbcConnection#commit()} method or the
   * {@link JdbcConnection#rollback()} method to close the transaction. Use
   * proper try-catch logic.
   *
   * @param isolationLevel
   *         the isolation level of the transaction
   *
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   */
  public JdbcConnection beginTransaction(int isolationLevel) {
    return beginTransaction(getConnectionSource(), isolationLevel);
  }

  /**
   * Begins a transaction with the given isolation level. Every statement executed
   * on the return {@link JdbcConnection} instance, will be executed in the
   * transaction. It is very important to always call either the
   * {@link JdbcConnection#commit()} method or the
   * {@link JdbcConnection#rollback()} method to close the transaction. Use
   * proper try-catch logic.
   *
   * @param connectionSource
   *         the {@link ConnectionSource} implementation substitution, that
   *         will be used instead of one from {@link DefaultSession} instance.
   * @param isolationLevel
   *         the isolation level of the transaction
   *
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   */
  public JdbcConnection beginTransaction(ConnectionSource connectionSource, int isolationLevel) {
    JdbcConnection connection = new JdbcConnection(this, connectionSource, false);
    boolean success = false;
    try {
      final Connection root = connection.getJdbcConnection();
      root.setAutoCommit(false);
      root.setTransactionIsolation(isolationLevel);
      success = true;
    }
    catch (SQLException e) {
      throw new PersistenceException("Could not start the transaction - " + e.getMessage(), e);
    }
    finally {
      if (!success) {
        connection.close();
      }
    }

    return connection;
  }

  /**
   * Begins a transaction with isolation level
   * {@link Connection#TRANSACTION_READ_COMMITTED}. Every statement
   * executed on the return {@link JdbcConnection} instance, will be executed in the
   * transaction. It is very important to always call either the
   * {@link JdbcConnection#commit()} method or the
   * {@link JdbcConnection#rollback()} method to close the transaction. Use
   * proper try-catch logic.
   *
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   */
  public JdbcConnection beginTransaction() {
    return beginTransaction(Connection.TRANSACTION_READ_COMMITTED);
  }

  /**
   * Begins a transaction with isolation level
   * {@link Connection#TRANSACTION_READ_COMMITTED}. Every statement
   * executed on the return {@link JdbcConnection} instance, will be executed in the
   * transaction. It is very important to always call either the
   * {@link JdbcConnection#commit()} method or the
   * {@link JdbcConnection#rollback()} method to close the transaction. Use
   * proper try-catch logic.
   *
   * @param connectionSource
   *         the {@link ConnectionSource} implementation substitution, that
   *         will be used instead of one from {@link DefaultSession} instance.
   *
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   */
  public JdbcConnection beginTransaction(ConnectionSource connectionSource) {
    return beginTransaction(connectionSource, Connection.TRANSACTION_READ_COMMITTED);
  }

  /**
   * Begins a transaction with isolation level
   * {@link Connection#TRANSACTION_READ_COMMITTED}. Every statement
   * executed on the return {@link JdbcConnection} instance, will be executed in the
   * transaction. It is very important to always call either the
   * {@link JdbcConnection#commit()} method or the
   * {@link JdbcConnection#rollback()} method to close the transaction. Use
   * proper try-catch logic.
   *
   * @param connection
   *         the {@link Connection}
   *
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   */
  public JdbcConnection beginTransaction(Connection connection) {
    return beginTransaction(ConnectionSources.join(connection), Connection.TRANSACTION_READ_COMMITTED);
  }

  /**
   * Calls the {@link StatementRunnable#run(JdbcConnection, Object)} method on the
   * {@link StatementRunnable} parameter. All statements run on the
   * {@link JdbcConnection} instance in the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method will be executed
   * in a transaction. The transaction will automatically be committed if the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method finishes without
   * throwing an exception. If an exception is thrown within the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method, the transaction
   * will automatically be rolled back.
   *
   * The isolation level of the transaction will be set to
   * {@link Connection#TRANSACTION_READ_COMMITTED}
   *
   * @param runnable
   *         The {@link StatementRunnable} instance.
   */
  public void runInTransaction(StatementRunnable runnable) {
    runInTransaction(runnable, null);
  }

  /**
   * Calls the {@link StatementRunnable#run(JdbcConnection, Object)} method on the
   * {@link StatementRunnable} parameter. All statements run on the
   * {@link JdbcConnection} instance in the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method will be executed
   * in a transaction. The transaction will automatically be committed if the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method finishes without
   * throwing an exception. If an exception is thrown within the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method, the transaction
   * will automatically be rolled back.
   *
   * The isolation level of the transaction will be set to
   * {@link Connection#TRANSACTION_READ_COMMITTED}
   *
   * @param runnable
   *         The {@link StatementRunnable} instance.
   * @param argument
   *         An argument which will be forwarded to the
   *         {@link StatementRunnable#run(JdbcConnection, Object) run} method
   */
  public void runInTransaction(StatementRunnable runnable, Object argument) {
    runInTransaction(runnable, argument, Connection.TRANSACTION_READ_COMMITTED);
  }

  /**
   * Calls the {@link StatementRunnable#run(JdbcConnection, Object)} method on the
   * {@link StatementRunnable} parameter. All statements run on the
   * {@link JdbcConnection} instance in the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method will be executed
   * in a transaction. The transaction will automatically be committed if the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method finishes without
   * throwing an exception. If an exception is thrown within the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method, the transaction
   * will automatically be rolled back.
   *
   * @param runnable
   *         The {@link StatementRunnable} instance.
   * @param argument
   *         An argument which will be forwarded to the
   *         {@link StatementRunnable#run(JdbcConnection, Object) run} method
   * @param isolationLevel
   *         The isolation level of the transaction
   */
  public void runInTransaction(StatementRunnable runnable, Object argument, int isolationLevel) {

    JdbcConnection connection = beginTransaction(isolationLevel);
    connection.setRollbackOnException(false);

    try {
      runnable.run(connection, argument);
    }
    catch (Throwable throwable) {
      connection.rollback();
      throw new PersistenceException("An error occurred while executing StatementRunnable. Transaction is rolled back.", throwable);
    }
    connection.commit();
  }

  public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult) {
    return runInTransaction(runnableWithResult, null);
  }

  public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult, Object argument) {
    return runInTransaction(runnableWithResult, argument, Connection.TRANSACTION_READ_COMMITTED);
  }

  public <V> V runInTransaction(StatementRunnableWithResult<V> runnableWithResult, Object argument, int isolationLevel) {
    JdbcConnection connection = beginTransaction(isolationLevel);
    V result;

    try {
      result = runnableWithResult.run(connection, argument);
    }
    catch (Throwable throwable) {
      connection.rollback();
      throw new PersistenceException("An error occurred while executing StatementRunnableWithResult. Transaction rolled back.", throwable);
    }

    connection.commit();
    return result;
  }

  //

  public void setTypeHandlerRegistry(TypeHandlerRegistry typeHandlerRegistry) {
    this.typeHandlerRegistry = typeHandlerRegistry;
  }

  public TypeHandlerRegistry getTypeHandlerRegistry() {
    return typeHandlerRegistry;
  }
}
