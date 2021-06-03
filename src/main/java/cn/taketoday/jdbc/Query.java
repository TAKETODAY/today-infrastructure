package cn.taketoday.jdbc;

import java.io.InputStream;
import java.sql.Array;
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

import cn.taketoday.context.logger.Logger;
import cn.taketoday.context.logger.LoggerFactory;
import cn.taketoday.context.utils.Assert;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.ObjectUtils;
import cn.taketoday.jdbc.data.LazyTable;
import cn.taketoday.jdbc.data.Row;
import cn.taketoday.jdbc.data.Table;
import cn.taketoday.jdbc.data.TableResultSetIterator;
import cn.taketoday.jdbc.reflection.ReadableProperty;
import cn.taketoday.jdbc.result.DefaultResultSetHandlerFactoryBuilder;
import cn.taketoday.jdbc.result.ResultSetHandler;
import cn.taketoday.jdbc.result.ResultSetHandlerFactory;
import cn.taketoday.jdbc.result.ResultSetHandlerFactoryBuilder;
import cn.taketoday.jdbc.result.ResultSetHandlerIterator;
import cn.taketoday.jdbc.type.ObjectTypeHandler;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.jdbc.type.TypeHandlerRegistry;
import cn.taketoday.jdbc.utils.JdbcUtils;

/**
 * Represents a sql2o statement. With sql2o, all statements are instances of the
 * Query class.
 */
public class Query implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(Query.class);

  private final JdbcConnection connection;
  private Map<String, String> caseSensitiveColumnMappings;
  private Map<String, String> columnMappings;
  private PreparedStatement preparedStatement = null;
  private boolean caseSensitive;
  private boolean autoDeriveColumnNames;
  private boolean throwOnMappingFailure = true;
  private String name;
  private final boolean returnGeneratedKeys;
  private final String[] columnNames;
  private final Map<String, List<Integer>> paramNameToIdxMap;
  private final Map<String, ParameterSetter> parameters;
  private String parsedQuery;
  private int maxBatchRecords = 0;
  private int currentBatchRecords = 0;

  private ResultSetHandlerFactoryBuilder resultSetHandlerFactoryBuilder;

  public Query(JdbcConnection connection, String queryText, boolean returnGeneratedKeys) {
    this(connection, queryText, returnGeneratedKeys, null);
  }

  public Query(JdbcConnection connection, String queryText, String[] columnNames) {
    this(connection, queryText, false, columnNames);
  }

  private Query(JdbcConnection connection, String queryText, boolean returnGeneratedKeys, String[] columnNames) {
    this.connection = connection;
    this.columnNames = columnNames;
    this.returnGeneratedKeys = returnGeneratedKeys;
    this.setColumnMappings(connection.getSession().getDefaultColumnMappings());
    this.caseSensitive = connection.getSession().isDefaultCaseSensitive();

    paramNameToIdxMap = new HashMap<>();
    parameters = new HashMap<>();

    parsedQuery = connection.getSession()
            .getParsingStrategy()
            .parseSql(queryText, paramNameToIdxMap);
  }

  // ------------------------------------------------
  // ------------- Getter/Setters -------------------
  // ------------------------------------------------

  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  public Query setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  public boolean isAutoDeriveColumnNames() {
    return autoDeriveColumnNames;
  }

  public Query setAutoDeriveColumnNames(boolean autoDeriveColumnNames) {
    this.autoDeriveColumnNames = autoDeriveColumnNames;
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

  public ResultSetHandlerFactoryBuilder getResultSetHandlerFactoryBuilder() {
    if (resultSetHandlerFactoryBuilder == null) {
      resultSetHandlerFactoryBuilder = new DefaultResultSetHandlerFactoryBuilder(getTypeHandlerRegistry());
    }
    return resultSetHandlerFactoryBuilder;
  }

  public void setResultSetHandlerFactoryBuilder(ResultSetHandlerFactoryBuilder resultSetHandlerFactoryBuilder) {
    this.resultSetHandlerFactoryBuilder = resultSetHandlerFactoryBuilder;
  }

  public Map<String, List<Integer>> getParamNameToIdxMap() {
    return paramNameToIdxMap;
  }

  // ------------------------------------------------
  // ------------- Add Parameters -------------------
  // ------------------------------------------------

  public void addParameter(String name, ParameterSetter parameterSetter) {
    if (!getParamNameToIdxMap().containsKey(name)) {
      throw new PersistenceException(
              "Failed to add parameter with name '"
                      + name + "'. No parameter with that name is declared in the sql.");
    }
    parameters.put(name, parameterSetter);
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

    final TypeHandler<T> typeHandler = getTypeHandlerRegistry().getTypeHandler(parameterClass);
    addParameter(name, new TypeHandlerParameterSetter<>(value, typeHandler));
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
  public Query addParameter(String name, Object value) {
    return value == null
           ? addNullParameter(name)
           : addParameter(name, (Class<Object>) value.getClass(), value);
  }

  public Query addNullParameter(String name) {
    addParameter(name, ParameterSetter.null_setter);
    return this;
  }

  public Query addParameter(String name, final InputStream value) {
    addParameter(name, new BinaryStreamParameterSetter(value));
    return this;
  }

  public Query addParameter(String name, final int value) {
    addParameter(name, (statement, paramIdx) -> statement.setInt(paramIdx, value));
    return this;
  }

  public Query addParameter(String name, final long value) {
    addParameter(name, (statement, paramIdx) -> statement.setLong(paramIdx, value));
    return this;
  }

  public Query addParameter(String name, final String value) {
    addParameter(name, new StringParameterSetter(value));
    return this;
  }

  public Query addParameter(String name, final Timestamp value) {
    addParameter(name, new TimestampParameterSetter(value));
    return this;
  }

  public Query addParameter(String name, final Time value) {
    addParameter(name, new TimeParameterSetter(value));
    return this;
  }

  public Query addParameter(String name, final boolean value) {
    addParameter(name, new BooleanParameterSetter(value));
    return this;
  }

  /**
   * Set an array parameter.<br>
   * For example: <pre>
   *     createQuery("SELECT * FROM user WHERE id IN(:ids)")
   *      .addParameter("ids", 4, 5, 6)
   *      .executeAndFetch(...)
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
   * @throws IllegalArgumentException
   *         if values parameter is null
   */
  public Query addParameters(String name, final Object... values) {
    addParameter(name, new ArrayParameterSetter(values));
    return this;
  }

  /**
   * Set an array parameter.<br>
   * See {@link #addParameters(String, Object...)} for details
   */
  public Query addParameter(String name, final Collection<?> values) {
    addParameter(name, new ArrayParameterSetter(values));
    return this;
  }

  public Query bind(final Object pojo) {
    Class<?> clazz = pojo.getClass();
    final Map<String, List<Integer>> paramNameToIdxMap = getParamNameToIdxMap();
    for (ReadableProperty property : ReadableProperty.readableProperties(clazz).values()) {
      try {
        if (paramNameToIdxMap.containsKey(property.name)) {
          addParameter(property.name, (Class<Object>) property.type, property.get(pojo));
        }
      }
      catch (IllegalArgumentException ex) {
        log.debug("Ignoring Illegal Arguments", ex);
      }
    }
    return this;
  }

  @Override
  public void close() {
    final PreparedStatement prepared = this.preparedStatement;
    if (prepared != null) {
      connection.removeStatement(prepared);
      try {
        JdbcUtils.close(prepared);
      }
      catch (SQLException ex) {
        log.warn("Could not close statement.", ex);
      }
    }
  }

  // ------------------------------------------------
  // -------------------- Execute -------------------
  // ------------------------------------------------

  // visible for testing
  PreparedStatement buildPreparedStatement() {
    return buildPreparedStatement(true);
  }

  private PreparedStatement buildPreparedStatement(boolean allowArrayParameters) {
    // array parameter handling
    final Map<String, List<Integer>> paramNameToIdxMap = this.paramNameToIdxMap;
    parsedQuery = ArrayParameters.updateQueryAndParametersIndexes(parsedQuery,
                                                                  paramNameToIdxMap,
                                                                  parameters,
                                                                  allowArrayParameters);

    // prepare statement creation
    PreparedStatement statement = this.preparedStatement;
    if (statement == null) {
      statement = getPreparedStatement(this.connection.getJdbcConnection());
      this.preparedStatement = statement; // update
      connection.registerStatement(statement);
    }

    // parameters assignation to query
    for (Map.Entry<String, ParameterSetter> parameter : parameters.entrySet()) {
      final ParameterSetter setter = parameter.getValue();
      try {
        for (final int paramIdx : paramNameToIdxMap.get(parameter.getKey())) {
          setter.setParameter(statement, paramIdx);
        }
      }
      catch (SQLException e) {
        throw new PersistenceException(
                String.format("Error adding parameter '%s' - %s",
                              parameter.getKey(), e.getMessage()), e);
      }
    }
    // the parameters need to be cleared, so in case of batch,
    // only new parameters will be added
    parameters.clear();
    return statement;
  }

  private PreparedStatement getPreparedStatement(final Connection connection) {
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
   * Iterable {@link ResultSet} that wraps {@link ResultSetHandlerIterator}.
   */
  private abstract class ResultSetIterableBase<T> implements ResultSetIterable<T> {
    private final long start;
    private final long afterExecQuery;
    protected ResultSet rs;

    boolean autoCloseConnection = false;

    public ResultSetIterableBase() {
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
        JdbcUtils.close(rs);
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
        if (this.isAutoCloseConnection()) {
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

  /**
   * Read a collection lazily. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterable#close()} when
   * you are done iterating.
   *
   * @param returnType
   *         type of each row
   *
   * @return iterable results
   */
  public <T> ResultSetIterable<T> executeAndFetchLazy(final Class<T> returnType) {
    final ResultSetHandlerFactory<T> handlerFactory = newResultSetHandlerFactory(returnType);
    return executeAndFetchLazy(handlerFactory);
  }

  private <T> ResultSetHandlerFactory<T> newResultSetHandlerFactory(Class<T> returnType) {
    ResultSetHandlerFactoryBuilder builder = getResultSetHandlerFactoryBuilder();
    builder.setAutoDeriveColumnNames(this.autoDeriveColumnNames);
    builder.setCaseSensitive(this.caseSensitive);
    builder.setColumnMappings(this.getColumnMappings());
    builder.throwOnMappingError(this.throwOnMappingFailure);
    return builder.newFactory(returnType);
  }

  /**
   * Read a collection lazily. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterable#close()} when
   * you are done iterating.
   *
   * @param resultSetHandlerFactory
   *         factory to provide ResultSetHandler
   *
   * @return iterable results
   */
  public <T> ResultSetIterable<T> executeAndFetchLazy(final ResultSetHandlerFactory<T> resultSetHandlerFactory) {
    return new ResultSetIterableBase<T>() {
      public Iterator<T> iterator() {
        return new ResultSetHandlerIterator<>(rs, resultSetHandlerFactory);
      }
    };
  }

  /**
   * Read a collection lazily. Generally speaking, this should only be used if you
   * are reading MANY results and keeping them all in a Collection would cause
   * memory issues. You MUST call {@link ResultSetIterable#close()} when
   * you are done iterating.
   *
   * @param resultSetHandler
   *         ResultSetHandler
   *
   * @return iterable results
   */
  public <T> ResultSetIterable<T> executeAndFetchLazy(final ResultSetHandler<T> resultSetHandler) {
    return new ResultSetIterableBase<T>() {
      public Iterator<T> iterator() {
        return new ResultSetHandlerIterator<>(rs, resultSetHandler);
      }
    };
  }

  public <T> List<T> executeAndFetch(Class<T> returnType) {
    return executeAndFetch(newResultSetHandlerFactory(returnType));
  }

  public <T> List<T> executeAndFetch(ResultSetHandler<T> handler) {
    try (ResultSetIterable<T> iterable = executeAndFetchLazy(handler)) {
      return executeAndFetch(iterable);
    }
  }

  public <T> List<T> executeAndFetch(ResultSetHandlerFactory<T> factory) {
    try (ResultSetIterable<T> iterable = executeAndFetchLazy(factory)) {
      return executeAndFetch(iterable);
    }
  }

  public <T> List<T> executeAndFetch(ResultSetIterable<T> iterable) {
    List<T> list = new ArrayList<>();
    for (T item : iterable) {
      list.add(item);
    }
    return list;
  }

  public <T> T executeAndFetchFirst(Class<T> returnType) {
    return executeAndFetchFirst(newResultSetHandlerFactory(returnType));
  }

  public <T> T executeAndFetchFirst(ResultSetHandler<T> handler) {
    try (ResultSetIterable<T> iterable = executeAndFetchLazy(handler)) {
      return executeAndFetchFirst(iterable);
    }
  }

  public <T> T executeAndFetchFirst(ResultSetHandlerFactory<T> factory) {
    try (ResultSetIterable<T> iterable = executeAndFetchLazy(factory)) {
      return executeAndFetchFirst(iterable);
    }
  }

  public <T> T executeAndFetchFirst(ResultSetIterable<T> iterable) {
    Iterator<T> iterator = iterable.iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }

  public LazyTable executeAndFetchTableLazy() {
    final LazyTable lt = new LazyTable();

    lt.setRows(new ResultSetIterableBase<Row>() {
      public Iterator<Row> iterator() {
        return new TableResultSetIterator(rs, isCaseSensitive(), lt);
      }
    });
    return lt;
  }

  public Table executeAndFetchTable() {
    List<Row> rows = new ArrayList<>();

    try (LazyTable lt = executeAndFetchTableLazy()) {
      for (Row item : lt.rows()) {
        rows.add(item);
      }
      // lt==null is always false
      return new Table(lt.getName(), rows, lt.columns());
    }
  }

  public JdbcConnection executeUpdate() {
    final long start = System.currentTimeMillis();
    try {
      logExecution();
      PreparedStatement statement = buildPreparedStatement();
      this.connection.setResult(statement.executeUpdate());
      this.connection.setKeys(this.returnGeneratedKeys ? statement.getGeneratedKeys() : null);
      connection.setCanGetKeys(this.returnGeneratedKeys);
    }
    catch (SQLException ex) {
      this.connection.onException();
      throw new PersistenceException("Error in executeUpdate, " + ex.getMessage(), ex);
    }
    finally {
      closeConnectionIfNecessary();
    }

    if (log.isDebugEnabled()) {
      long end = System.currentTimeMillis();
      log.debug("total: {} ms; executed update [{}]",
                end - start, this.getName() == null ? "No name" : this.getName());
    }
    return this.connection;
  }

  public Object executeScalar(TypeHandler<?> typeHandler) {
    long start = System.currentTimeMillis();

    logExecution();
    try (final PreparedStatement ps = buildPreparedStatement();
            final ResultSet rs = ps.executeQuery()) {

      if (rs.next()) {
        final Object ret = typeHandler.getResult(rs, 1);
        if (log.isDebugEnabled()) {
          log.debug("total: {} ms; executed scalar [{}]",
                    System.currentTimeMillis() - start, this.getName() == null ? "No name" : getName());
        }
        return ret;
      }
      else {
        return null;
      }
    }
    catch (SQLException e) {
      this.connection.onException();
      throw new PersistenceException(
              "Database error occurred while running executeScalar: " + e.getMessage(), e);
    }
    finally {
      closeConnectionIfNecessary();
    }
  }

  public Object executeScalar() {
    return executeScalar(ObjectTypeHandler.getSharedInstance());
  }

  public TypeHandlerRegistry getTypeHandlerRegistry() {
    return this.connection.getSession().getTypeHandlerRegistry();
  }

  public <V> V executeScalar(Class<V> returnType) {
    final Object source = executeScalar(getTypeHandlerRegistry().getTypeHandler(returnType));
    return ConvertUtils.convert(returnType, source);
  }

  public <T> List<T> executeScalarList(final Class<T> returnType) {
    return executeAndFetch(newScalarResultSetHandler(returnType));
  }

  private <T> ResultSetHandler<T> newScalarResultSetHandler(final Class<T> returnType) {
    final TypeHandler<T> typeHandler = getTypeHandlerRegistry().getTypeHandler(returnType);
    return new ResultSetHandler<T>() {
      public T handle(ResultSet resultSet) throws SQLException {
        return typeHandler.getResult(resultSet, 1);
      }
    };
  }

  /************** batch stuff *******************/

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
   * @throws IllegalArgumentException
   *         Thrown if the value is negative.
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
      if (this.maxBatchRecords > 0) {
        if (++this.currentBatchRecords % this.maxBatchRecords == 0) {
          this.executeBatch();
        }
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
    this.addToBatch();

    if (this.currentBatchRecords == 0) {
      return this.connection.getKeys(klass);
    }
    else {
      return Collections.emptyList();
    }
  }

  public JdbcConnection executeBatch() {
    long start = System.currentTimeMillis();
    try {
      logExecution();
      PreparedStatement statement = buildPreparedStatement();
      connection.setBatchResult(statement.executeBatch());
      this.currentBatchRecords = 0;
      try {
        connection.setKeys(this.returnGeneratedKeys ? statement.getGeneratedKeys() : null);
        connection.setCanGetKeys(this.returnGeneratedKeys);
      }
      catch (SQLException sqlex) {
        throw new PersistenceException(
                "Error while trying to fetch generated keys from database. If you are not expecting any generated keys, " +
                        "fix this error by setting the fetchGeneratedKeys parameter in the createQuery() method to 'false'",
                sqlex);
      }
    }
    catch (Throwable e) {
      this.connection.onException();
      throw new PersistenceException("Error while executing batch operation: " + e.getMessage(), e);
    }
    finally {
      closeConnectionIfNecessary();
    }
    if (log.isDebugEnabled()) {
      log.debug("total: {} ms; executed batch [{}]",
                System.currentTimeMillis() - start, this.getName() == null ? "No name" : this.getName());
    }
    return this.connection;
  }

  /*********** column mapping ****************/

  public Map<String, String> getColumnMappings() {
    if (this.isCaseSensitive()) {
      return this.caseSensitiveColumnMappings;
    }
    else {
      return this.columnMappings;
    }
  }

  public Query setColumnMappings(Map<String, String> mappings) {

    this.caseSensitiveColumnMappings = new HashMap<>();
    this.columnMappings = new HashMap<>();

    for (Map.Entry<String, String> entry : mappings.entrySet()) {
      this.caseSensitiveColumnMappings.put(entry.getKey(), entry.getValue());
      this.columnMappings.put(entry.getKey().toLowerCase(), entry.getValue().toLowerCase());
    }

    return this;
  }

  public Query addColumnMapping(String columnName, String propertyName) {
    this.caseSensitiveColumnMappings.put(columnName, propertyName);
    this.columnMappings.put(columnName.toLowerCase(), propertyName.toLowerCase());

    return this;
  }

  /************** private stuff ***************/

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
      log.debug("Executing query:{}{}", System.lineSeparator(), this.parsedQuery);
    }
  }

  // from http://stackoverflow.com/questions/5606338/cast-primitive-type-array-into-object-array-in-java
  static Object[] toObjectArray(Object val) {
    if (val instanceof Object[]) return (Object[]) val;
    int arrayLength = java.lang.reflect.Array.getLength(val);
    Object[] outputArray = new Object[arrayLength];
    for (int i = 0; i < arrayLength; ++i) {
      outputArray[i] = java.lang.reflect.Array.get(val, i);
    }
    return outputArray;
  }

  @Override
  public String toString() {
    return parsedQuery;
  }

  // ParameterSetter

  class ArrayParameterSetter implements ParameterSetter {
    final Object[] values;

    ArrayParameterSetter(Collection<?> values) {
      Assert.notNull(values, "Array parameter cannot be null");
      this.values = values.toArray();
    }

    ArrayParameterSetter(Object[] values) {
      Assert.notNull(values, "Array parameter cannot be null");
      this.values = values;
    }

    @Override
    public int getParameterCount() {
      return values.length;
    }

    @Override
    public void setParameter(final PreparedStatement statement, int paramIdx) throws SQLException {
      if (values.length == 0) {
        getTypeHandlerRegistry().getObjectTypeHandler()
                .setParameter(statement, paramIdx, null);
      }
      else {
        final TypeHandler<Object> typeHandler = getTypeHandlerRegistry().getUnknownTypeHandler();
        for (final Object value : values) {
          typeHandler.setParameter(statement, paramIdx++, value);
        }
      }
    }
  }

  // ValuedParameterSetter

  abstract static class ValuedParameterSetter<T> implements ParameterSetter {
    final T value;

    protected ValuedParameterSetter(T value) {
      this.value = value;
    }
  }

  static class BinaryStreamParameterSetter extends ValuedParameterSetter<InputStream> {

    BinaryStreamParameterSetter(InputStream value) {
      super(value);
    }

    @Override
    public void setParameter(PreparedStatement statement, int paramIdx) throws SQLException {
      statement.setBinaryStream(paramIdx, value);
    }
  }

  static class StringParameterSetter extends ValuedParameterSetter<String> {

    StringParameterSetter(String value) {
      super(value);
    }

    @Override
    public void setParameter(PreparedStatement statement, int paramIdx) throws SQLException {
      statement.setString(paramIdx, value);
    }
  }

  static class BooleanParameterSetter extends ValuedParameterSetter<Boolean> {

    BooleanParameterSetter(boolean value) {
      super(value);
    }

    @Override
    public void setParameter(PreparedStatement statement, int paramIdx) throws SQLException {
      statement.setBoolean(paramIdx, value);
    }
  }

  static class TimeParameterSetter extends ValuedParameterSetter<Time> {

    TimeParameterSetter(Time value) {
      super(value);
    }

    @Override
    public void setParameter(PreparedStatement statement, int paramIdx) throws SQLException {
      statement.setTime(paramIdx, value);
    }
  }

  static class TimestampParameterSetter extends ValuedParameterSetter<Timestamp> {

    TimestampParameterSetter(Timestamp value) {
      super(value);
    }

    @Override
    public void setParameter(PreparedStatement statement, int paramIdx) throws SQLException {
      statement.setTimestamp(paramIdx, value);
    }
  }

  static class TypeHandlerParameterSetter<T> extends ValuedParameterSetter<T> {

    final TypeHandler<T> typeHandler;

    TypeHandlerParameterSetter(T value, TypeHandler<T> typeHandler) {
      super(value);
      this.typeHandler = typeHandler;
    }

    @Override
    public void setParameter(PreparedStatement statement, int paramIdx) throws SQLException {
      typeHandler.setParameter(statement, paramIdx, value);
    }
  }

}
