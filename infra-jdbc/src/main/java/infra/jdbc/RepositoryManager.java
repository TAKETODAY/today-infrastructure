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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import infra.core.conversion.ConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.dao.DataAccessException;
import infra.format.support.ApplicationConversionService;
import infra.jdbc.datasource.DataSourceUtils;
import infra.jdbc.datasource.DriverManagerDataSource;
import infra.jdbc.datasource.SingleConnectionDataSource;
import infra.jdbc.parsing.QueryParameter;
import infra.jdbc.parsing.SqlParameterParser;
import infra.jdbc.support.JdbcAccessor;
import infra.jdbc.support.JdbcTransactionManager;
import infra.jdbc.type.TypeHandlerManager;
import infra.lang.Assert;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.annotation.Isolation;

/**
 * RepositoryManager is the main class for the infra-jdbc library.
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
public class RepositoryManager extends JdbcAccessor implements QueryProducer {

  private final PlatformTransactionManager transactionManager;

  private boolean defaultCaseSensitive;

  private boolean generatedKeys = true;

  private boolean catchResourceCloseErrors = false;

  private SqlParameterParser sqlParameterParser = new SqlParameterParser();

  private TypeHandlerManager typeHandlerManager = TypeHandlerManager.sharedInstance;

  private ConversionService conversionService = DefaultConversionService.getSharedInstance();

  private @Nullable Map<String, String> defaultColumnMappings;

  private @Nullable PrimitiveTypeNullHandler primitiveTypeNullHandler;

  public RepositoryManager(String jndiLookup) {
    this(DataSourceUtils.getJndiDatasource(jndiLookup));
  }

  /**
   * Creates a new instance of the RepositoryManager class.
   * Internally this constructor will create a DriverManagerDataSource
   *
   * @param url JDBC database url
   * @param user database username
   * @param pass database password
   * @see DriverManagerDataSource
   */
  public RepositoryManager(String url, String user, String pass) {
    this(new DriverManagerDataSource(url, user, pass));
  }

  /**
   * Creates a new instance of the RepositoryManager class, which uses the given DataSource to
   * acquire connections to the database.
   *
   * @param dataSource The DataSource RepositoryManager uses to acquire connections to the database.
   */
  public RepositoryManager(DataSource dataSource) {
    this(dataSource, new JdbcTransactionManager(dataSource));
  }

  /**
   * Creates a new instance of the RepositoryManager class, which uses the given DataSource to
   * acquire connections to the database.
   *
   * @param dataSource The DataSource RepositoryManager uses to
   * acquire connections to the database.
   */
  public RepositoryManager(DataSource dataSource, PlatformTransactionManager transactionManager) {
    Assert.notNull(transactionManager, "transactionManager is required");
    setDataSource(dataSource);
    this.transactionManager = transactionManager;
  }

  /**
   * Gets the default column mappings Map. column mappings added to this Map are
   * always available when RepositoryManager attempts to map between result sets and object
   * instances.
   *
   * @return The {@code Map<String,String>} instance, which RepositoryManager internally uses
   * to map column names with property names.
   */
  public @Nullable Map<String, String> getDefaultColumnMappings() {
    return defaultColumnMappings;
  }

  /**
   * Sets the default column mappings Map.
   *
   * @param defaultColumnMappings A {@link Map} instance RepositoryManager uses
   * internally to map between column names and property names.
   */
  public void setDefaultColumnMappings(@Nullable Map<String, String> defaultColumnMappings) {
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
   * Sets a value indicating if this instance of RepositoryManager is case-sensitive when
   * mapping between columns names and property names. This should almost always
   * be false, because most relational databases are not case-sensitive.
   */
  public void setDefaultCaseSensitive(boolean defaultCaseSensitive) {
    this.defaultCaseSensitive = defaultCaseSensitive;
  }

  /**
   * Sets whether generated keys should be returned by default for queries executed through this RepositoryManager.
   *
   * @param generatedKeys true if queries should return generated keys by default, false otherwise
   */
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

  /**
   * Sets the {@link SqlParameterParser} to be used for parsing SQL parameters.
   *
   * @param sqlParameterParser the {@link SqlParameterParser} to be used for parsing SQL parameters.
   * @throws IllegalArgumentException if the provided {@link SqlParameterParser} is null.
   */
  public void setSqlParameterParser(SqlParameterParser sqlParameterParser) {
    Assert.notNull(sqlParameterParser, "SqlParameterParser is required");
    this.sqlParameterParser = sqlParameterParser;
  }

  /**
   * Gets the {@link SqlParameterParser} used for parsing SQL parameters.
   *
   * @return the {@link SqlParameterParser} instance used for parsing SQL parameters.
   */
  public SqlParameterParser getSqlParameterParser() {
    return sqlParameterParser;
  }

  /**
   * Sets the {@link TypeHandlerManager} to be used for handling type conversions.
   *
   * @param typeHandlerManager the {@link TypeHandlerManager} instance to be used,
   * if null then the shared instance ({@link TypeHandlerManager#sharedInstance}) will be used
   */
  public void setTypeHandlerManager(@Nullable TypeHandlerManager typeHandlerManager) {
    this.typeHandlerManager =
            typeHandlerManager == null ? TypeHandlerManager.sharedInstance : typeHandlerManager;
  }

  /**
   * Gets the {@link TypeHandlerManager} used for handling type conversions.
   *
   * @return the current {@link TypeHandlerManager} instance
   */
  public TypeHandlerManager getTypeHandlerManager() {
    return typeHandlerManager;
  }

  /**
   * Sets the {@link ConversionService} to be used for converting keys or other objects.
   *
   * @param conversionService the {@link ConversionService} instance to be used,
   * if null then the shared instance ({@link ApplicationConversionService#getSharedInstance()}) will be used
   */
  public void setConversionService(@Nullable ConversionService conversionService) {
    this.conversionService = conversionService == null
            ? ApplicationConversionService.getSharedInstance() : conversionService;
  }

  /**
   * Gets the {@link ConversionService} used for converting keys or other objects.
   *
   * @return the current {@link ConversionService} instance
   */
  public ConversionService getConversionService() {
    return conversionService;
  }

  /**
   * Sets the {@link PrimitiveTypeNullHandler} to handle null values when the property is of primitive type.
   * This handler will be used to manage scenarios where a null value needs to be assigned to a primitive type field,
   * which would normally cause an issue since primitive types cannot hold null values.
   *
   * @param primitiveTypeNullHandler the {@link PrimitiveTypeNullHandler} instance to be used,
   * may be null if no special handling of primitive type nulls is needed
   */
  public void setPrimitiveTypeNullHandler(@Nullable PrimitiveTypeNullHandler primitiveTypeNullHandler) {
    this.primitiveTypeNullHandler = primitiveTypeNullHandler;
  }

  /**
   * Returns the currently configured {@link PrimitiveTypeNullHandler}.
   * This handler is responsible for managing null values when they need to be assigned to primitive type properties.
   *
   * @return the current {@link PrimitiveTypeNullHandler} instance, or null if none has been set
   */
  public @Nullable PrimitiveTypeNullHandler getPrimitiveTypeNullHandler() {
    return primitiveTypeNullHandler;
  }

  /**
   * Sets whether to catch and ignore errors when closing resources such as connections, statements,
   * and result sets. When set to {@code true}, any exceptions thrown during resource cleanup
   * will be silently ignored. When {@code false} (default), such exceptions will be propagated.
   *
   * @param catchResourceCloseErrors {@code true} to suppress resource closing errors, {@code false} otherwise
   */
  public void setCatchResourceCloseErrors(boolean catchResourceCloseErrors) {
    this.catchResourceCloseErrors = catchResourceCloseErrors;
  }

  /**
   * Returns whether to catch and ignore errors when closing resources such as connections, statements,
   * and result sets.
   *
   * @return {@code true} if resource closing errors are suppressed, {@code false} otherwise
   */
  public boolean isCatchResourceCloseErrors() {
    return catchResourceCloseErrors;
  }

  /**
   * Return the transaction management strategy to be used.
   */
  public PlatformTransactionManager getTransactionManager() {
    return this.transactionManager;
  }

  //

  protected String parse(String sql, Map<String, QueryParameter> paramNameToIdxMap) {
    return sqlParameterParser.parse(sql, paramNameToIdxMap);
  }

  // ---------------------------------------------------------------------
  // Implementation of QueryProducer methods
  // ---------------------------------------------------------------------

  /**
   * Creates a {@link Query}
   * <p>
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre> {@code
   * try (Connection con = repositoryManager.open()) {
   *    return repositoryManager.createQuery(query, name, returnGeneratedKeys)
   *                .fetch(Pojo.class);
   * }
   * }</pre>
   * </p>
   *
   * @param query the sql query string
   * @param returnGeneratedKeys boolean value indicating if the database should return any
   * generated keys.
   * @return the {@link NamedQuery} instance
   */
  @Override
  public Query createQuery(String query, boolean returnGeneratedKeys) {
    return open(true).createQuery(query, returnGeneratedKeys);
  }

  /**
   * Creates a {@link Query}
   *
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>{@code
   *     try (Connection con = repositoryManager.open()) {
   *         return repositoryManager.createQuery(query, name)
   *                      .fetch(Pojo.class);
   *     }
   *  }</pre>
   *
   * @param query the sql query string
   * @return the {@link NamedQuery} instance
   */
  @Override
  public Query createQuery(String query) {
    return open(true).createQuery(query);
  }

  /**
   * Creates a {@link NamedQuery}
   * <p>
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>{@code
   * try (Connection con = repositoryManager.open()) {
   *    return repositoryManager.createNamedQuery(query, name, returnGeneratedKeys)
   *                .fetch(Pojo.class);
   * }
   * }</pre>
   * </p>
   *
   * @param query the sql query string
   * @param returnGeneratedKeys boolean value indicating if the database should return any
   * generated keys.
   * @return the {@link NamedQuery} instance
   */
  @Override
  public NamedQuery createNamedQuery(String query, boolean returnGeneratedKeys) {
    return open(true).createNamedQuery(query, returnGeneratedKeys);
  }

  /**
   * Creates a {@link NamedQuery}
   *
   * better to use :
   * create queries with {@link JdbcConnection} class instead,
   * using try-with-resource blocks
   * <pre>{@code
   *     try (Connection con = repositoryManager.open()) {
   *         return repositoryManager.createNamedQuery(query, name)
   *                      .fetch(Pojo.class);
   *     }
   *  }</pre>
   *
   * @param query the sql query string
   * @return the {@link NamedQuery} instance
   */
  @Override
  public NamedQuery createNamedQuery(String query) {
    return open(true).createNamedQuery(query);
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
    return new JdbcConnection(this, obtainDataSource(), autoClose);
  }

  /**
   * Opens a connection to the database
   *
   * @param connection the {@link Connection}
   * @return instance of the {@link JdbcConnection} class.
   */
  public JdbcConnection open(Connection connection) {
    NestedConnection nested = new NestedConnection(connection);
    return new JdbcConnection(this, new SingleConnectionDataSource(nested, false), false);
  }

  /**
   * Opens a connection to the database
   *
   * @param dataSource the {@link DataSource} implementation substitution, that
   * will be used instead of one from {@link RepositoryManager} instance.
   * @return instance of the {@link JdbcConnection} class.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public JdbcConnection open(DataSource dataSource) {
    return new JdbcConnection(this, dataSource, false);
  }

  /**
   * Invokes the run method on the {@link ResultStatementRunnable}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   *
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V extends @Nullable Object, P extends @Nullable Object> V withConnection(ResultStatementRunnable<V, P> runnable, @Nullable P argument) {
    try (JdbcConnection connection = open()) {
      return runnable.run(connection, argument);
    }
    catch (DataAccessException e) {
      throw e;
    }
    catch (SQLException e) {
      throw translateException("Executing StatementRunnable", null, e);
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
  public <V extends @Nullable Object, P extends @Nullable Object> V withConnection(ResultStatementRunnable<V, P> runnable) {
    return withConnection(runnable, null);
  }

  /**
   * Invokes the run method on the {@link ResultStatementRunnable}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   *
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <T extends @Nullable Object> void withConnection(StatementRunnable<T> runnable) {
    withConnection(runnable, null);
  }

  /**
   * Invokes the run method on the {@link ResultStatementRunnable}
   * instance. This method guarantees that the connection is closed properly, when
   * either the run method completes or if an exception occurs.
   *
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <T extends @Nullable Object> void withConnection(StatementRunnable<T> runnable, T argument) {
    try (JdbcConnection connection = open()) {
      runnable.run(connection, argument);
    }
    catch (DataAccessException e) {
      throw e;
    }
    catch (SQLException e) {
      throw translateException("Executing StatementRunnable", null, e);
    }
    catch (Throwable t) {
      throw new PersistenceException("An error occurred while executing StatementRunnable", t);
    }
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
    return beginTransaction(obtainDataSource(), TransactionDefinition.forIsolationLevel(isolationLevel));
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
  public JdbcConnection beginTransaction(Isolation isolationLevel) {
    return beginTransaction(obtainDataSource(), TransactionDefinition.forIsolationLevel(isolationLevel));
  }

  /**
   * Begins a transaction with the given {@link TransactionDefinition}.
   * Every statement executed on the return {@link JdbcConnection} instance,
   * will be executed in the transaction. It is very important to always
   * call either the {@link JdbcConnection#commit()} method or the
   * {@link JdbcConnection#rollback()} method to close the transaction. Use
   * proper try-catch logic.
   *
   * @param definition the TransactionDefinition instance (can be {@code null} for defaults),
   * describing propagation behavior, isolation level, timeout etc.
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public JdbcConnection beginTransaction(@Nullable TransactionDefinition definition) {
    return beginTransaction(obtainDataSource(), definition);
  }

  /**
   * Begins a transaction with the {@link TransactionDefinition}. Every statement executed
   * on the return {@link JdbcConnection} instance, will be executed in the
   * transaction. It is very important to always call either the
   * {@link JdbcConnection#commit()} method or the
   * {@link JdbcConnection#rollback()} method to close the transaction. Use
   * proper try-catch logic.
   *
   * @param source the {@link DataSource} implementation substitution, that
   * will be used instead of one from {@link RepositoryManager} instance.
   * @param definition the TransactionDefinition instance (can be {@code null} for defaults),
   * describing propagation behavior, isolation level, timeout etc.
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public JdbcConnection beginTransaction(DataSource source, @Nullable TransactionDefinition definition) {
    JdbcConnection connection = new JdbcConnection(this, source);
    connection.beginTransaction(definition);
    connection.createConnection();
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
   * @param root the {@link Connection}
   * @return the {@link JdbcConnection} instance to use to run statements in the
   * transaction.
   */
  public JdbcConnection beginTransaction(Connection root) {
    JdbcConnection connection = open(root);
    boolean success = false;
    try {
      root.setAutoCommit(false);
      root.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
      success = true;
    }
    catch (SQLException e) {
      throw translateException("Setting transaction options", null, e);
    }
    finally {
      if (!success) {
        connection.close();
      }
    }

    return connection;
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
   * <p>
   * The isolation level of the transaction will be set to
   * {@link Connection#TRANSACTION_READ_COMMITTED}
   *
   * @param runnable The {@link StatementRunnable} instance.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <T extends @Nullable Object> void runInTransaction(StatementRunnable<T> runnable) {
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
   * <p>
   * The isolation level of the transaction will be set to
   * {@link Connection#TRANSACTION_READ_COMMITTED}
   *
   * @param runnable The {@link StatementRunnable} instance.
   * @param argument An argument which will be forwarded to the
   * {@link StatementRunnable#run(JdbcConnection, Object) run} method
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <T extends @Nullable Object> void runInTransaction(StatementRunnable<T> runnable, T argument) {
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
  public <T extends @Nullable Object> void runInTransaction(StatementRunnable<T> runnable, T argument, int isolationLevel) {
    JdbcConnection connection = beginTransaction(isolationLevel);
    connection.setRollbackOnException(false);

    try {
      runnable.run(connection, argument);
    }
    catch (Throwable throwable) {
      connection.rollback();
      if (throwable instanceof DataAccessException e) {
        throw e;
      }
      else if (throwable instanceof SQLException e) {
        throw translateException("Running in transaction", null, e);
      }
      throw new PersistenceException("An error occurred while executing StatementRunnable. Transaction is rolled back.", throwable);
    }
    connection.commit();
  }

  /**
   * Executes the given {@link ResultStatementRunnable} in a transaction with default isolation level.
   * The transaction will automatically be committed if the runnable completes successfully,
   * or rolled back if an exception is thrown.
   *
   * @param runnable the {@link ResultStatementRunnable} to execute
   * @param <V> the return type of the runnable
   * @param <P> the parameter type passed to the runnable
   * @return the result of the runnable execution
   * @throws CannotGetJdbcConnectionException if unable to acquire a connection from the connection source
   */
  public <V extends @Nullable Object, P extends @Nullable Object> V runInTransaction(ResultStatementRunnable<V, P> runnable) {
    return runInTransaction(runnable, null);
  }

  /**
   * Executes the given {@link ResultStatementRunnable} in a transaction with default isolation level.
   * The transaction will automatically be committed if the runnable completes successfully,
   * or rolled back if an exception is thrown.
   *
   * @param runnable the {@link ResultStatementRunnable} to execute
   * @param argument the argument to pass to the runnable
   * @param <V> the return type of the runnable
   * @param <P> the parameter type passed to the runnable
   * @return the result of the runnable execution
   * @throws CannotGetJdbcConnectionException if unable to acquire a connection from the connection source
   */
  public <V extends @Nullable Object, P extends @Nullable Object> V runInTransaction(ResultStatementRunnable<V, P> runnable, P argument) {
    return runInTransaction(runnable, argument, Connection.TRANSACTION_READ_COMMITTED);
  }

  /**
   * Executes the given {@link ResultStatementRunnable} in a transaction with the specified isolation level.
   * The transaction will automatically be committed if the runnable completes successfully,
   * or rolled back if an exception is thrown.
   *
   * @param runnable the {@link ResultStatementRunnable} to execute
   * @param argument the argument to pass to the runnable
   * @param isolationLevel the isolation level of the transaction
   * @param <V> the return type of the runnable
   * @param <P> the parameter type passed to the runnable
   * @return the result of the runnable execution
   * @throws CannotGetJdbcConnectionException if unable to acquire a connection from the connection source
   */
  public <V extends @Nullable Object, P extends @Nullable Object> V runInTransaction(ResultStatementRunnable<V, P> runnable, P argument, int isolationLevel) {
    return runInTransaction(runnable, argument, TransactionDefinition.forIsolationLevel(isolationLevel));
  }

  /**
   * Executes the given {@link ResultStatementRunnable} in a transaction with the specified isolation level.
   * The transaction will automatically be committed if the runnable completes successfully,
   * or rolled back if an exception is thrown.
   *
   * @param runnable the {@link ResultStatementRunnable} to execute
   * @param argument the argument to pass to the runnable
   * @param isolation the isolation level of the transaction
   * @param <V> the return type of the runnable
   * @param <P> the parameter type passed to the runnable
   * @return the result of the runnable execution
   * @throws CannotGetJdbcConnectionException if unable to acquire a connection from the connection source
   */
  public <V extends @Nullable Object, P extends @Nullable Object> V runInTransaction(ResultStatementRunnable<V, P> runnable, P argument, Isolation isolation) {
    return runInTransaction(runnable, argument, TransactionDefinition.forIsolationLevel(isolation));
  }

  /**
   * Executes the given {@link ResultStatementRunnable} in a transaction with the specified transaction definition.
   * The transaction will automatically be committed if the runnable completes successfully,
   * or rolled back if an exception is thrown.
   *
   * @param runnable the {@link ResultStatementRunnable} to execute
   * @param argument the argument to pass to the runnable
   * @param definition the TransactionDefinition instance (can be {@code null} for defaults),
   * describing propagation behavior, isolation level, timeout etc.
   * @param <V> the return type of the runnable
   * @param <P> the parameter type passed to the runnable
   * @return the result of the runnable execution
   * @throws CannotGetJdbcConnectionException if unable to acquire a connection from the connection source
   */
  public <V extends @Nullable Object, P extends @Nullable Object> V runInTransaction(ResultStatementRunnable<V, P> runnable,
          P argument, @Nullable TransactionDefinition definition) {
    JdbcConnection connection = beginTransaction(definition);
    V result;
    try {
      result = runnable.run(connection, argument);
    }
    catch (Throwable ex) {
      connection.rollback();
      if (ex instanceof DataAccessException e) {
        throw e;
      }
      else if (ex instanceof SQLException e) {
        throw translateException("Running in transaction", null, e);
      }
      throw new PersistenceException(
              "An error occurred while executing ResultStatementRunnable. Transaction rolled back.", ex);
    }
    connection.commit();
    return result;
  }

}
