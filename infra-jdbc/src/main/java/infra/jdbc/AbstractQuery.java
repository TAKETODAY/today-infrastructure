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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Spliterator;

import infra.core.conversion.ConversionService;
import infra.core.conversion.support.DefaultConversionService;
import infra.dao.DataAccessException;
import infra.jdbc.core.ResultSetExtractor;
import infra.jdbc.format.SqlStatementLogger;
import infra.jdbc.support.JdbcUtils;
import infra.jdbc.type.ObjectTypeHandler;
import infra.jdbc.type.TypeHandler;
import infra.jdbc.type.TypeHandlerManager;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.ObjectUtils;

/**
 * AbstractQuery serves as the base class for query execution in a JDBC environment.
 * It provides methods to configure, execute, and process SQL queries. The class supports
 * various operations such as fetching results, iterating over large datasets, executing
 * updates, and managing batch operations. Additionally, it allows customization of query
 * behavior through configuration options like case sensitivity, auto-deriving columns,
 * and exception handling.
 *
 * <p>Example usage:
 * <pre>{@code
 *   // Create a connection (assuming JdbcConnection is already implemented)
 *   JdbcConnection connection =...;
 *
 *   // Create an AbstractQuery instance
 *   AbstractQuery query = ...;
 *
 *   // Set query parameters
 *   query.processStatement(stmt -> stmt.setInt(1, 1));
 *
 *   // Fetch results as a list of User objects
 *   List<User> users = query.fetch(User.class);
 *
 *   // Iterate through the results
 *   for (User user : users) {
 *     System.out.println(user.getName());
 *   }
 *
 *   // Close the query to release resources
 *   query.close();
 * }</pre>
 *
 * <p>Key Features:
 * <ul>
 *   <li>Supports fetching results into custom objects or scalar values.</li>
 *   <li>Provides lazy and memory-efficient iteration for large result sets.</li>
 *   <li>Handles batch operations with configurable limits.</li>
 *   <li>Allows customization of column mappings and type handlers.</li>
 *   <li>Integrates with JDBC's PreparedStatement for parameter binding.</li>
 * </ul>
 *
 * <p>Configuration Options:
 * <ul>
 *   <li>{@link #setCaseSensitive(boolean)}: Enables or disables case-sensitive column mapping.</li>
 *   <li>{@link #setAutoDerivingColumns(boolean)}: Automatically maps database columns to object fields.</li>
 *   <li>{@link #throwOnMappingFailure(boolean)}: Controls whether exceptions are thrown on mapping failures.</li>
 *   <li>{@link #setMaxBatchRecords(int)}: Configures the maximum number of records per batch.</li>
 * </ul>
 *
 * <p>Resource Management:
 * <ul>
 *   <li>Implements {@link AutoCloseable} to ensure proper resource cleanup.</li>
 *   <li>Automatically closes the underlying PreparedStatement and ResultSet when necessary.</li>
 * </ul>
 *
 * <p>Thread Safety:
 * <ul>
 *   <li>Instances of this class are not thread-safe. Each thread should use its own instance.</li>
 * </ul>
 *
 * <p>Subclasses can extend this class to provide additional functionality or override default behavior.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/18 23:17
 */
public abstract sealed class AbstractQuery implements AutoCloseable permits NamedQuery, Query {

  private static final Logger log = LoggerFactory.getLogger(AbstractQuery.class);

  static final SqlStatementLogger stmtLogger = SqlStatementLogger.sharedInstance;

  private final JdbcConnection connection;

  private final String @Nullable [] columnNames;

  private final boolean returnGeneratedKeys;

  private @Nullable String name;

  private final String querySQL;
  private int maxBatchRecords = 0;
  private int currentBatchRecords = 0;

  private boolean caseSensitive;
  private boolean autoDerivingColumns = true;
  private boolean throwOnMappingFailure = true;

  private @Nullable PreparedStatement preparedStatement;

  private @Nullable TypeHandlerManager typeHandlerManager;

  private @Nullable Map<String, String> columnMappings;

  private @Nullable Map<String, String> caseSensitiveColumnMappings;

  private @Nullable QueryStatementCallback statementCallback;

  private @Nullable BatchResult batchResult;

  public AbstractQuery(JdbcConnection connection, String querySQL, boolean generatedKeys) {
    this(connection, querySQL, generatedKeys, null);
  }

  public AbstractQuery(JdbcConnection connection, String querySQL, String @Nullable [] columnNames) {
    this(connection, querySQL, false, columnNames);
  }

  protected AbstractQuery(JdbcConnection connection, String querySQL, boolean generatedKeys, String @Nullable [] columnNames) {
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

  /**
   * Checks whether the query is configured to be case-sensitive when handling column names.
   *
   * <p>This method returns the current state of case sensitivity, which can be set using
   * {@link #setCaseSensitive(boolean)}. By default, the behavior depends on the initialization
   * of the containing class.
   *
   * @return {@code true} if the query is case-sensitive; {@code false} otherwise.
   */
  public boolean isCaseSensitive() {
    return caseSensitive;
  }

  /**
   * Sets whether this query should handle column names in a case-sensitive manner.
   * When set to {@code true}, the query will treat column names with case sensitivity,
   * ensuring that the exact case of the column names is respected during processing.
   * When set to {@code false}, the query will treat column names in a case-insensitive manner.
   *
   * <p>Example usage:
   * <pre>{@code
   *   AbstractQuery query = new AbstractQuery(connection, "SELECT * FROM users", true);
   *
   *   // Enable case sensitivity
   *   query.setCaseSensitive(true);
   *
   *   // Disable case sensitivity
   *   query.setCaseSensitive(false);
   * }</pre>
   *
   * @param caseSensitive {@code true} to enable case sensitivity; {@code false} to disable it
   * @return the current instance of {@link AbstractQuery}, allowing for method chaining
   */
  public AbstractQuery setCaseSensitive(boolean caseSensitive) {
    this.caseSensitive = caseSensitive;
    return this;
  }

  /**
   * Checks whether the table is currently set to automatically derive its columns.
   * This feature allows the table to dynamically generate columns based on the data source,
   * which can simplify configuration in certain scenarios.
   *
   * @return true if auto-deriving columns is enabled, false otherwise
   */
  public boolean isAutoDerivingColumns() {
    return autoDerivingColumns;
  }

  /**
   * Sets whether the query should automatically derive columns based on the
   * provided data. This can be useful when working with dynamic schemas or
   * when the column structure is not explicitly defined.
   *
   * @param autoDerivingColumns a boolean value indicating whether automatic
   * derivation of columns should be enabled.
   * Set to {@code true} to enable, {@code false} otherwise.
   * @return the current instance of {@link AbstractQuery} for method chaining.
   */
  public AbstractQuery setAutoDerivingColumns(boolean autoDerivingColumns) {
    this.autoDerivingColumns = autoDerivingColumns;
    return this;
  }

  /**
   * Sets whether an exception should be thrown if a mapping failure occurs
   * during query execution. This method allows the user to configure the
   * behavior of the query when encountering mapping issues.
   *
   * <p>Example usage:
   * <pre>{@code
   * AbstractQuery query = ...;
   *
   * // Configure the query to throw an exception on mapping failure
   * query.throwOnMappingFailure(true);
   *
   * // Alternatively, configure the query to suppress exceptions on mapping failure
   * query.throwOnMappingFailure(false);
   * }</pre>
   *
   * @param throwOnMappingFailure a boolean value indicating whether to throw
   * an exception on mapping failure. Use {@code true} to enable throwing
   * exceptions, and {@code false} to disable it.
   * @return the current instance of {@link AbstractQuery}, allowing for
   * method chaining.
   * @see JdbcBeanMetadata
   */
  public AbstractQuery throwOnMappingFailure(boolean throwOnMappingFailure) {
    this.throwOnMappingFailure = throwOnMappingFailure;
    return this;
  }

  /**
   * Checks whether an exception should be thrown on mapping failure.
   *
   * <p>This method returns the current state of the {@code throwOnMappingFailure}
   * property. When this property is set to {@code true}, any mapping failure will
   * result in an exception being thrown. If set to {@code false}, the system will
   * handle mapping failures gracefully without throwing an exception.</p>
   *
   * @return {@code true} if an exception should be thrown on mapping failure,
   * {@code false} otherwise.
   */
  public boolean isThrowOnMappingFailure() {
    return throwOnMappingFailure;
  }

  /**
   * Returns the current JDBC connection object held by this instance.
   *
   * <p>This method is typically used to obtain the connection for executing SQL
   * statements or managing transactions. Ensure that the connection is properly
   * initialized before calling this method to avoid null pointer exceptions.
   *
   * @return the {@code JdbcConnection} object representing the current
   * database connection. Returns {@code null} if no connection
   * has been established.
   */
  public JdbcConnection getConnection() {
    return this.connection;
  }

  public @Nullable String getName() {
    return name;
  }

  public AbstractQuery setName(String name) {
    this.name = name;
    return this;
  }

  /**
   * Closes the {@code PreparedStatement} associated with this object and removes it
   * from the connection's statement registry. If the statement is already closed or
   * not initialized, this method does nothing.
   * <p>
   * Any exceptions encountered while closing the statement are logged as warnings
   * using the logger associated with this class. This ensures that the application
   * can continue running even if the statement closure fails.
   *
   * <p>
   * Notes:
   * - This method is idempotent; calling it multiple times has no additional effect.
   * - The {@code PreparedStatement} is removed from the connection's registry before
   * attempting to close it, ensuring proper resource management.
   */
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
   * Processes the given statement callback and associates it with this query.
   * If a non-null callback is provided, it will be invoked during the query
   * processing phase to customize the statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * AbstractQuery query = new AbstractQuery() {
   *   // Implementation details...
   * };
   *
   * QueryStatementCallback callback = stmt -> {
   *   // Customize the statement here
   *   stmt.addCondition("status = 'ACTIVE'");
   * };
   *
   * query.processStatement(callback);
   * }</pre>
   *
   * @param callback the callback to process the statement, may be {@code null}
   * @return the current query instance for method chaining
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
   * Fetches all rows from the result set and maps each row to an instance of the specified return type.
   * This method internally uses {@link #iterate(Class)} to create an iterator, then collects all
   * elements into a list using the iterator's {@code list()} method.
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Assuming `query` is an instance of AbstractQuery
   *   List<MyClass> results = query.fetch(MyClass.class);
   *
   *   // Iterate through the results
   *   for (MyClass obj : results) {
   *     System.out.println(obj);
   *   }
   * }</pre>
   *
   * @param <T> the type of the elements in the returned list
   * @param returnType the class object representing the type of each row in the result set
   * @return a list of instances of the specified return type, containing all rows from the result set
   */
  public <T extends @Nullable Object> List<T> fetch(Class<T> returnType) {
    return iterate(returnType).list();
  }

  /**
   * Fetches data from the result set and returns a list of objects extracted
   * by the provided handler. This method is typically used to process query
   * results in a database operation.
   *
   * <p>Example usage:
   * <pre>{@code
   * ResultSetExtractor<MyObject> extractor = rs -> {
   *   MyObject obj = new MyObject();
   *   obj.setId(rs.getInt("id"));
   *   obj.setName(rs.getString("name"));
   *   return obj;
   * };
   *
   * List<MyObject> results = fetch(extractor);
   * results.forEach(System.out::println);
   * }</pre>
   *
   * @param <T> the type of objects extracted by the handler
   * @param handler the {@link ResultSetExtractor} used to extract objects
   * from the result set. Must not be null.
   * @return a list of objects extracted from the result set using
   * the provided handler. Returns an empty list if no data
   * is available.
   */
  public <T extends @Nullable Object> List<T> fetch(ResultSetExtractor<T> handler) {
    return iterate(handler).list();
  }

  /**
   * Fetches a list of results from the database using the provided
   * {@code ResultSetHandlerFactory}. This method internally calls
   * {@code iterate(factory)} and converts the resulting iterable
   * into a list.
   *
   * <p>Example usage:
   * <pre>{@code
   *   ResultSetHandlerFactory<MyObject> factory = resultSet -> {
   *     MyObject obj = new MyObject();
   *     obj.setId(resultSet.getInt("id"));
   *     obj.setName(resultSet.getString("name"));
   *     return obj;
   *   };
   *
   *   List<MyObject> results = fetch(factory);
   *   results.forEach(System.out::println);
   * }</pre>
   *
   * @param <T> the type of elements to be fetched from the database
   * @param factory a factory that creates a handler for processing each row
   * of the result set
   * @return a list of objects of type T, processed by the handler created
   * by the provided factory
   * @see ResultSetHandlerFactory
   */
  public <T extends @Nullable Object> List<T> fetch(ResultSetHandlerFactory<T> factory) {
    return iterate(factory).list();
  }

  /**
   * Fetches the first element from the iteration result and returns it as an instance
   * of the specified return type. If no elements are available, {@code null} is returned.
   *
   * <p>This method is useful when you need to retrieve only the first result from a query
   * or operation that might produce multiple results. It internally invokes the
   * {@link #iterate(Class)} method to perform the iteration and extracts the first element.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   *   MyClass result = fetchFirst(MyClass.class);
   *   if (result != null) {
   *     System.out.println("First result: " + result);
   *   }
   *   else {
   *     System.out.println("No results found.");
   *   }
   * }</pre>
   *
   * @param <T> the type of the return value, which corresponds to the class provided
   * @param returnType the class object representing the desired return type
   * @return the first element of the iteration as an instance of the specified type,
   * or {@code null} if no elements are available
   */
  public <T extends @Nullable Object> T fetchFirst(Class<T> returnType) {
    return iterate(returnType).first();
  }

  /**
   * Fetches the first result extracted by the provided {@code ResultSetExtractor}.
   * This method internally calls {@code iterate(handler)} and retrieves the first
   * element from the resulting iterable.
   *
   * <p>Example usage:
   * <pre>{@code
   * ResultSetExtractor<MyObject> extractor = resultSet -> {
   *   List<MyObject> results = new ArrayList<>();
   *   while (resultSet.next()) {
   *     results.add(new MyObject(resultSet.getString("column_name")));
   *   }
   *   return results;
   * };
   *
   * MyObject firstResult = fetchFirst(extractor);
   * if (firstResult != null) {
   *   System.out.println("First result: " + firstResult);
   * }
   * else {
   *   System.out.println("No results found.");
   * }
   * }</pre>
   *
   * @param <T> the type of the result to be extracted
   * @param handler the {@code ResultSetExtractor} used to process the result set
   * and extract results; must not be null
   * @return the first extracted result, or {@code null} if no results are
   * available in the result set
   */
  public <T extends @Nullable Object> T fetchFirst(ResultSetExtractor<T> handler) {
    return iterate(handler).first();
  }

  /**
   * Fetches the first result from the query execution using the provided
   * {@code ResultSetHandlerFactory}. If no results are available,
   * this method returns {@code null}.
   *
   * <p>This method is particularly useful when you expect at most one result
   * or are only interested in the first result of a query.
   *
   * <p>Example usage:
   * <pre>{@code
   * ResultSetHandlerFactory<MyObject> factory = resultSet -> {
   *   // Implement your result set handling logic here
   *   return new MyObject(resultSet.getString("column_name"));
   * };
   *
   * MyObject firstResult = fetchFirst(factory);
   * if (firstResult != null) {
   *   System.out.println("First result: " + firstResult);
   * }
   * else {
   *   System.out.println("No results found.");
   * }
   * }</pre>
   *
   * @param <T> the type of the result object to be fetched
   * @param factory the {@code ResultSetHandlerFactory} used to process the
   * result set and produce the desired object
   * @return the first result of the query execution, or {@code null} if no
   * results are available
   */
  public <T extends @Nullable Object> T fetchFirst(ResultSetHandlerFactory<T> factory) {
    return iterate(factory).first();
  }

  /**
   * Creates and returns a {@code ResultSetIterator} for iterating over query results
   * mapped to the specified return type. This method is useful when you need to process
   * database query results in a type-safe manner.
   *
   * <p>You MUST call {@link ResultSetIterator#close()} when you are done iterating.
   * <p>Example usage:
   * <pre>{@code
   *   ResultSetHandlerFactory<MyEntity> factory = ...;
   *   ResultSetIterator<MyEntity> iterator = db.iterate(MyEntity.class);
   *
   *   while (iterator.hasNext()) {
   *     MyEntity entity = iterator.next();
   *     System.out.println(entity);
   *   }
   *   iterator.close();
   * }</pre>
   *
   * @param <T> the type of the return value, which corresponds to the class
   * provided as the parameter
   * @param returnType the {@code Class<T>} object representing the type to which
   * the query results should be mapped
   * @return a {@code ResultSetIterator<T>} instance that allows iteration over the
   * query results as instances of the specified return type
   */
  public <T extends @Nullable Object> ResultSetIterator<T> iterate(Class<T> returnType) {
    return new ResultSetHandlerIterator<>(createHandlerFactory(returnType));
  }

  /**
   * Creates and returns a {@code ResultSetIterator} that iterates over a result set
   * using the provided handler to extract data.
   *
   * <p>This method is useful when you need to process rows of a result set lazily,
   * converting each row into a desired object type via the specified handler.</p>
   *
   * <p>You MUST call {@link ResultSetIterator#close()} when you are done iterating.
   *
   * <p>Example usage:
   * <pre>{@code
   *   ResultSetExtractor<MyObject> extractor = rs -> {
   *     MyObject obj = new MyObject();
   *     obj.setId(rs.getInt("id"));
   *     obj.setName(rs.getString("name"));
   *     return obj;
   *   };
   *
   *   ResultSetIterator<MyObject> iterator = iterate(extractor);
   *   while (iterator.hasNext()) {
   *     MyObject obj = iterator.next();
   *     System.out.println(obj);
   *   }
   *   iterator.close();
   * }</pre>
   * </p>
   *
   * @param <T> the type of objects extracted from the result set
   * @param handler the {@code ResultSetExtractor} used to extract objects from the result set
   * @return a {@code ResultSetIterator} that iterates over the result set using the handler
   */
  public <T extends @Nullable Object> ResultSetIterator<T> iterate(ResultSetExtractor<T> handler) {
    return new ResultSetHandlerIterator<>(handler);
  }

  /**
   * Creates and returns a {@code ResultSetIterator} for iterating over a result set
   * using the provided {@code ResultSetHandlerFactory}.
   * <p>
   * This method is useful when you want to process each row of a result set with a
   * custom handler. The factory provided will be responsible for creating handlers
   * that process individual rows.
   * <p>You MUST call {@link ResultSetIterator#close()} when you are done iterating.
   * <p>
   * Example usage:
   * <pre>{@code
   * ResultSetHandlerFactory<MyObject> factory = resultSet -> {
   *   MyObject obj = new MyObject();
   *   obj.setId(resultSet.getInt("id"));
   *   obj.setName(resultSet.getString("name"));
   *   return obj;
   * };
   *
   * ResultSetIterator<MyObject> iterator = iterate(factory);
   * while (iterator.hasNext()) {
   *   MyObject obj = iterator.next();
   *   System.out.println(obj);
   * }
   * iterator.close();
   * }</pre>
   *
   * @param <T> the type of objects produced by the iterator
   * @param factory the {@code ResultSetHandlerFactory} used to create handlers for
   * processing each row of the result set
   * @return a {@code ResultSetIterator} that iterates over the result set using the
   * provided factory
   */
  public <T extends @Nullable Object> ResultSetIterator<T> iterate(ResultSetHandlerFactory<T> factory) {
    return new ResultSetHandlerIterator<>(factory);
  }

  public <T extends @Nullable Object> ResultSetHandlerFactory<T> createHandlerFactory(Class<T> returnType) {
    return new DefaultResultSetHandlerFactory<>(
            new JdbcBeanMetadata(returnType, caseSensitive, autoDerivingColumns, throwOnMappingFailure),
            connection.getManager(), getColumnMappings());
  }

  /**
   * Fetches and returns a {@code LazyTable} instance using the conversion service
   * obtained from the current connection's manager. This method is a convenience
   * wrapper around the overloaded {@code fetchLazyTable} method with a predefined
   * conversion service.
   *
   * <p>Example usage:
   * <pre>{@code
   *   LazyTable lazyTable = service.fetchLazyTable();
   *
   *   // Use the lazyTable instance for further operations
   * }</pre>
   *
   * @return a {@code LazyTable} instance configured with the appropriate
   * conversion service for lazy loading operations.
   */
  public LazyTable fetchLazyTable() {
    return fetchLazyTable(connection.getManager().getConversionService());
  }

  /**
   * Fetches and returns a {@code LazyTable} instance by executing a query and
   * utilizing the provided {@code ConversionService} for result processing.
   * <p>
   * This method is useful when you need to lazily load table data from a query
   * result. The {@code ConversionService}, if provided, can be used to apply
   * custom conversions to the fetched data.
   * <p>
   * Example usage:
   * <pre>{@code
   *   ConversionService conversionService = new DefaultConversionService();
   *   LazyTable lazyTable = fetchLazyTable(conversionService);
   *
   *   // Iterate over the lazy-loaded table
   *   for (Row row : lazyTable.rows()) {
   *     System.out.println(row);
   *   }
   * }</pre>
   *
   * @param conversionService an optional {@code ConversionService} instance that
   * can be used to apply custom conversions to the query results. If null,
   * no custom conversions will be applied.
   * @return a {@code LazyTable} instance representing the lazily loaded table
   * data from the executed query.
   */
  public LazyTable fetchLazyTable(@Nullable ConversionService conversionService) {
    return new TableResultSetIterator(executeQuery(), isCaseSensitive(), conversionService).createLazyTable();
  }

  public Table fetchTable() {
    return fetchTable(connection.getManager().getConversionService());
  }

  /**
   * Fetches and constructs a {@code Table} object by converting data from a
   * {@code LazyTable} using the provided {@code ConversionService}.
   *
   * <p>This method retrieves rows from a {@code LazyTable} instance, converts
   * them (if necessary), and stores them in an {@code ArrayList<Row>}. The
   * resulting {@code Table} object is then constructed using the name, rows,
   * and columns from the {@code LazyTable}.
   *
   * <p><b>Example Usage:</b>
   * <pre>{@code
   * ConversionService conversionService = new DefaultConversionService();
   * Table table = fetchTable(conversionService);
   * System.out.println("Table Name: " + table.getName());
   * for (Row row : table.rows()) {
   *   System.out.println(row);
   * }
   * }</pre>
   *
   * @param conversionService the {@code ConversionService} used to facilitate
   * data conversion within the {@code LazyTable}
   * @return a fully constructed {@code Table} object containing the name, rows,
   * and columns extracted from the {@code LazyTable}
   */
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
   * Executes an update operation and returns the result of the operation.
   * This method is typically used for executing SQL statements such as INSERT,
   * UPDATE, or DELETE. It internally calls another overloaded method
   * {@code executeUpdate(returnGeneratedKeys)} to perform the actual operation.
   *
   * @param <T> the type of the generated key or result associated with the update operation
   * @return an {@link UpdateResult} object containing the outcome of the update operation,
   * including any generated keys or error information
   */
  public <T extends @Nullable Object> UpdateResult<T> executeUpdate() {
    return executeUpdate(returnGeneratedKeys);
  }

  /**
   * Executes an update operation and returns the result.
   * This method allows specifying whether generated keys should be retrieved
   * during the execution of the update.
   *
   * @param generatedKeys a boolean flag indicating whether generated keys
   * should be retrieved. If {@code true}, the method will use a shared
   * instance of {@code ObjectTypeHandler} to handle the generated keys;
   * otherwise, no handler will be used.
   * @return an {@code UpdateResult<T>} object representing the result of the
   * update operation. This includes information about the success of
   * the operation and any generated keys if requested.
   */
  @SuppressWarnings("unchecked")
  public <T extends @Nullable Object> UpdateResult<T> executeUpdate(boolean generatedKeys) {
    return (UpdateResult<T>) executeUpdate(generatedKeys ? ObjectTypeHandler.sharedInstance : null);
  }

  /**
   * Executes an update operation using the provided generated key handler.
   * This method logs the statement, executes the update via a prepared statement,
   * and handles generated keys if a handler is provided. It also ensures proper
   * exception handling and resource cleanup.
   *
   * <p>Example usage:
   * <pre>{@code
   * TypeHandler<Long> keyHandler = new LongTypeHandler();
   * UpdateResult<Long> result = executeUpdate(keyHandler);
   * List<Long> generatedKeys = result.getKeys();
   * System.out.println("Generated keys: " + generatedKeys);
   * }</pre>
   *
   * @param <T> the type of generated keys to be handled
   * @param generatedKeyHandler a handler for processing generated keys, or null if no keys are expected
   * @return an {@link UpdateResult} object containing the update count and optionally the generated keys
   * @throws DataAccessException if an SQL exception occurs during the execution of the update
   */
  public <T extends @Nullable Object> UpdateResult<T> executeUpdate(@Nullable TypeHandler<T> generatedKeyHandler) {
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

  /**
   * Returns the {@code TypeHandlerManager} instance associated with this object.
   * If the manager is not already initialized, it will be retrieved from the
   * connection's manager and cached for future use.
   *
   * <p>Example usage:
   * <pre>{@code
   *   MyObject obj = new MyObject();
   *   TypeHandlerManager manager = obj.getTypeHandlerManager();
   *
   *   // Use the manager to handle types
   *   TypeHandler<?> handler = manager.getTypeHandler(MyClass.class);
   *   if (handler != null) {
   *     System.out.println("Type handler found for MyClass");
   *   }
   * }</pre>
   *
   * @return the {@code TypeHandlerManager} instance, which is used to manage
   * type handlers for this object
   */
  public TypeHandlerManager getTypeHandlerManager() {
    TypeHandlerManager ret = this.typeHandlerManager;
    if (ret == null) {
      ret = this.connection.getManager().getTypeHandlerManager();
      this.typeHandlerManager = ret;
    }
    return ret;
  }

  /**
   * Checks whether generated keys should be returned after executing an update
   * operation (e.g., INSERT, UPDATE, or DELETE) in a database query.
   *
   * <p>This method is typically used in conjunction with JDBC operations where
   * the return of auto-generated keys (such as auto-incremented primary keys)
   * needs to be explicitly enabled or disabled.
   *
   * @return {@code true} if generated keys are to be returned;
   * {@code false} otherwise.
   */
  public boolean isReturnGeneratedKeys() {
    return returnGeneratedKeys;
  }

  /**
   * Sets the {@code TypeHandlerManager} for this instance.
   * The {@code TypeHandlerManager} is responsible for managing type handlers
   * used in data mapping or conversion processes.
   *
   * @param typeHandlerManager the {@code TypeHandlerManager} to set, or
   * {@code null} to clear the current manager
   */
  public void setTypeHandlerManager(@Nullable TypeHandlerManager typeHandlerManager) {
    this.typeHandlerManager = typeHandlerManager;
  }

  /**
   * Returns a scalar value processed by the default object type handler.
   * This method internally delegates the operation to an overloaded
   * {@code scalar} method, passing {@link ObjectTypeHandler#sharedInstance}
   * as the handler.
   *
   * <p>This method can return {@code null} if the underlying implementation
   * or the provided handler results in a {@code null} value. The exact behavior
   * depends on the logic implemented in the overloaded method and the handler.
   *
   * <p><strong>Example Usage:</strong>
   * <pre>{@code
   * Object result = scalar();
   * if (result != null) {
   *   System.out.println("Scalar value: " + result);
   * }
   * else {
   *   System.out.println("The scalar value is null.");
   * }
   * }</pre>
   *
   * @return the scalar value processed by the default handler, or {@code null}
   * if the computation results in no value
   */
  @SuppressWarnings("unchecked")
  public <T extends @Nullable Object> T scalar() {
    return (T) scalar(ObjectTypeHandler.sharedInstance);
  }

  /**
   * Returns a scalar value of the specified return type.
   * This method retrieves the scalar value by using a type handler
   * associated with the given return type. If no appropriate type handler
   * is found, the behavior depends on the implementation of the
   * {@code scalar(TypeHandler)} method.
   *
   * <p>Example usage:
   * <pre>{@code
   *   Integer result = scalar(Integer.class);
   *
   *   if (result != null) {
   *     System.out.println("Scalar value: " + result);
   *   }
   *   else {
   *     System.out.println("No scalar value found.");
   *   }
   * }</pre>
   *
   * @param <V> the type of the scalar value to be returned
   * @param returnType the class object representing the desired return type
   * for the scalar value. Must not be null.
   * @return the scalar value of the specified type, or null if no value
   * is available or no suitable type handler is found
   */
  public <V extends @Nullable Object> V scalar(Class<V> returnType) {
    return scalar(getTypeHandlerManager().getTypeHandler(returnType));
  }

  /**
   * Executes a scalar query and returns the first column of the first row in the result set.
   * This method is typically used for queries that return a single value, such as aggregate functions
   * (e.g., COUNT, SUM, MAX) or simple lookups.
   *
   * <p>Example usage:
   * <pre>{@code
   * TypeHandler<Integer> typeHandler = new IntegerTypeHandler();
   * Integer count = scalar(typeHandler);
   * if (count != null) {
   *   System.out.println("Result: " + count);
   * }
   * else {
   *   System.out.println("No result found.");
   * }
   * }</pre>
   *
   * <p>The method logs the execution time and handles resource cleanup automatically. If an exception
   * occurs during execution, it is translated into a runtime exception and rethrown.
   *
   * @param <T> the type of the result, determined by the provided {@link TypeHandler}
   * @param typeHandler the {@link TypeHandler} instance used to extract the result from the
   * {@link ResultSet}. Must not be null.
   * @return the scalar value extracted from the first column of the first row in the result set,
   * or {@code null} if the result set is empty.
   * @throws RuntimeException if a SQL exception occurs during query execution. The exception is
   * translated using the {@link #translateException(String, SQLException)} method.
   */
  public <T extends @Nullable Object> T scalar(TypeHandler<T> typeHandler) {
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

  /**
   * Returns a list of scalar values extracted from the result set,
   * using the provided return type to determine the appropriate
   * type handling and conversion.
   *
   * <p>This method is useful when you want to retrieve a list of
   * single-column results from a query. It leverages a {@link TypeHandler}
   * to convert the raw database values into the desired Java type.</p>
   *
   * <p>Example usage:
   * <pre>{@code
   *   // Assuming a query that returns a list of integers
   *   List<Integer> ids = scalars(Integer.class);
   *
   *   // Example with strings
   *   List<String> names = scalars(String.class);
   * }</pre>
   * </p>
   *
   * @param <T> the type of the scalar values to be returned
   * @param returnType the Class object representing the desired
   * type of the scalar values (e.g., Integer.class, String.class)
   * @return a list of scalar values of the specified type, extracted
   * from the result set
   */
  public <T extends @Nullable Object> List<T> scalars(Class<T> returnType) {
    TypeHandler<T> typeHandler = getTypeHandlerManager().getTypeHandler(returnType);
    return scalars(typeHandler);
  }

  /**
   * Executes a query and returns a list of scalar values extracted using the provided
   * {@code TypeHandler}. This method is useful when you need to map database results
   * to a specific type using a custom handler.
   *
   * <p>Example usage:
   * <pre>{@code
   * TypeHandler<Integer> intHandler = resultSet -> resultSet.getInt("column_name");
   * List<Integer> scalars = query.scalars(intHandler);
   *
   * for (Integer value : scalars) {
   *   System.out.println("Scalar value: " + value);
   * }
   * }</pre>
   *
   * @param <T> the type of scalar values to be extracted
   * @param typeHandler the {@code TypeHandler} used to extract scalar values from the result set
   * @return a list of scalar values of type {@code T} extracted using the provided handler
   */
  public <T extends @Nullable Object> List<T> scalars(TypeHandler<T> typeHandler) {
    return fetch(new TypeHandlerResultSetHandler<>(typeHandler));
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
      throw new IllegalArgumentException("maxBatchRecords should be a non-negative value");
    }
    this.maxBatchRecords = maxBatchRecords;
    return this;
  }

  /**
   * Returns the maximum number of records allowed in a single batch.
   *
   * This method retrieves the value of the {@code maxBatchRecords} property,
   * which defines the upper limit of records that can be processed in one batch.
   * This is useful for controlling memory usage or optimizing performance
   * in batch processing scenarios.
   *
   * Example usage:
   * <pre>{@code
   *   int maxRecords = getMaxBatchRecords();
   *   System.out.println("Maximum records per batch: " + maxRecords);
   *
   *   // Use the value to configure batch processing logic
   *   if (records.size() > maxRecords) {
   *     System.out.println("Record count exceeds the allowed batch size.");
   *   }
   * }</pre>
   *
   * @return the maximum number of records allowed in a single batch
   */
  public int getMaxBatchRecords() {
    return this.maxBatchRecords;
  }

  /**
   * Returns the number of records in the current batch.
   *
   * This method is useful when processing data in batches and you need to know
   * how many records are present in the current batch. For example, it can be
   * used to determine whether the batch size meets a certain threshold or to
   * log the batch size for monitoring purposes.
   *
   * @return the number of records in the current batch
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
   * Executes a batch operation based on the current configuration and returns
   * the result of the execution. This method internally calls
   * {@link #executeBatch(boolean)} with the value of {@code returnGeneratedKeys}.
   *
   * <p>This is typically used when you need to execute multiple SQL statements
   * in a single batch for performance optimization.
   *
   * @return a {@link BatchResult} object containing the outcome of the batch
   * execution, such as update counts or generated keys if applicable.
   */
  public BatchResult executeBatch() {
    return executeBatch(returnGeneratedKeys);
  }

  /**
   * Executes a batch operation using the current statement and optionally retrieves generated keys.
   * This method logs the execution time, handles exceptions, and resets the batch records counter
   * upon successful execution. If enabled, it fetches generated keys from the database and adds
   * them to the result.
   *
   * @param generatedKeys a boolean flag indicating whether to retrieve generated keys
   * from the database. Set to {@code true} if generated keys are expected,
   * otherwise set to {@code false}.
   * @return a {@link BatchResult} object containing the results of the batch execution,
   * including any generated keys if requested.
   * @throws GeneratedKeysException if an error occurs while fetching generated keys and
   * the {@code generatedKeys} parameter is set to {@code true}.
   * @throws DataAccessException if a SQL exception occurs during the batch execution.
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

  /**
   * Executes a batch operation using the provided {@link TypeHandler} to handle generated keys.
   * This method logs the execution time, processes the batch result, and handles any exceptions
   * that may occur during the execution. If a {@link TypeHandler} is provided, it is used to
   * process the generated keys returned by the database.
   *
   * <p>Example usage:
   * <pre>{@code
   * TypeHandler<MyType> handler = new MyTypeHandler();
   * BatchResult result = executor.executeBatch(handler);
   * System.out.println("Batch executed successfully: " + result.getAffectedRows());
   * }</pre>
   *
   * <p>If no generated keys are expected, you can pass {@code null} as the handler:
   * <pre>{@code
   * BatchResult result = executor.executeBatch(null);
   * System.out.println("Batch executed without fetching generated keys.");
   * }</pre>
   *
   * @param <T> the type of the generated keys to be handled by the {@link TypeHandler}
   * @param handler a {@link TypeHandler} instance to process generated keys, or {@code null}
   * if no generated keys are expected
   * @return a {@link BatchResult} object containing the results of the batch execution,
   * including update counts and optionally generated keys
   * @throws GeneratedKeysException if an error occurs while fetching generated keys and
   * the {@code handler} is not {@code null}
   * @throws DataAccessException if a SQL exception occurs during the batch execution
   */
  public <T extends @Nullable Object> BatchResult executeBatch(@Nullable TypeHandler<T> handler) {
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

  public @Nullable Map<String, String> getColumnMappings() {
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
        columnMappings.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue().toLowerCase(Locale.ROOT));
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
    columnMappings.put(columnName.toLowerCase(Locale.ROOT), propertyName.toLowerCase(Locale.ROOT));
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
   * @param <T> result type
   * @since 4.0
   */
  private final class ResultIterable<T extends @Nullable Object> extends ResultSetIterable<T> {

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
      return iterator;
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
   * @param <T> result type
   * @since 4.0
   */
  abstract class CloseResultSetIterator<T extends @Nullable Object> extends ResultSetIterator<T> {

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
   * @param <T> result type
   * @since 4.0
   */
  final class ResultSetHandlerIterator<T extends @Nullable Object> extends CloseResultSetIterator<T> {
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

    private final String tableName;

    private final List<Column> columns;

    private final boolean isCaseSensitive;

    private final ConversionService conversionService;

    private final Map<String, Integer> columnNameToIdxMap;

    private TableResultSetIterator(ResultSet rs, boolean isCaseSensitive, @Nullable ConversionService conversionService) {
      super(rs);
      this.isCaseSensitive = isCaseSensitive;
      this.conversionService = conversionService == null ? DefaultConversionService.getSharedInstance() : conversionService;
      try {
        ResultSetMetaData meta = rs.getMetaData();
        ArrayList<Column> columns = new ArrayList<>();
        var columnNameToIdxMap = new LinkedHashMap<String, Integer>();

        int columnCount = meta.getColumnCount();
        for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
          String colName = JdbcUtils.lookupColumnName(meta, colIdx);
          String colType = meta.getColumnTypeName(colIdx);
          columns.add(new Column(colName, colIdx - 1, colType));

          String colMapName = isCaseSensitive ? colName : colName.toLowerCase(Locale.ROOT);
          columnNameToIdxMap.put(colMapName, colIdx - 1);
        }
        this.columns = columns;
        this.tableName = meta.getTableName(1);
        this.columnNameToIdxMap = columnNameToIdxMap;
      }
      catch (SQLException e) {
        throw new PersistenceException("Error while reading metadata from database", e);
      }
    }

    public LazyTable createLazyTable() {
      return new LazyTable(tableName, asIterable(), columns);
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
