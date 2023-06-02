/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.format.support.ApplicationConversionService;
import cn.taketoday.jdbc.datasource.DataSourceTransactionManager;
import cn.taketoday.jdbc.datasource.DataSourceUtils;
import cn.taketoday.jdbc.datasource.DriverManagerDataSource;
import cn.taketoday.jdbc.datasource.SingleConnectionDataSource;
import cn.taketoday.jdbc.parsing.QueryParameter;
import cn.taketoday.jdbc.parsing.SqlParameterParser;
import cn.taketoday.jdbc.persistence.DefaultEntityManager;
import cn.taketoday.jdbc.persistence.EntityManager;
import cn.taketoday.jdbc.result.PrimitiveTypeNullHandler;
import cn.taketoday.jdbc.support.ClobToStringConverter;
import cn.taketoday.jdbc.support.JdbcAccessor;
import cn.taketoday.jdbc.support.OffsetTimeToSQLTimeConverter;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.transaction.TransactionException;
import cn.taketoday.transaction.TransactionStatus;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.annotation.Isolation;
import cn.taketoday.transaction.support.CallbackPreferringPlatformTransactionManager;
import cn.taketoday.transaction.support.TransactionCallback;

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
public class RepositoryManager extends JdbcAccessor implements QueryProducer {

  private TypeHandlerRegistry typeHandlerRegistry = TypeHandlerRegistry.getSharedInstance();

  private boolean defaultCaseSensitive;
  private boolean generatedKeys = true;
  private boolean catchResourceCloseErrors = true;

  private Map<String, String> defaultColumnMappings;
  private SqlParameterParser sqlParameterParser = new SqlParameterParser();

  private ConversionService conversionService;

  @Nullable
  private PrimitiveTypeNullHandler primitiveTypeNullHandler;

  @Nullable
  private EntityManager entityManager;

  private DataSourceTransactionManager transactionManager = new DataSourceTransactionManager();

  {
    // TODO Converter 问题
    DefaultConversionService sharedInstance = DefaultConversionService.getSharedInstance();
    sharedInstance.addConverter(new ClobToStringConverter());
    sharedInstance.addConverter(new OffsetTimeToSQLTimeConverter());
    this.conversionService = sharedInstance;
  }

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
    setDataSource(dataSource);
  }

  public RepositoryManager(DataSource dataSource, boolean generatedKeys) {
    setDataSource(dataSource);
    this.generatedKeys = generatedKeys;
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

  public EntityManager getEntityManager() {
    if (entityManager == null) {
      entityManager = new DefaultEntityManager(this);
    }
    return entityManager;
  }

  public void setCatchResourceCloseErrors(boolean catchResourceCloseErrors) {
    this.catchResourceCloseErrors = catchResourceCloseErrors;
  }

  public boolean isCatchResourceCloseErrors() {
    return catchResourceCloseErrors;
  }

  /**
   * Set the transaction management strategy to be used.
   */
  public void setTransactionManager(DataSourceTransactionManager transactionManager) {
    Assert.notNull(transactionManager, "transactionManager is required");
    this.transactionManager = transactionManager;
  }

  @Override
  public void setDataSource(@Nullable DataSource dataSource) {
    super.setDataSource(dataSource);
    transactionManager.setDataSource(dataSource);
  }

  /**
   * Return the transaction management strategy to be used.
   */
  public DataSourceTransactionManager getTransactionManager() {
    return this.transactionManager;
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
   * <pre>
   *     try (Connection con = repositoryManager.open()) {
   *         return repositoryManager.createQuery(query, name)
   *                      .fetch(Pojo.class);
   *     }
   *  </pre>
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
   * <pre>
   * try (Connection con = repositoryManager.open()) {
   *    return repositoryManager.createNamedQuery(query, name, returnGeneratedKeys)
   *                .fetch(Pojo.class);
   * }
   * </pre>
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
   * <pre>
   *     try (Connection con = repositoryManager.open()) {
   *         return repositoryManager.createNamedQuery(query, name)
   *                      .fetch(Pojo.class);
   *     }
   *  </pre>
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
  public <V, P> V withConnection(ResultStatementRunnable<V, P> runnable, @Nullable P argument) {
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
  public <V, P> V withConnection(ResultStatementRunnable<V, P> runnable) {
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
   * @param definition the options of the transaction
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
   * @param definition the options of the transaction
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
   *
   * The isolation level of the transaction will be set to
   * {@link Connection#TRANSACTION_READ_COMMITTED}
   *
   * @param runnable The {@link StatementRunnable} instance.
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <T> void runInTransaction(StatementRunnable<T> runnable) {
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
  public <T> void runInTransaction(StatementRunnable<T> runnable, @Nullable T argument) {
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
  public <T> void runInTransaction(StatementRunnable<T> runnable, @Nullable T argument, int isolationLevel) {
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
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V, P> V runInTransaction(ResultStatementRunnable<V, P> runnable) {
    return runInTransaction(runnable, null);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V, P> V runInTransaction(ResultStatementRunnable<V, P> runnable, @Nullable P argument) {
    return runInTransaction(runnable, argument, Connection.TRANSACTION_READ_COMMITTED);
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V, P> V runInTransaction(ResultStatementRunnable<V, P> runnable, @Nullable P argument, int isolationLevel) {
    return runInTransaction(runnable, argument, TransactionDefinition.forIsolationLevel(isolationLevel));
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V, P> V runInTransaction(ResultStatementRunnable<V, P> runnable, @Nullable P argument, Isolation isolation) {
    return runInTransaction(runnable, argument, TransactionDefinition.forIsolationLevel(isolation));
  }

  /**
   * @throws CannotGetJdbcConnectionException Could not acquire a connection from connection-source
   */
  public <V, P> V runInTransaction(ResultStatementRunnable<V, P> runnable,
          @Nullable P argument, @Nullable TransactionDefinition definition) {
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

  public <T> T runInTransaction(TransactionCallback<T> action) throws TransactionException {
    return runInTransaction(action, (TransactionDefinition) null);
  }

  public <T> T runInTransaction(TransactionCallback<T> action, @Nullable Isolation isolation) throws TransactionException {
    if (isolation != null) {
      return runInTransaction(action, TransactionDefinition.forIsolationLevel(isolation));
    }
    return runInTransaction(action, (TransactionDefinition) null);
  }

  public <T> T runInTransaction(TransactionCallback<T> action, @Nullable TransactionDefinition definition) throws TransactionException {
    PlatformTransactionManager transactionManager = getTransactionManager();
    Assert.state(transactionManager != null, "No PlatformTransactionManager set");

    if (transactionManager instanceof CallbackPreferringPlatformTransactionManager) {
      return ((CallbackPreferringPlatformTransactionManager) transactionManager).execute(definition, action);
    }
    else {
      TransactionStatus status = transactionManager.getTransaction(definition);
      T result;
      try {
        result = action.doInTransaction(status);
      }
      catch (RuntimeException | Error ex) {
        // Transactional code threw application exception -> rollback
        rollbackOnException(transactionManager, status, ex);
        throw ex;
      }
      catch (Throwable ex) {
        // Transactional code threw unexpected exception -> rollback
        rollbackOnException(transactionManager, status, ex);
        throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");
      }
      transactionManager.commit(status);
      return result;
    }
  }

  /**
   * Perform a rollback, handling rollback exceptions properly.
   *
   * @param transactionManager PlatformTransactionManager
   * @param status object representing the transaction
   * @param ex the thrown application exception or error
   * @throws TransactionException in case of a rollback error
   */
  private void rollbackOnException(
          PlatformTransactionManager transactionManager, TransactionStatus status, Throwable ex) throws TransactionException {
    logger.debug("Initiating transaction rollback on application exception", ex);
    try {
      transactionManager.rollback(status);
    }
    catch (TransactionSystemException ex2) {
      logger.error("Application exception overridden by rollback exception", ex);
      ex2.initApplicationException(ex);
      throw ex2;
    }
    catch (RuntimeException | Error ex2) {
      logger.error("Application exception overridden by rollback exception", ex);
      throw ex2;
    }
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

  //

  public <T> TypeHandler<T> getTypeHandler(BeanProperty property) {
    return typeHandlerRegistry.getTypeHandler(property);
  }

  public <T> TypeHandler<T> getTypeHandler(Class<T> type) {
    return typeHandlerRegistry.getTypeHandler(type);
  }

}
