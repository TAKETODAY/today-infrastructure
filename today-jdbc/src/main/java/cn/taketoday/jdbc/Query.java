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

import java.io.InputStream;
import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.jdbc.parsing.QueryParameter;
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
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.ObjectUtils;

/**
 * Represents a sql statement.
 */
public final class Query implements AutoCloseable {
  private static final Logger log = LoggerFactory.getLogger(Query.class);

  private final JdbcConnection connection;

  private final String[] columnNames;
  private final boolean returnGeneratedKeys;

  /** parameter name to parameter index and setter */
  private final HashMap<String, QueryParameter> queryParameters = new HashMap<>();

  private String name;
  private String parsedQuery;
  private int maxBatchRecords = 0;
  private int currentBatchRecords = 0;

  private boolean caseSensitive;
  private boolean autoDerivingColumns;
  private boolean throwOnMappingFailure = true;
  private PreparedStatement preparedStatement = null;

  private TypeHandlerRegistry typeHandlerRegistry;

  private Map<String, String> columnMappings;
  private Map<String, String> caseSensitiveColumnMappings;

  private boolean hasArrayParameter = false;

  @Nullable
  private StatementCallback statementCallback;

  public Query(JdbcConnection connection, String queryText, boolean generatedKeys) {
    this(connection, queryText, generatedKeys, null);
  }

  public Query(JdbcConnection connection, String queryText, String[] columnNames) {
    this(connection, queryText, false, columnNames);
  }

  private Query(JdbcConnection connection, String queryText, boolean generatedKeys, String[] columnNames) {
    this.connection = connection;
    this.columnNames = columnNames;
    this.returnGeneratedKeys = generatedKeys;
    RepositoryManager manager = connection.getManager();
    setColumnMappings(manager.getDefaultColumnMappings());
    this.caseSensitive = manager.isDefaultCaseSensitive();
    this.parsedQuery = manager.parse(queryText, queryParameters);
  }

  //---------------------------------------------------------------------
  // Getter/Setters
  //---------------------------------------------------------------------

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public Query setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public boolean isAutoDerivingColumns() {
    return autoDerivingColumns;
  }

  public Query setAutoDerivingColumns(boolean autoDerivingColumns) {
    this.autoDerivingColumns = autoDerivingColumns;
    return this;
  }

  public Query throwOnMappingFailure(boolean throwOnMappingFailure) {
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

  public Query setName(String name) {
    this.name = name;
    return this;
  }

  public HashMap<String, QueryParameter> getQueryParameters() {
    return queryParameters;
  }

  //---------------------------------------------------------------------
  // Add Parameters
  //---------------------------------------------------------------------

  public void addParameter(String name, ParameterBinder parameterBinder) {
    QueryParameter queryParameter = queryParameters.get(name);
    if (queryParameter == null) {
      throw new PersistenceException(
              "Failed to add parameter with name '"
                      + name + "'. No parameter with that name is declared in the sql.");
    }
    queryParameter.setSetter(parameterBinder);
  }

  public <T> Query addParameter(String name, Class<T> parameterClass, T value) {
    if (parameterClass.isArray()
            // byte[] is used for blob already
            && parameterClass != byte[].class) {
      return addParameters(name, toObjectArray(value));
    }

    if (Collection.class.isAssignableFrom(parameterClass)) {
      return addParameter(name, (Collection<?>) value);
    }
    TypeHandler<T> typeHandler = getTypeHandlerRegistry().getTypeHandler(parameterClass);
    final class TypeHandlerParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        typeHandler.setParameter(statement, paramIdx, value);
      }
    }
    addParameter(name, new TypeHandlerParameterBinder());
    return this;
  }

  public Query withParams(Object... paramValues) {
    int i = 0;
    for (Object paramValue : paramValues) {
      addParameter("p" + (++i), paramValue);
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  public Query addParameter(String name, @Nullable Object value) {
    return value == null
           ? addNullParameter(name)
           : addParameter(name, (Class<Object>) value.getClass(), value);
  }

  public Query addNullParameter(String name) {
    addParameter(name, ParameterBinder.null_binder);
    return this;
  }

  public Query addParameter(String name, InputStream value) {
    final class BinaryStreamParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setBinaryStream(paramIdx, value);
      }
    }
    addParameter(name, new BinaryStreamParameterBinder());
    return this;
  }

  public Query addParameter(String name, int value) {
    final class IntegerParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setInt(paramIdx, value);
      }
    }
    addParameter(name, new IntegerParameterBinder());
    return this;
  }

  public Query addParameter(String name, long value) {
    final class LongParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setLong(paramIdx, value);
      }
    }
    addParameter(name, new LongParameterBinder());
    return this;
  }

  public Query addParameter(String name, String value) {
    final class StringParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setString(paramIdx, value);
      }
    }
    addParameter(name, new StringParameterBinder());
    return this;
  }

  public Query addParameter(String name, Timestamp value) {
    final class TimestampParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setTimestamp(paramIdx, value);
      }
    }
    addParameter(name, new TimestampParameterBinder());
    return this;
  }

  public Query addParameter(String name, Time value) {
    final class TimeParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setTime(paramIdx, value);
      }
    }

    addParameter(name, new TimeParameterBinder());
    return this;
  }

  public Query addParameter(String name, boolean value) {
    final class BooleanParameterBinder extends ParameterBinder {

      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        statement.setBoolean(paramIdx, value);
      }
    }
    addParameter(name, new BooleanParameterBinder());
    return this;
  }

  /**
   * Set an array parameter.<br>
   * For example: <pre>
   *     createQuery("SELECT * FROM user WHERE id IN(:ids)")
   *      .addParameter("ids", 4, 5, 6)
   *      .fetch(...)
   * </pre> will generate the query :
   * <code>SELECT * FROM user WHERE id IN(4,5,6)</code><br>
   * <br>
   * It is not possible to use array parameters with a batch
   * <code>PreparedStatement</code>: since the text query passed to the
   * <code>PreparedStatement</code> depends on the number of parameters in the
   * array, array parameters are incompatible with batch mode.<br>
   * <br>
   * If the values array is empty, <code>null</code> will be set to the array
   * parameter: <code>SELECT * FROM user WHERE id IN(NULL)</code>
   *
   * @throws IllegalArgumentException if values parameter is null
   */
  public Query addParameters(String name, Object... values) {
    addParameter(name, new ArrayParameterBinder(values));
    this.hasArrayParameter = true;
    return this;
  }

  /**
   * add map of parameters
   *
   * @param parameters map of parameters
   * @see #addParameter(String, Object)
   */
  public Query addParameters(Map<String, Object> parameters) {
    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      addParameter(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Set an array parameter.<br>
   * See {@link #addParameters(String, Object...)} for details
   */
  public Query addParameter(String name, Collection<?> values) {
    addParameter(name, new ArrayParameterBinder(values));
    this.hasArrayParameter = true;
    return this;
  }

  @SuppressWarnings("unchecked")
  public Query bind(Object pojo) {
    HashMap<String, QueryParameter> queryParameters = getQueryParameters();
    for (BeanProperty property : BeanMetadata.from(pojo)) {
      String name = property.getName();
      try {
        if (queryParameters.containsKey(name)) {
          addParameter(name, (Class<Object>) property.getType(), property.getValue(pojo));
        }
      }
      catch (IllegalArgumentException ex) {
        log.debug("Ignoring Illegal Arguments", ex);
      }
    }
    return this;
  }

  public void processStatement(StatementCallback callback) {
    statementCallback = callback;
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

  // visible for testing
  PreparedStatement buildPreparedStatement() {
    return buildPreparedStatement(true);
  }

  /**
   * @return PreparedStatement
   * @throws ParameterBindFailedException parameter bind failed
   * @throws ArrayParameterBindFailedException array parameter bind failed
   */
  private PreparedStatement buildPreparedStatement(boolean allowArrayParameters) {
    HashMap<String, QueryParameter> queryParameters = getQueryParameters();
    if (hasArrayParameter) {
      // array parameter handling
      this.parsedQuery = ArrayParameters.updateQueryAndParametersIndexes(
              parsedQuery,
              queryParameters,
              allowArrayParameters
      );
    }

    // prepare statement creation
    PreparedStatement statement = this.preparedStatement;
    if (statement == null) {
      JdbcConnection connection = getConnection();
      statement = getPreparedStatement(connection.getJdbcConnection());
      this.preparedStatement = statement; // update
      connection.registerStatement(statement);
    }

    // parameters assignation to query
    for (QueryParameter parameter : queryParameters.values()) {
      try {
        parameter.setTo(statement);
      }
      catch (SQLException e) {
        throw new ParameterBindFailedException(
                "Error binding parameter '" + parameter.getName() + "' - " + e.getMessage(), e);
      }
    }

    if (statementCallback != null) {
      statementCallback.doWith(statement);
    }

    // the parameters need to be cleared, so in case of batch,
    // only new parameters will be added
//    parameters.clear(); TODO clear queryParameters setter
    return statement;
  }

  private PreparedStatement getPreparedStatement(Connection connection) {
    try {
      if (ObjectUtils.isNotEmpty(columnNames)) {
        return connection.prepareStatement(parsedQuery, columnNames);
      }
      if (returnGeneratedKeys) {
        return connection.prepareStatement(parsedQuery, Statement.RETURN_GENERATED_KEYS);
      }
      return connection.prepareStatement(parsedQuery);
    }
    catch (SQLException ex) {
      throw new PersistenceException("Error preparing statement - " + ex.getMessage(), ex);
    }
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
    final class FactoryResultSetIterable extends AbstractResultSetIterable<T> {
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

  public <T> T fetchFirst(Class<T> returnType) {
    return fetchFirst(createHandlerFactory(returnType));
  }

  public <T> T fetchFirst(ResultSetHandler<T> handler) {
    try (ResultSetIterable<T> iterable = fetchIterable(handler)) {
      return fetchFirst(iterable);
    }
  }

  public <T> T fetchFirst(ResultSetHandlerFactory<T> factory) {
    try (ResultSetIterable<T> iterable = fetchIterable(factory)) {
      return fetchFirst(iterable);
    }
  }

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

  public JdbcConnection executeUpdate() {
    long start = System.currentTimeMillis();
    JdbcConnection connection = getConnection();
    try {
      logExecution();
      PreparedStatement statement = buildPreparedStatement();
      connection.setResult(statement.executeUpdate());
      boolean generatedKeys = isReturnGeneratedKeys();
      connection.setKeys(generatedKeys ? statement.getGeneratedKeys() : null);
      connection.setCanGetKeys(generatedKeys);
    }
    catch (SQLException ex) {
      connection.onException();
      throw new PersistenceException("Error in executeUpdate, " + ex.getMessage(), ex);
    }
    finally {
      closeConnectionIfNecessary();
    }

    if (log.isDebugEnabled()) {
      log.debug("total: {} ms; executed update [{}]", System.currentTimeMillis() - start, obtainName());
    }
    return connection;
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

  public void setTypeHandlerRegistry(TypeHandlerRegistry typeHandlerRegistry) {
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
    logExecution();
    long start = System.currentTimeMillis();
    try (PreparedStatement ps = buildPreparedStatement();
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
      throw new PersistenceException(
              "Database error occurred while running executeScalar: " + e.getMessage(), e);
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
  public Query setMaxBatchRecords(int maxBatchRecords) {
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
  public Query addToBatch() {
    try {
      buildPreparedStatement(false).addBatch();
      if (this.maxBatchRecords > 0
              && ++this.currentBatchRecords % this.maxBatchRecords == 0) {
        executeBatch();
      }
    }
    catch (SQLException e) {
      throw new PersistenceException("Error while adding statement to batch", e);
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
    if (this.currentBatchRecords == 0) {
      return connection.getKeys(klass);
    }
    else {
      return Collections.emptyList();
    }
  }

  public JdbcConnection executeBatch() {
    logExecution();
    long start = System.currentTimeMillis();
    JdbcConnection connection = this.connection;
    try {
      PreparedStatement statement = buildPreparedStatement();
      connection.setBatchResult(statement.executeBatch());
      this.currentBatchRecords = 0;
      try {
        connection.setKeys(returnGeneratedKeys ? statement.getGeneratedKeys() : null);
        connection.setCanGetKeys(returnGeneratedKeys);
      }
      catch (SQLException e) {
        throw new PersistenceException(
                "Error while trying to fetch generated keys from database. " +
                        "If you are not expecting any generated keys, fix this" +
                        " error by setting the fetchGeneratedKeys parameter in" +
                        " the createQuery() method to 'false'", e);
      }
    }
    catch (Throwable e) {
      connection.onException();
      throw new PersistenceException("Error while executing batch operation: " + e.getMessage(), e);
    }
    finally {
      closeConnectionIfNecessary();
    }
    if (log.isDebugEnabled()) {
      log.debug("total: {} ms; executed batch [{}]", System.currentTimeMillis() - start, obtainName());
    }
    return connection;
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

  public Query addColumnMapping(String columnName, String propertyName) {
    if (columnMappings == null) {
      this.columnMappings = new HashMap<>();
      this.caseSensitiveColumnMappings = new HashMap<>();
    }
    this.caseSensitiveColumnMappings.put(columnName, propertyName);
    this.columnMappings.put(columnName.toLowerCase(), propertyName.toLowerCase());
    return this;
  }

  //---------------------------------------------------------------------
  // private stuff
  //---------------------------------------------------------------------

  private void closeConnectionIfNecessary() {
    try {
      if (connection.autoClose) {
        connection.close();
      }
    }
    catch (Exception ex) {
      throw new PersistenceException("Error while attempting to close connection", ex);
    }
  }

  private void logExecution() {
    if (log.isDebugEnabled()) {
      log.debug("Executing query:{}{}", System.lineSeparator(), parsedQuery);
    }
  }

  // from http://stackoverflow.com/questions/5606338/cast-primitive-type-array-into-object-array-in-java
  private static Object[] toObjectArray(Object val) {
    if (val instanceof Object[]) {
      return (Object[]) val;
    }
    int arrayLength = Array.getLength(val);
    Object[] outputArray = new Object[arrayLength];
    for (int i = 0; i < arrayLength; ++i) {
      outputArray[i] = Array.get(val, i);
    }
    return outputArray;
  }

  @Override
  public String toString() {
    return parsedQuery;
  }

  //---------------------------------------------------------------------
  // ParameterSetter
  //---------------------------------------------------------------------

  final class ArrayParameterBinder extends ParameterBinder {
    final Object[] values;

    ArrayParameterBinder(Collection<?> values) {
      Assert.notNull(values, "Array parameter cannot be null");
      this.values = values.toArray();
    }

    ArrayParameterBinder(Object[] values) {
      Assert.notNull(values, "Array parameter cannot be null");
      this.values = values;
    }

    public int getParameterCount() {
      return values.length;
    }

    @Override
    public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
      if (values.length == 0) {
        getTypeHandlerRegistry().getObjectTypeHandler()
                .setParameter(statement, paramIdx, null);
      }
      else {
        TypeHandler<Object> typeHandler = getTypeHandlerRegistry().getUnknownTypeHandler();
        for (Object value : values) {
          typeHandler.setParameter(statement, paramIdx++, value);
        }
      }
    }
  }

  /**
   * Iterable {@link ResultSet} that wraps {@link ResultSetHandlerIterator}.
   */
  private abstract class AbstractResultSetIterable<T> implements ResultSetIterable<T> {
    private final long start;
    private final long afterExecQuery;
    protected final ResultSet rs;

    boolean autoCloseConnection = false;

    AbstractResultSetIterable() {
      try {
        start = System.currentTimeMillis();
        logExecution();
        rs = buildPreparedStatement().executeQuery();
        afterExecQuery = System.currentTimeMillis();
      }
      catch (SQLException ex) {
        throw new PersistenceException("Database error: " + ex.getMessage(), ex);
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
        throw new PersistenceException("Error closing ResultSet.", ex);
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

    @Override
    public boolean isAutoCloseConnection() {
      return this.autoCloseConnection;
    }

    @Override
    public void setAutoCloseConnection(boolean autoCloseConnection) {
      this.autoCloseConnection = autoCloseConnection;
    }
  }

}
