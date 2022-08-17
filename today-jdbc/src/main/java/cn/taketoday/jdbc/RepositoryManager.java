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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.parsing.QueryParameter;
import cn.taketoday.jdbc.parsing.SqlParameterParser;
import cn.taketoday.jdbc.result.PrimitiveTypeNullHandler;
import cn.taketoday.jdbc.sql.EntityManager;
import cn.taketoday.jdbc.support.ClobToStringConverter;
import cn.taketoday.jdbc.support.ConnectionSource;
import cn.taketoday.jdbc.support.DataSourceConnectionSource;
import cn.taketoday.jdbc.support.OffsetTimeToSQLTimeConverter;
import cn.taketoday.jdbc.support.TimeToJodaLocalTimeConverter;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * RepositoryManager is the main class for the today-jdbc library.
 * <p>
 * An <code>RepositoryManager</code> instance represents a way of connecting to one specific
 * database. To create a new instance, one need to specify either jdbc-url,
 * username and password for the database or a data source.
 * <p>
 * Internally the RepositoryManager instance uses a data source to create jdbc connections
 * to the database. If url, username and password was specified in the
 * constructor, a simple data source is created, which works as a simple wrapper
 * around the jdbc driver.
 * <p>
 * This library is learned from <a href="https://github.com/aaberg/sql2o">Sql2o</a>
 *
 * @author Hubery Huang
 * @author <a href="https://github.com/aaberg">Lars Aaberg</a>
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RepositoryManager {

  private TypeHandlerRegistry typeHandlerRegistry = TypeHandlerRegistry.getSharedInstance();

  private boolean defaultCaseSensitive;
  private boolean generatedKeys = true;
  private ConnectionSource connectionSource;
  private Map<String, String> defaultColumnMappings;
  private SqlParameterParser sqlParameterParser = new SqlParameterParser();

  private ConversionService conversionService;

  @Nullable
  private PrimitiveTypeNullHandler primitiveTypeNullHandler;

  private EntityManager entityManager;

  public RepositoryManager(String jndiLookup) {
    this(DataSourceUtils.getJndiDatasource(jndiLookup));
  }

  /**
   * Creates a new instance of the RepositoryManager class.
   * Internally this constructor will create a GenericConnectionSource
   *
   * @param url JDBC database url
   * @param user database username
   * @param pass database password
   * @see cn.taketoday.jdbc.support.GenericConnectionSource
   */
  public RepositoryManager(String url, String user, String pass) {
    this(ConnectionSource.from(url, user, pass));
  }

  /**
   * Creates a new instance of the RepositoryManager class, which uses the given DataSource to
   * acquire connections to the database.
   *
   * @param dataSource The DataSource RepositoryManager uses to acquire connections to the database.
   */
  public RepositoryManager(DataSource dataSource) {
    this(ConnectionSource.fromDataSource(dataSource));
  }

  public RepositoryManager(DataSource dataSource, boolean generatedKeys) {
    this(ConnectionSource.fromDataSource(dataSource));
    this.generatedKeys = generatedKeys;
  }

  public RepositoryManager(ConnectionSource source) {
    this.connectionSource = source;
    final DefaultConversionService sharedInstance = DefaultConversionService.getSharedInstance();
    sharedInstance.addConverter(new ClobToStringConverter());
    sharedInstance.addConverter(new OffsetTimeToSQLTimeConverter());
    if (FeatureDetector.isJodaTimeAvailable()) {
      sharedInstance.addConverter(new TimeToJodaLocalTimeConverter());
    }
    this.conversionService = sharedInstance;
  }

  public RepositoryManager(ConnectionSource source, ConversionService conversionService) {
    this.connectionSource = source;
    this.conversionService = conversionService;
  }

  /**
   * Gets the DataSource that RepositoryManager uses internally to acquire database
   * connections.
   *
   * @return The DataSource instance
   */
  public DataSource getDataSource() {
    if (connectionSource instanceof DataSourceConnectionSource)
      return ((DataSourceConnectionSource) connectionSource).getDataSource();
    else
      return null;
  }

  /**
   * Gets the {@link ConnectionSource} that RepositoryManager uses internally to acquire
   * database connections.
   *
   * @return The ConnectionSource instance
   */
  public ConnectionSource getConnectionSource() {
    return connectionSource;
  }

  /**
   * Sets the {@link ConnectionSource} that RepositoryManager uses internally to acquire
   * database connections.
   *
   * @param connectionSource the ConnectionSource instance to use
   */
  public void setConnectionSource(ConnectionSource connectionSource) {
    this.connectionSource = connectionSource;
  }

  /**
   * Gets the default column mappings Map. column mappings added to this Map are
   * always available when RepositoryManager attempts to map between result sets and object
   * instances.
   *
   * @return The {@link Map<String,String>} instance, which RepositoryManager internally uses
   * to map column names with property names.
   */
  public Map<String, String> getDefaultColumnMappings() {
    return defaultColumnMappings;
  }

  /**
   * Sets the default column mappings Map.
   *
   * @param defaultColumnMappings A {@link Map} instance RepositoryManager uses internally to map between column
   * names and property names.
   */
  public void setDefaultColumnMappings(Map<String, String> defaultColumnMappings) {
    this.defaultColumnMappings = defaultColumnMappings;
  }

  /**
   * Gets value indicating if this instance of RepositoryManager is case sensitive when
   * mapping between columns names and property names.
   */
  public boolean isDefaultCaseSensitive() {
    return defaultCaseSensitive;
  }

  /**
   * Sets a value indicating if this instance of RepositoryManager is case sensitive when
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

  public void setSqlParameterParser(SqlParameterParser sqlParameterParser) {
    Assert.notNull(sqlParameterParser, "SqlParameterParser is required");
    this.sqlParameterParser = sqlParameterParser;
  }

  public SqlParameterParser getSqlParameterParser() {
    return sqlParameterParser;
  }

  public void setTypeHandlerRegistry(@Nullable TypeHandlerRegistry typeHandlerRegistry) {
    this.typeHandlerRegistry =
            typeHandlerRegistry == null ? TypeHandlerRegistry.getSharedInstance() : typeHandlerRegistry;
  }

  public TypeHandlerRegistry getTypeHandlerRegistry() {
    return typeHandlerRegistry;
  }

  /**
   * set {@link ConversionService} to convert keys or other object
   *
   * @param conversionService ConversionService
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService == null
                             ? ApplicationConversionService.getSharedInstance() : conversionService;
  }

  public ConversionService getConversionService() {
    return conversionService;
  }

  /**
   * set {@link PrimitiveTypeNullHandler}
   * to handle null values when property is PrimitiveType
   *
   * @param primitiveTypeNullHandler PrimitiveTypeNullHandler
   */
  public void setPrimitiveTypeNullHandler(@Nullable PrimitiveTypeNullHandler primitiveTypeNullHandler) {
    this.primitiveTypeNullHandler = primitiveTypeNullHandler;
  }

  /**
   * @return {@link PrimitiveTypeNullHandler}
   */
  @Nullable
  public PrimitiveTypeNullHandler getPrimitiveTypeNullHandler() {
    return primitiveTypeNullHandler;
  }

  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  //

  protected String parse(String sql, Map<String, QueryParameter> paramNameToIdxMap) {
    return sqlParameterParser.parse(sql, paramNameToIdxMap);
  }

  // Query

  /**
   * Creates a {@link Query}
   * <p>
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>
   * try (Connection con = repositoryManager.open()) {
   *    return repositoryManager.createQuery(query, name, returnGeneratedKeys)
   *                .fetch(Pojo.class);
   * }
   * </pre>
   * </p>
   *
   * @param query the sql query string
   * @param returnGeneratedKeys boolean value indicating if the database should return any
   * generated keys.
   * @return the {@link Query} instance
   */
  public Query createQuery(String query, boolean returnGeneratedKeys) {
    return open(true).createQuery(query, returnGeneratedKeys);
  }

  /**
   * Creates a {@link Query}
   *
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>
   *     try (Connection con = repositoryManager.open()) {
   *         return repositoryManager.createQuery(query, name)
   *                      .fetch(Pojo.class);
   *     }
   *  </pre>
   *
   * @param query the sql query string
   * @return the {@link Query} instance
   */
  public Query createQuery(String query) {
    return open(true).createQuery(query);
  }

  // JdbcConnection

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
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public JdbcConnection open(boolean autoClose) {
    return new JdbcConnection(this, connectionSource, autoClose);
  }

  /**
   * Opens a connection to the database
   *
   * @param connection the {@link Connection}
   * @return instance of the {@link JdbcConnection} class.
   */
  public JdbcConnection open(Connection connection) {
    return new JdbcConnection(this, connection, false);
  }

  /**
   * Opens a connection to the database
   *
   * @param connectionSource the {@link ConnectionSource} implementation substitution, that
   * will be used instead of one from {@link RepositoryManager} instance.
   * @return instance of the {@link JdbcConnection} class.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public JdbcConnection open(ConnectionSource connectionSource) {
    return new JdbcConnection(this, connectionSource, false);
  }

  /**
   * Invokes the run method on the {@link ResultStatementRunnable}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   *
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V> V withConnection(ResultStatementRunnable<V> runnable, @Nullable Object argument) {
    try (JdbcConnection connection = open()) {
      return runnable.run(connection, argument);
    }
    catch (Throwable t) {
      throw new PersistenceException("An error occurred while executing StatementRunnable", t);
    }
  }

  /**
   * Invokes the run method on the {@link ResultStatementRunnable}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   *
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V> V withConnection(ResultStatementRunnable<V> runnable) {
    return withConnection(runnable, null);
  }

  /**
   * Invokes the run method on the {@link ResultStatementRunnable}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   *
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public void withConnection(StatementRunnable runnable) {
    withConnection(runnable, null);
  }

  /**
   * Invokes the run method on the {@link ResultStatementRunnable}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   *
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public void withConnection(StatementRunnable runnable, @Nullable Object argument) {
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
   * @param isolationLevel the isolation level of the transaction
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
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
   * @param source the {@link ConnectionSource} implementation substitution, that
   * will be used instead of one from {@link RepositoryManager} instance.
   * @param isolationLevel the isolation level of the transaction
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public JdbcConnection beginTransaction(ConnectionSource source, int isolationLevel) {
    JdbcConnection connection = new JdbcConnection(this, source, false);
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
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
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
   * @param source the {@link ConnectionSource} implementation substitution, that
   * will be used instead of one from {@link RepositoryManager} instance.
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public JdbcConnection beginTransaction(ConnectionSource source) {
    return beginTransaction(source, Connection.TRANSACTION_READ_COMMITTED);
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
   * @param connection the {@link Connection}
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   */
  public JdbcConnection beginTransaction(Connection connection) {
    return beginTransaction(ConnectionSource.join(connection), Connection.TRANSACTION_READ_COMMITTED);
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
   * @param runnable The {@link StatementRunnable} instance.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
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
   * @param runnable The {@link StatementRunnable} instance.
   * @param argument An argument which will be forwarded to the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
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
   * @param runnable The {@link StatementRunnable} instance.
   * @param argument An argument which will be forwarded to the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method
   * @param isolationLevel The isolation level of the transaction
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
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

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V> V runInTransaction(ResultStatementRunnable<V> runnable) {
    return runInTransaction(runnable, null);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V> V runInTransaction(ResultStatementRunnable<V> runnable, Object argument) {
    return runInTransaction(runnable, argument, Connection.TRANSACTION_READ_COMMITTED);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V> V runInTransaction(ResultStatementRunnable<V> runnable, Object argument, int isolationLevel) {
    JdbcConnection connection = beginTransaction(isolationLevel);
    V result;
    try {
      result = runnable.run(connection, argument);
    }
    catch (Throwable e) {
      connection.rollback();
      throw new PersistenceException(
              "An error occurred while executing StatementRunnableWithResult. Transaction rolled back.", e);
    }
    connection.commit();
    return result;
  }

  //

  /**
   * persist an entity to underlying repository
   *
   * @param entity entity instance
   * @throws IllegalArgumentException if the instance is not an entity
   */
  public void persist(Object entity) {
    entityManager.persist(entity);
  }

  /**
   * Merge the state of the given entity into underlying repository
   *
   * @param entity entity instance
   * @throws IllegalArgumentException if instance is not an
   * entity or is a removed entity
   */
  public void merge(Object entity) {
    entityManager.merge(entity);
  }

  //

  public <T> TypeHandler<T> getTypeHandler(BeanProperty property) {
    return typeHandlerRegistry.getTypeHandler(property);
  }

  public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
    return typeHandlerRegistry.getTypeHandler(type);
  }

}
