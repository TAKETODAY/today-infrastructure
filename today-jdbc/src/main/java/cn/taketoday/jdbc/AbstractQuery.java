/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;

import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.dao.DataAccessException;
import cn.taketoday.jdbc.core.ResultSetExtractor;
import cn.taketoday.jdbc.format.SqlStatementLogger;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.type.ObjectTypeHandler;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerManager;
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
  private boolean autoDerivingColumns = true;
  private boolean throwOnMappingFailure = true;

  @Nullable
  private PreparedStatement preparedStatement;

  @Nullable
  private TypeHandlerManager typeHandlerManager;

  @Nullable
  private Map<String, String> columnMappings;

  @Nullable
  private Map<String, String> caseSensitiveColumnMappings;

  @Nullable
  private QueryStatementCallback statementCallback;

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

  private ResultSet executeQuery() {
    try {
      logStatement();
      return buildStatement().executeQuery();
    }
    catch (SQLException e) {
      throw translateException("Statement execute query", e);
    }
  }

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
      try {
        statementCallback.doWith(statement);
      }
      catch (SQLException ex) {
        throw translateException("User statement callback", ex);
      }
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
  public AbstractQuery processStatement(@Nullable QueryStatementCallback callback) {
    statementCallback = callback;
    return this;
  }

  protected String getQuerySQL(boolean allowArrayParameters) {
    return querySQL;
  }

  // -----------------------------------------------------------------------------------------------
  // Fetch data
  // -----------------------------------------------------------------------------------------------

  /**
   * Read a collection of T.
   *
   * @param returnType returnType
   * @return iterable results
   */
  public <T> List<T> fetch(Class<T> returnType) {
    return iterate(returnType).list();
  }

  /**
   * Read a collection of T.
   *
   * @return iterable results
   */
  public <T> List<T> fetch(ResultSetExtractor<T> handler) {
    return iterate(handler).list();
  }

  /**
   * Read a collection of T.
   *
   * @return iterable results
   */
  public <T> List<T> fetch(ResultSetHandlerFactory<T> factory) {
    return iterate(factory).list();
  }

  @Nullable
  public <T> T fetchFirst(Class<T> returnType) {
    return iterate(returnType).first();
  }

  @Nullable
  public <T> T fetchFirst(ResultSetExtractor<T> handler) {
    return iterate(handler).first();
  }

  @Nullable
  public <T> T fetchFirst(ResultSetHandlerFactory<T> factory) {
    return iterate(factory).first();
  }

  /**
   * Iterate Elements. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterator#close()} when
   * you are done iterating.
   *
   * @param returnType type of each row
   * @return iterable results
   */
  public <T> ResultSetIterator<T> iterate(Class<T> returnType) {
    return new ResultSetHandlerIterator<>(createHandlerFactory(returnType));
  }

  /**
   * Iterate Elements. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterator#close()} when
   * you are done iterating.
   *
   * @param handler ResultSetHandler
   * @return iterable results
   */
  public <T> ResultSetIterator<T> iterate(ResultSetExtractor<T> handler) {
    return new ResultSetHandlerIterator<>(handler);
  }

  /**
   * Iterate Elements. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterator#close()} when
   * you are done iterating.
   *
   * @param factory factory to provide ResultSetHandler
   * @return iterable results
   */
  public <T> ResultSetIterator<T> iterate(ResultSetHandlerFactory<T> factory) {
    return new ResultSetHandlerIterator<>(factory);
  }

  public <T> ResultSetHandlerFactory<T> createHandlerFactory(Class<T> returnType) {
    return new DefaultResultSetHandlerFactory<>(
            new JdbcBeanMetadata(returnType, caseSensitive, autoDerivingColumns, throwOnMappingFailure),
            connection.getManager(), getColumnMappings());
  }

  public LazyTable fetchLazyTable() {
    return fetchLazyTable(connection.getManager().getConversionService());
  }

  public LazyTable fetchLazyTable(@Nullable ConversionService conversionService) {
    LazyTable lt = new LazyTable();
    lt.setRows(new TableResultSetIterator(executeQuery(), isCaseSensitive(), lt, conversionService).asIterable());
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
    return (UpdateResult<T>) executeUpdate(generatedKeys ? ObjectTypeHandler.sharedInstance : null);
  }

  /**
   * @param generatedKeyHandler {@link PreparedStatement#getGeneratedKeys()} value getter
   * @see PreparedStatement#executeUpdate()
   */
  public <T> UpdateResult<T> executeUpdate(@Nullable TypeHandler<T> generatedKeyHandler) {
    logStatement();
    long start = System.currentTimeMillis();
    try {
      PreparedStatement statement = buildStatement();
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

  public TypeHandlerManager getTypeHandlerManager() {
    TypeHandlerManager ret = this.typeHandlerManager;
    if (ret == null) {
      ret = this.connection.getManager().getTypeHandlerManager();
      this.typeHandlerManager = ret;
    }
    return ret;
  }

  public boolean isReturnGeneratedKeys() {
    return returnGeneratedKeys;
  }

  public void setTypeHandlerManager(@Nullable TypeHandlerManager typeHandlerManager) {
    this.typeHandlerManager = typeHandlerManager;
  }

  public Object fetchScalar() {
    return fetchScalar(ObjectTypeHandler.sharedInstance);
  }

  public <V> V fetchScalar(Class<V> returnType) {
    return fetchScalar(getTypeHandlerManager().getTypeHandler(returnType));
  }

  public <T> List<T> fetchScalars(Class<T> returnType) {
    TypeHandler<T> typeHandler = getTypeHandlerManager().getTypeHandler(returnType);
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
   *
   * @since 4.0
   */
  private class ResultIterable<T> extends ResultSetIterable<T> {

    private final ResultSetIterator<T> iterator;

    private ResultIterable(ResultSetIterator<T> iterator) {
      this.iterator = iterator;
    }

    @Override
    public Iterator<T> iterator() {
      return iterator;
    }

    @Override
    public Spliterator<T> spliterator() {
      return iterator.spliterator();
    }

    @Override
    public void close() {
      try {
        iterator.close();
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

  /**
   * @since 4.0
   */
  abstract class CloseResultSetIterator<T> extends ResultSetIterator<T> {

    protected CloseResultSetIterator(ResultSet rs) {
      super(rs);
    }

    @Override
    public ResultSetIterable<T> asIterable() {
      return new ResultIterable<>(this);
    }

    @Override
    public void close() {
      try {
        resultSet.close();
      }
      catch (SQLException ex) {
        if (connection.getManager().isCatchResourceCloseErrors()) {
          throw translateException("Closing ResultSet", ex);
        }
        else {
          log.debug("ResultSet close failed", ex);
        }
      }
      finally {
        closeConnectionIfNecessary();
      }
    }
  }

  /**
   * @since 4.0
   */
  final class ResultSetHandlerIterator<T> extends CloseResultSetIterator<T> {
    private final ResultSetExtractor<T> handler;

    public ResultSetHandlerIterator(ResultSetExtractor<T> handler) {
      super(executeQuery());
      this.handler = handler;
    }

    public ResultSetHandlerIterator(ResultSetHandlerFactory<T> factory) {
      super(executeQuery());
      try {
        this.handler = factory.getResultSetHandler(resultSet.getMetaData());
      }
      catch (SQLException e) {
        throw translateException("Get ResultSetHandler", e);
      }
    }

    @Override
    protected T readNext(ResultSet resultSet) throws SQLException {
      return handler.extractData(resultSet);
    }

  }

  /**
   * @since 4.0
   */
  final class TableResultSetIterator extends CloseResultSetIterator<Row> {
    private final List<Column> columns;

    private final boolean isCaseSensitive;

    private final ConversionService conversionService;

    private final Map<String, Integer> columnNameToIdxMap;

    private TableResultSetIterator(ResultSet rs, boolean isCaseSensitive, LazyTable lt, @Nullable ConversionService conversionService) {
      super(rs);
      this.isCaseSensitive = isCaseSensitive;
      this.conversionService = conversionService == null ? DefaultConversionService.getSharedInstance() : conversionService;
      try {
        ResultSetMetaData meta = rs.getMetaData();
        ArrayList<Column> columns = new ArrayList<>();
        HashMap<String, Integer> columnNameToIdxMap = new HashMap<>();

        lt.setName(meta.getTableName(1));
        lt.setColumns(columns);

        int columnCount = meta.getColumnCount();
        for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
          String colName = JdbcUtils.lookupColumnName(meta, colIdx);
          String colType = meta.getColumnTypeName(colIdx);
          columns.add(new Column(colName, colIdx - 1, colType));

          String colMapName = isCaseSensitive ? colName : colName.toLowerCase();
          columnNameToIdxMap.put(colMapName, colIdx - 1);
        }
        this.columns = columns;
        this.columnNameToIdxMap = columnNameToIdxMap;
      }
      catch (SQLException e) {
        throw new PersistenceException("Error while reading metadata from database", e);
      }
    }

    @Override
    protected Row readNext(ResultSet resultSet) throws SQLException {
      final Row row = new Row(columnNameToIdxMap, columns.size(), isCaseSensitive, conversionService);
      for (Column column : columns) {
        final int index = column.getIndex();
        row.addValue(index, resultSet.getObject(index + 1));
      }
      return row;
    }

  }

}
