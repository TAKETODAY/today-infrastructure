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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.format.SqlStatementLogger;
import cn.taketoday.jdbc.result.DefaultResultSetHandlerFactory;
import cn.taketoday.jdbc.result.JdbcBeanMetadata;
import cn.taketoday.jdbc.result.LazyTable;
import cn.taketoday.jdbc.result.ResultSetHandler;
import cn.taketoday.jdbc.result.ResultSetHandlerFactory;
import cn.taketoday.jdbc.result.ResultSetHandlerIterator;
import cn.taketoday.jdbc.result.ResultSetIterable;
import cn.taketoday.jdbc.result.Row;
import cn.taketoday.jdbc.result.Table;
import cn.taketoday.jdbc.result.TableResultSetIterator;
import cn.taketoday.jdbc.result.TypeHandlerResultSetHandler;
import cn.taketoday.jdbc.type.ObjectTypeHandler;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/18 23:17
 */
public sealed abstract class AbstractQuery implements AutoCloseable permits NamedQuery, Query {
  private static final Logger log = LoggerFactory.getLogger(AbstractQuery.class);
  static final SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  private final JdbcConnection connection;

  @Nullable
  private final String[] columnNames;
  private final boolean returnGeneratedKeys;

  @Nullable
  private String name;

  private final String querySQL;
  private int maxBatchRecords = 0;
  private int currentBatchRecords = 0;

  private boolean caseSensitive;
  private boolean autoDerivingColumns;
  private boolean throwOnMappingFailure = true;

  @Nullable
  private PreparedStatement preparedStatement;

  @Nullable
  private TypeHandlerRegistry typeHandlerRegistry;

  @Nullable
  private Map<String, String> columnMappings;

  @Nullable
  private Map<String, String> caseSensitiveColumnMappings;

  @Nullable
  private StatementCallback statementCallback;

  @Nullable
  private BatchResult batchResult;

  public AbstractQuery(JdbcConnection connection, String querySQL, boolean generatedKeys) {
    this(connection, querySQL, generatedKeys, null);
  }

  public AbstractQuery(JdbcConnection connection, String querySQL, @Nullable String[] columnNames) {
    this(connection, querySQL, false, columnNames);
  }

  protected AbstractQuery(JdbcConnection connection, String querySQL, boolean generatedKeys, @Nullable String[] columnNames) {
    this.connection = connection;
    this.columnNames = columnNames;
    this.returnGeneratedKeys = generatedKeys;
    RepositoryManager manager = connection.getManager();
    setColumnMappings(manager.getDefaultColumnMappings());
    this.caseSensitive = manager.isDefaultCaseSensitive();
    this.querySQL = querySQL;
  }

  //---------------------------------------------------------------------
  // Getter/Setters
  //---------------------------------------------------------------------

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public AbstractQuery setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public boolean isAutoDerivingColumns() {
    return autoDerivingColumns;
  }

  public AbstractQuery setAutoDerivingColumns(boolean autoDerivingColumns) {
    this.autoDerivingColumns = autoDerivingColumns;
    return this;
  }

  public AbstractQuery throwOnMappingFailure(boolean throwOnMappingFailure) {
    this.throwOnMappingFailure = throwOnMappingFailure;
    return this;
  }

  public boolean isThrowOnMappingFailure() {
    return throwOnMappingFailure;
  }

  public JdbcConnection getConnection() {
    return this.connection;
  }

  public String getName() {
    return name;
  }

  public AbstractQuery setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public void close() {
    PreparedStatement prepared = this.preparedStatement;
    if (prepared != null) {
      connection.removeStatement(prepared);
      try {
        prepared.close();
      }
      catch (SQLException ex) {
        log.warn("Could not close statement.", ex);
      }
    }
  }

  //---------------------------------------------------------------------
  // Execute
  //---------------------------------------------------------------------

  protected PreparedStatement buildStatement() {
    return buildStatement(true);
  }

  /**
   * @return PreparedStatement
   * @throws ParameterBindFailedException parameter bind failed
   * @throws ArrayParameterBindFailedException array parameter bind failed
   */
  protected PreparedStatement buildStatement(boolean allowArrayParameters) {
    // prepare statement creation
    PreparedStatement statement = this.preparedStatement;
    if (statement == null) {
      JdbcConnection connection = getConnection();
      statement = preparedStatement(connection.getJdbcConnection(), allowArrayParameters);
      this.preparedStatement = statement; // update
      connection.registerStatement(statement);
    }

    postProcessStatement(statement);

    if (statementCallback != null) {
      statementCallback.doWith(statement);
    }

    // the parameters need to be cleared, so in case of batch,
    // only new parameters will be added
    return statement;
  }

  protected void postProcessStatement(PreparedStatement statement) {

  }

  private PreparedStatement preparedStatement(Connection connection, boolean allowArrayParameters) {
    try {
      String querySQL = getQuerySQL(allowArrayParameters);
      if (ObjectUtils.isNotEmpty(columnNames)) {
        return connection.prepareStatement(querySQL, columnNames);
      }
      if (returnGeneratedKeys) {
        return connection.prepareStatement(querySQL, Statement.RETURN_GENERATED_KEYS);
      }
      return connection.prepareStatement(querySQL);
    }
    catch (SQLException ex) {
      throw translateException("Preparing statement", ex);
    }
  }

  /**
   * add a Statement processor when {@link  #buildStatement() build a PreparedStatement}
   */
  public AbstractQuery processStatement(StatementCallback callback) {
    statementCallback = callback;
    return this;
  }

  protected String getQuerySQL(boolean allowArrayParameters) {
    return querySQL;
  }

  /**
   * Read a collection lazily. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterable#close()} when
   * you are done iterating.
   *
   * @param returnType type of each row
   * @return iterable results
   */
  public <T> ResultSetIterable<T> fetchIterable(Class<T> returnType) {
    return fetchIterable(createHandlerFactory(returnType));
  }

  public <T> ResultSetHandlerFactory<T> createHandlerFactory(Class<T> returnType) {
    return new DefaultResultSetHandlerFactory<>(
            new JdbcBeanMetadata(returnType, caseSensitive, autoDerivingColumns, throwOnMappingFailure),
            connection.getManager(), getColumnMappings());
  }

  /**
   * Read a collection lazily. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterable#close()} when
   * you are done iterating.
   *
   * @param factory factory to provide ResultSetHandler
   * @return iterable results
   */
  public <T> ResultSetIterable<T> fetchIterable(ResultSetHandlerFactory<T> factory) {
    final class FactoryResultSetIterable extends AbstractQuery.AbstractResultSetIterable<T> {
      @Override
      public Iterator<T> iterator() {
        return new ResultSetHandlerIterator<>(rs, factory);
      }
    }
    return new FactoryResultSetIterable();
  }

  /**
   * Read a collection lazily. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterable#close()} when
   * you are done iterating.
   *
   * @param handler ResultSetHandler
   * @return iterable results
   */
  public <T> ResultSetIterable<T> fetchIterable(ResultSetHandler<T> handler) {
    final class HandlerResultSetIterable extends AbstractResultSetIterable<T> {
      @Override
      public Iterator<T> iterator() {
        return new ResultSetHandlerIterator<>(rs, handler);
      }
    }
    return new HandlerResultSetIterable();
  }

  /**
   * Read a collection of T.
   *
   * @param returnType returnType
   * @return iterable results
   */
  public <T> List<T> fetch(Class<T> returnType) {
    return fetch(createHandlerFactory(returnType));
  }

  public <T> List<T> fetch(ResultSetHandler<T> handler) {
    try (ResultSetIterable<T> iterable = fetchIterable(handler)) {
      return fetch(iterable);
    }
  }

  public <T> List<T> fetch(ResultSetHandlerFactory<T> factory) {
    try (ResultSetIterable<T> iterable = fetchIterable(factory)) {
      return fetch(iterable);
    }
  }

  public <T> List<T> fetch(ResultSetIterable<T> iterable) {
    ArrayList<T> list = new ArrayList<>();
    for (T item : iterable) {
      list.add(item);
    }
    return list;
  }

  @Nullable
  public <T> T fetchFirst(Class<T> returnType) {
    return fetchFirst(createHandlerFactory(returnType));
  }

  @Nullable
  public <T> T fetchFirst(ResultSetHandler<T> handler) {
    try (ResultSetIterable<T> iterable = fetchIterable(handler)) {
      return fetchFirst(iterable);
    }
  }

  @Nullable
  public <T> T fetchFirst(ResultSetHandlerFactory<T> factory) {
    try (ResultSetIterable<T> iterable = fetchIterable(factory)) {
      return fetchFirst(iterable);
    }
  }

  @Nullable
  public <T> T fetchFirst(ResultSetIterable<T> iterable) {
    Iterator<T> iterator = iterable.iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }

  public LazyTable fetchLazyTable() {
    return fetchLazyTable(connection.getManager().getConversionService());
  }

  public LazyTable fetchLazyTable(@Nullable ConversionService conversionService) {
    LazyTable lt = new LazyTable();
    final class RowResultSetIterable extends AbstractResultSetIterable<Row> {
      @Override
      public Iterator<Row> iterator() {
        return new TableResultSetIterator(rs, isCaseSensitive(), lt, conversionService);
      }
    }
    lt.setRows(new RowResultSetIterable());
    return lt;
  }

  public Table fetchTable() {
    return fetchTable(connection.getManager().getConversionService());
  }

  public Table fetchTable(ConversionService conversionService) {
    ArrayList<Row> rows = new ArrayList<>();
    try (LazyTable lt = fetchLazyTable(conversionService)) {
      for (Row item : lt.rows()) {
        rows.add(item);
      }
      // lt==null is always false
      return new Table(lt.getName(), rows, lt.columns());
    }
  }

  /**
   * @see PreparedStatement#executeUpdate()
   */
  public <T> UpdateResult<T> executeUpdate() {
    return executeUpdate(returnGeneratedKeys);
  }

  /**
   * @see PreparedStatement#executeUpdate()
   */
  @SuppressWarnings("unchecked")
  public <T> UpdateResult<T> executeUpdate(boolean generatedKeys) {
    return (UpdateResult<T>) executeUpdate(generatedKeys ? ObjectTypeHandler.getSharedInstance() : null);
  }

  /**
   * @param generatedKeyHandler {@link PreparedStatement#getGeneratedKeys()} value getter
   * @see PreparedStatement#executeUpdate()
   */
  public <T> UpdateResult<T> executeUpdate(@Nullable TypeHandler<T> generatedKeyHandler) {
    logStatement();
    long start = System.currentTimeMillis();
    PreparedStatement statement = buildStatement();
    try {
      var ret = new UpdateResult<T>(statement.executeUpdate(), connection);

      if (generatedKeyHandler != null) {
        ret.setKeys(statement.getGeneratedKeys(), generatedKeyHandler);
      }

      if (log.isDebugEnabled()) {
        log.debug("total: {} ms; executed update [{}]", System.currentTimeMillis() - start, obtainName());
      }
      return ret;
    }
    catch (SQLException ex) {
      connection.onException();
      throw translateException("Execute update", ex);
    }
    finally {
      closeConnectionIfNecessary();
    }
  }

  public TypeHandlerRegistry getTypeHandlerRegistry() {
    TypeHandlerRegistry ret = this.typeHandlerRegistry;
    if (ret == null) {
      ret = this.connection.getManager().getTypeHandlerRegistry();
      this.typeHandlerRegistry = ret;
    }
    return ret;
  }

  public boolean isReturnGeneratedKeys() {
    return returnGeneratedKeys;
  }

  public void setTypeHandlerRegistry(@Nullable TypeHandlerRegistry typeHandlerRegistry) {
    this.typeHandlerRegistry = typeHandlerRegistry;
  }

  public Object fetchScalar() {
    return fetchScalar(ObjectTypeHandler.getSharedInstance());
  }

  public <V> V fetchScalar(Class<V> returnType) {
    return fetchScalar(getTypeHandlerRegistry().getTypeHandler(returnType));
  }

  public <T> List<T> fetchScalars(Class<T> returnType) {
    TypeHandler<T> typeHandler = getTypeHandlerRegistry().getTypeHandler(returnType);
    return fetch(new TypeHandlerResultSetHandler<>(typeHandler));
  }

  public <T> T fetchScalar(TypeHandler<T> typeHandler) {
    logStatement();
    long start = System.currentTimeMillis();
    try (PreparedStatement ps = buildStatement();
            ResultSet rs = ps.executeQuery()) {

      if (rs.next()) {
        T ret = typeHandler.getResult(rs, 1);
        if (log.isDebugEnabled()) {
          log.debug("total: {} ms; executed scalar [{}]", System.currentTimeMillis() - start, obtainName());
        }
        return ret;
      }
      else {
        return null;
      }
    }
    catch (SQLException e) {
      connection.onException();
      throw translateException("Execute scalar", e);
    }
    finally {
      closeConnectionIfNecessary();
    }
  }

  private String obtainName() {
    return name == null ? "No name" : name;
  }

  //---------------------------------------------------------------------
  // batch stuff
  //---------------------------------------------------------------------

  /**
   * Sets the number of batched commands this Query allows to be added before
   * implicitly calling <code>executeBatch()</code> from
   * <code>addToBatch()</code>. <br/>
   *
   * When set to 0, executeBatch is not called implicitly. This is the default
   * behaviour. <br/>
   *
   * When using this, please take care about calling <code>executeBatch()</code>
   * after finished adding all commands to the batch because commands may remain
   * unexecuted after the last <code>addToBatch()</code> call. Additionally, if
   * fetchGeneratedKeys is set, then previously generated keys will be lost after
   * a batch is executed.
   *
   * @throws IllegalArgumentException Thrown if the value is negative.
   */
  public AbstractQuery setMaxBatchRecords(int maxBatchRecords) {
    if (maxBatchRecords < 0) {
      throw new IllegalArgumentException("maxBatchRecords should be a nonnegative value");
    }
    this.maxBatchRecords = maxBatchRecords;
    return this;
  }

  public int getMaxBatchRecords() {
    return this.maxBatchRecords;
  }

  /**
   * @return The current number of unexecuted batched statements
   */
  public int getCurrentBatchRecords() {
    return this.currentBatchRecords;
  }

  /**
   * @return True if maxBatchRecords is set and there are unexecuted batched
   * commands or maxBatchRecords is not set
   */
  public boolean isExplicitExecuteBatchRequired() {
    return (this.maxBatchRecords > 0 && this.currentBatchRecords > 0) || (this.maxBatchRecords == 0);
  }

  /**
   * Adds a set of parameters to this <code>Query</code> object's batch of
   * commands. <br/>
   *
   * If maxBatchRecords is more than 0, executeBatch is called upon adding that
   * many commands to the batch. <br/>
   *
   * The current number of batched commands is accessible via the
   * <code>getCurrentBatchRecords()</code> method.
   */
  public AbstractQuery addToBatch() {
    try {
      buildStatement(false).addBatch();
      if (this.maxBatchRecords > 0
              && ++this.currentBatchRecords % this.maxBatchRecords == 0) {
        executeBatch();
      }
    }
    catch (SQLException e) {
      throw translateException("Adding statement to batch", e);
    }
    return this;
  }

  /**
   * Adds a set of parameters to this <code>Query</code> object's batch of
   * commands and returns any generated keys. <br/>
   *
   * If maxBatchRecords is more than 0, executeBatch is called upon adding that
   * many commands to the batch. This method will return any generated keys if
   * <code>fetchGeneratedKeys</code> is set. <br/>
   *
   * The current number of batched commands is accessible via the
   * <code>getCurrentBatchRecords()</code> method.
   */
  public <A> List<A> addToBatchGetKeys(Class<A> klass) {
    addToBatch();
    BatchResult batchResult = this.batchResult;
    if (batchResult != null) {
      return batchResult.getKeys(klass);
    }
    else {
      return Collections.emptyList();
    }
  }

  /**
   * @see PreparedStatement#executeBatch()
   */
  public BatchResult executeBatch() {
    return executeBatch(returnGeneratedKeys);
  }

  /**
   * @see PreparedStatement#executeBatch()
   */
  public BatchResult executeBatch(boolean generatedKeys) {
    logStatement();
    long start = System.currentTimeMillis();
    try {
      PreparedStatement statement = buildStatement();

      BatchResult batchResult = this.batchResult;
      if (batchResult == null) {
        batchResult = new BatchResult(connection);
        this.batchResult = batchResult;
      }
      batchResult.setBatchResult(statement.executeBatch());
      try {
        if (generatedKeys) {
          batchResult.addKeys(statement.getGeneratedKeys());
        }

        if (log.isDebugEnabled()) {
          log.debug("total: {} ms; executed batch [{}]", System.currentTimeMillis() - start, obtainName());
        }
        // reset currentBatchRecords to 0
        this.currentBatchRecords = 0;
        return batchResult;
      }
      catch (SQLException e) {
        throw new GeneratedKeysException(
                "Error while trying to fetch generated keys from database. " +
                        "If you are not expecting any generated keys, fix this" +
                        " error by setting the fetchGeneratedKeys parameter in" +
                        " the createQuery() method to 'false'", e);
      }
    }
    catch (SQLException e) {
      connection.onException();
      throw translateException("Executing batch operation", e);
    }
    finally {
      closeConnectionIfNecessary();
    }
  }

  public <T> BatchResult executeBatch(@Nullable TypeHandler<T> handler) {
    logStatement();
    long start = System.currentTimeMillis();
    try {
      PreparedStatement statement = buildStatement();

      BatchResult batchResult = this.batchResult;
      if (batchResult == null) {
        batchResult = new BatchResult(connection);
        this.batchResult = batchResult;
      }
      batchResult.setBatchResult(statement.executeBatch());
      try {
        if (handler != null) {
          batchResult.addKeys(statement.getGeneratedKeys(), handler);
        }

        if (log.isDebugEnabled()) {
          log.debug("total: {} ms; executed batch [{}]", System.currentTimeMillis() - start, obtainName());
        }
        // reset currentBatchRecords to 0
        this.currentBatchRecords = 0;
        return batchResult;
      }
      catch (SQLException e) {
        throw new GeneratedKeysException(
                "Error while trying to fetch generated keys from database. " +
                        "If you are not expecting any generated keys, fix this" +
                        " error by setting the fetchGeneratedKeys parameter in" +
                        " the createQuery() method to 'false'", e);
      }
    }
    catch (SQLException e) {
      connection.onException();
      throw translateException("Executing batch operation", e);
    }
    finally {
      closeConnectionIfNecessary();
    }
  }

  //---------------------------------------------------------------------
  // column mapping
  //---------------------------------------------------------------------

  @Nullable
  public Map<String, String> getColumnMappings() {
    if (isCaseSensitive()) {
      return caseSensitiveColumnMappings;
    }
    else {
      return columnMappings;
    }
  }

  /**
   * set the map of column-mappings
   * <p>
   * if input {@code mappings} is {@code null} reset the
   * {@link #columnMappings} and {@link #caseSensitiveColumnMappings}
   *
   * @param mappings column-mappings
   */
  public void setColumnMappings(@Nullable Map<String, String> mappings) {
    if (CollectionUtils.isNotEmpty(mappings)) {
      HashMap<String, String> columnMappings = new HashMap<>();
      HashMap<String, String> caseSensitiveColumnMappings = new HashMap<>();
      for (Map.Entry<String, String> entry : mappings.entrySet()) {
        caseSensitiveColumnMappings.put(entry.getKey(), entry.getValue());
        columnMappings.put(entry.getKey().toLowerCase(), entry.getValue().toLowerCase());
      }
      this.columnMappings = columnMappings;
      this.caseSensitiveColumnMappings = caseSensitiveColumnMappings;
    }
    else {
      this.columnMappings = null;
      this.caseSensitiveColumnMappings = null;
    }
  }

  public AbstractQuery addColumnMapping(String columnName, String propertyName) {
    if (columnMappings == null) {
      this.columnMappings = new HashMap<>();
    }
    if (caseSensitiveColumnMappings == null) {
      this.caseSensitiveColumnMappings = new HashMap<>();
    }
    caseSensitiveColumnMappings.put(columnName, propertyName);
    columnMappings.put(columnName.toLowerCase(), propertyName.toLowerCase());
    return this;
  }

  //---------------------------------------------------------------------
  // private stuff
  //---------------------------------------------------------------------

  private void closeConnectionIfNecessary() {
    if (connection.autoClose) {
      connection.close();
    }
  }

  private void logStatement() {
    if (stmtLogger.isDebugEnabled()) {
      stmtLogger.logStatement(querySQL);
    }
  }

  @Override
  public String toString() {
    return querySQL;
  }

  protected DataAccessException translateException(String task, SQLException ex) {
    return this.connection.getManager().translateException(task, querySQL, ex);
  }

  /**
   * Iterable {@link ResultSet} that wraps {@link ResultSetHandlerIterator}.
   */
  private abstract class AbstractResultSetIterable<T> extends ResultSetIterable<T> {
    private final long start;
    private final long afterExecQuery;
    protected final ResultSet rs;

    AbstractResultSetIterable() {
      try {
        start = System.currentTimeMillis();
        logStatement();
        rs = buildStatement().executeQuery();
        afterExecQuery = System.currentTimeMillis();
      }
      catch (SQLException ex) {
        throw translateException("Execute query", ex);
      }
    }

    @Override
    public void close() {
      try {
        rs.close();
        if (log.isDebugEnabled()) {
          long afterClose = System.currentTimeMillis();
          log.debug("total: {} ms, execution: {} ms, reading and parsing: {} ms; executed [{}]",
                  afterClose - start, afterExecQuery - start,
                  afterClose - afterExecQuery, name);
        }
      }
      catch (SQLException ex) {
        if (connection.getManager().isCatchResourceCloseErrors()) {
          throw translateException("Closing ResultSet", ex);
        }
        else {
          log.error("ResultSet close failed", ex);
        }
      }
      finally {
        if (isAutoCloseConnection()) {
          connection.close();
        }
        else {
          closeConnectionIfNecessary();
        }
      }
    }

  }

}
