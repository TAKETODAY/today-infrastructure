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

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

import infra.jdbc.type.TypeHandler;

/**
 * Represents a query object that extends {@link AbstractQuery} and provides methods to
 * construct, parameterize, and execute SQL queries. This class is designed to simplify
 * the process of binding parameters to prepared statements and executing them.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Support for adding and setting parameters of various types.</li>
 *   <li>Fluent API for chaining method calls.</li>
 *   <li>Automatic parameter binding during statement execution.</li>
 *   <li>Customizable behavior for case sensitivity, column mapping, and more.</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * JdbcConnection connection = ...;
 * Query query = new Query(connection, "INSERT INTO users (name, age) VALUES (?, ?)", true)
 *     .addParameter("John Doe")
 *     .addParameter(30);
 *
 * // Execute the query
 * query.execute();
 *
 * // Alternatively, set parameters at specific positions
 * Query updateQuery = new Query(connection, "UPDATE users SET age = ? WHERE name = ?", false)
 *     .setParameter(1, 35)
 *     .setParameter(2, "John Doe");
 *
 * // Execute the update query
 * updateQuery.executeUpdate();
 * }</pre>
 *
 * <p><strong>Parameter Binding:</strong>
 * Parameters can be added using the {@link #addParameter(Object)} method or set at specific
 * positions using {@link #setParameter(int, Object)}. The class supports a wide range of
 * parameter types, including primitive types, {@link String}, {@link InputStream}, and
 * Java 8 date/time types like {@link LocalDate}, {@link LocalTime}, and {@link LocalDateTime}.
 *
 * <p><strong>Custom Type Handling:</strong>
 * For custom objects, the {@link #addParameter(Object)} method uses a {@link TypeHandler}
 * to bind the parameter. Ensure that a suitable {@link TypeHandler} is registered for
 * the object's class.
 *
 * <p><strong>Statement Post-Processing:</strong>
 * The {@link #postProcessStatement(PreparedStatement)} method is invoked internally to bind
 * all parameters to the prepared statement. If an error occurs during binding, a
 * {@link ParameterBindFailedException} is thrown.
 *
 * <p><strong>Batch Operations:</strong>
 * Use the {@link #addToBatch()} method to add the current query to a batch. Batch execution
 * can then be performed using the appropriate execution method.
 *
 * <p><strong>Customizing Behavior:</strong>
 * Methods like {@link #setCaseSensitive(boolean)}, {@link #setAutoDerivingColumns(boolean)},
 * and {@link #throwOnMappingFailure(boolean)} allow customization of query behavior.
 *
 * <p><strong>Statement Callbacks:</strong>
 * A {@link QueryStatementCallback} can be registered using {@link #processStatement(QueryStatementCallback)}
 * to perform additional processing on the {@link PreparedStatement} before execution.
 *
 * <p><strong>Thread Safety:</strong>
 * Instances of this class are not thread-safe. Each thread should use its own instance.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/1/18 23:17
 */
public final class Query extends AbstractQuery {

  private final ArrayList<ParameterBinder> queryParameters = new ArrayList<>();

  public Query(JdbcConnection connection, String querySQL, boolean generatedKeys) {
    super(connection, querySQL, generatedKeys);
  }

  public Query(JdbcConnection connection, String querySQL, String @Nullable [] columnNames) {
    super(connection, querySQL, columnNames);
  }

  protected Query(JdbcConnection connection, String querySQL, boolean generatedKeys, String @Nullable [] columnNames) {
    super(connection, querySQL, generatedKeys, columnNames);
  }

  /**
   * Adds an integer parameter to the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forInt(int)} to create a binder
   * that will set the integer value at the appropriate position in the statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE age > ?",...);
   * query.addParameter(18);
   * }</pre>
   *
   * <p>The above example demonstrates how to add an integer parameter to a query.
   * The value `18` will be bound to the placeholder `?` in the SQL statement.
   *
   * @param value the integer value to be added as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query addParameter(int value) {
    return addParameter(ParameterBinder.forInt(value));
  }

  /**
   * Adds a long parameter to the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forLong(long)} to create a binder
   * that will set the long value at the appropriate position in the statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE id = ?",...);
   * query.addParameter(12345L);
   * }</pre>
   *
   * @param value the long value to be added as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query addParameter(long value) {
    return addParameter(ParameterBinder.forLong(value));
  }

  /**
   * Adds a string parameter to the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forString(String)} to create a binder
   * that will set the string value at the appropriate position in the statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE name = ?",...);
   * query.addParameter("John Doe");
   * }</pre>
   *
   * @param value the string value to be added as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query addParameter(String value) {
    return addParameter(ParameterBinder.forString(value));
  }

  /**
   * Adds a boolean parameter to the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forBoolean(boolean)} to create a binder
   * that will set the boolean value at the appropriate position in the statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE active = ?",...);
   * query.addParameter(true);
   * }</pre>
   *
   * @param value the boolean value to be added as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query addParameter(boolean value) {
    return addParameter(ParameterBinder.forBoolean(value));
  }

  /**
   * Adds an {@link InputStream} parameter to the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forBinaryStream(InputStream)} to create a binder
   * that will set the binary stream at the appropriate position in the statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * InputStream data = ...;
   * Query query = new Query(connection, "INSERT INTO files (data) VALUES (?)",...);
   * query.addParameter(data);
   * }</pre>
   *
   * @param value the InputStream value to be added as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query addParameter(InputStream value) {
    return addParameter(ParameterBinder.forBinaryStream(value));
  }

  /**
   * Adds a parameter of type {@link LocalDate} to the query.
   *
   * This method binds the provided {@code LocalDate} value to the query using
   * a parameter binder. It internally calls {@link #addParameter(ParameterBinder)}
   * with a binder created for the given value.
   *
   * @param value the {@link LocalDate} parameter to be added to the query.
   * Must not be null.
   * @return the current {@link Query} instance with the added parameter,
   * allowing for method chaining.
   *
   * Example usage:
   * <pre>{@code
   *   LocalDate date = LocalDate.of(2023, 10, 1);
   *   Query query = new Query(connection, "SELECT * FROM events WHERE event_date = ?",...);
   *     .addParameter(date);
   * }</pre>
   */
  public Query addParameter(LocalDate value) {
    return addParameter(ParameterBinder.forObject(value));
  }

  /**
   * Adds a parameter of type {@link LocalTime} to the query.
   *
   * This method binds the provided {@code LocalTime} value to the query using
   * a {@link ParameterBinder}. It is useful when constructing queries that
   * require time-based parameters.
   *
   * Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM schedules WHERE start_time = ?",...);
   *   LocalTime time = LocalTime.of(14, 30);
   *   query.addParameter(time);
   *
   *   // The query now includes the time parameter and can be executed.
   * }</pre>
   *
   * @param value the {@link LocalTime} value to be added as a parameter
   * @return the current {@link Query} instance with the added parameter,
   * allowing for method chaining
   */
  public Query addParameter(LocalTime value) {
    return addParameter(ParameterBinder.forObject(value));
  }

  /**
   * Adds a parameter of type {@link LocalDateTime} to the current query.
   * This method internally uses a parameter binder to bind the provided
   * value to the query, ensuring proper handling of the date-time object.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM logs WHERE log_time = ?",...);
   * LocalDateTime dateTime = LocalDateTime.now();
   *
   * // Add the LocalDateTime parameter to the query
   * query.addParameter(dateTime);
   * }</pre>
   *
   * @param value the {@link LocalDateTime} object to be added as a parameter
   * to the query. Must not be null.
   * @return the updated {@link Query} instance with the added parameter.
   * This allows for method chaining to build the query further.
   */
  public Query addParameter(LocalDateTime value) {
    return addParameter(ParameterBinder.forObject(value));
  }

  /**
   * Adds a generic object parameter to the query for binding in a {@link PreparedStatement}.
   * This method retrieves the appropriate {@link TypeHandler} for the object's class and uses
   * {@link ParameterBinder#forTypeHandler(TypeHandler, Object)} to create a binder for the statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * CustomObject obj = new CustomObject(...);
   * Query query = new Query(connection, "INSERT INTO custom_table (data) VALUES (?)",...);
   * query.addParameter(obj);
   * }</pre>
   *
   * @param value the object value to be added as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public Query addParameter(Object value) {
    TypeHandler typeHandler = getTypeHandlerManager().getTypeHandler(value.getClass());
    return addParameter(ParameterBinder.forTypeHandler(typeHandler, value));
  }

  /**
   * Adds a custom {@link ParameterBinder} to the query for advanced parameter binding.
   * This method allows direct addition of a pre-configured binder to the query's parameter list.
   *
   * <p>Example usage:
   * <pre>{@code
   * ParameterBinder binder = ParameterBinder.forInt(42);
   * Query query = new Query(connection, "SELECT * FROM numbers WHERE value = ?",...);
   * query.addParameter(binder);
   * }</pre>
   *
   * @param binder the {@link ParameterBinder} to be added
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query addParameter(ParameterBinder binder) {
    queryParameters.add(binder);
    return this;
  }

  //

  /**
   * Sets a string parameter at a specific position in the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forString(String)} to create a binder
   * for the specified position.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE name = ?",...);
   * query.setParameter(1, "John Doe");
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index)
   * @param value the string value to be set as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query setParameter(int pos, String value) {
    return setParameter(pos, ParameterBinder.forString(value));
  }

  /**
   * Sets an integer parameter at a specific position in the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forInt(int)} to create a binder
   * for the specified position.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE age = ?",...);
   * query.setParameter(1, 30);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index)
   * @param value the integer value to be set as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query setParameter(int pos, int value) {
    return setParameter(pos, ParameterBinder.forInt(value));
  }

  /**
   * Sets a long parameter at a specific position in the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forLong(long)} to create a binder
   * for the specified position.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE id = ?",...);
   * query.setParameter(1, 12345L);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index)
   * @param value the long value to be set as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query setParameter(int pos, long value) {
    return setParameter(pos, ParameterBinder.forLong(value));
  }

  /**
   * Sets a boolean parameter at a specific position in the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forBoolean(boolean)} to create a binder
   * for the specified position.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE active = ?",...);
   * query.setParameter(1, true);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index)
   * @param value the boolean value to be set as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query setParameter(int pos, boolean value) {
    return setParameter(pos, ParameterBinder.forBoolean(value));
  }

  /**
   * Sets an {@link InputStream} parameter at a specific position in the query for binding in a {@link PreparedStatement}.
   * This method internally uses {@link ParameterBinder#forBinaryStream(InputStream)} to create a binder
   * for the specified position.
   *
   * <p>Example usage:
   * <pre>{@code
   * InputStream data = ...;
   * Query query = new Query(connection, "INSERT INTO files (data) VALUES (?)",...);
   * query.setParameter(1, data);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index)
   * @param value the InputStream value to be set as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query setParameter(int pos, InputStream value) {
    return setParameter(pos, ParameterBinder.forBinaryStream(value));
  }

  /**
   * Sets the parameter at the specified position in the query to the given
   * {@link LocalDate} value. This method binds the provided value using a
   * parameter binder, which ensures proper handling of the value during query
   * execution.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query("SELECT * FROM events WHERE date = ?", ...);
   * LocalDate date = LocalDate.of(2023, 10, 15);
   * query.setParameter(1, date);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index).
   * Must be a positive integer.
   * @param value the {@link LocalDate} value to bind to the parameter.
   * Must not be null.
   * @return the same {@link Query} instance with the parameter set, allowing
   * for method chaining.
   */
  public Query setParameter(int pos, LocalDate value) {
    return setParameter(pos, ParameterBinder.forObject(value));
  }

  /**
   * Sets the parameter at the specified position in the query to the given
   * {@link LocalTime} value. This method is typically used to bind time-based
   * parameters to a query for execution.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query("SELECT * FROM events WHERE event_time = ?");
   * LocalTime eventTime = LocalTime.of(14, 30);
   *
   * // Bind the LocalTime value to the first parameter (position 1)
   * query.setParameter(1, eventTime);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index).
   * Must be a positive integer.
   * @param value the {@link LocalTime} value to bind to the specified parameter.
   * If null, the parameter will be treated as NULL in the query.
   * @return the same {@link Query} instance with the parameter set, allowing
   * for method chaining.
   */
  public Query setParameter(int pos, LocalTime value) {
    return setParameter(pos, ParameterBinder.forObject(value));
  }

  /**
   * Sets the parameter at the specified position in the query to the given
   * {@link LocalDateTime} value. This method binds the provided value to the
   * query using a parameter binder, which ensures proper handling of the value
   * during query execution.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query("SELECT * FROM events WHERE event_time = ?");
   * LocalDateTime dateTime = LocalDateTime.of(2023, 10, 1, 12, 0);
   *
   *  // Bind the LocalDateTime value to the first parameter (position 1)
   *  query.setParameter(1, dateTime);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index).
   * Must be a positive integer.
   * @param value the {@link LocalDateTime} value to bind to the parameter.
   * Must not be null.
   * @return the same {@link Query} instance with the parameter set, allowing
   * for method chaining.
   */
  public Query setParameter(int pos, LocalDateTime value) {
    return setParameter(pos, ParameterBinder.forObject(value));
  }

  /**
   * Sets a generic object parameter at a specific position in the query for binding in a {@link PreparedStatement}.
   * This method retrieves the appropriate {@link TypeHandler} for the object's class and uses
   * {@link ParameterBinder#forTypeHandler(TypeHandler, Object)} to create a binder for the specified position.
   *
   * <p>Example usage:
   * <pre>{@code
   * CustomObject obj = new CustomObject(...);
   * Query query = new Query(connection, "INSERT INTO custom_table (data) VALUES (?)",...);
   * query.setParameter(1, obj);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index)
   * @param value the object value to be set as a parameter
   * @return the current {@link Query} instance, allowing method chaining
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public Query setParameter(int pos, Object value) {
    TypeHandler typeHandler = getTypeHandlerManager().getTypeHandler(value.getClass());
    return setParameter(pos, ParameterBinder.forTypeHandler(typeHandler, value));
  }

  /**
   * Sets a custom {@link ParameterBinder} at a specific position in the query for advanced parameter binding.
   * This method allows direct assignment of a pre-configured binder to the specified position in the query's parameter list.
   *
   * <p>Example usage:
   * <pre>{@code
   * ParameterBinder binder = ParameterBinder.forInt(42);
   * Query query = new Query(connection, "SELECT * FROM numbers WHERE value = ?",...);
   * query.setParameter(1, binder);
   * }</pre>
   *
   * @param pos the position of the parameter in the query (1-based index)
   * @param binder the {@link ParameterBinder} to be set at the specified position
   * @return the current {@link Query} instance, allowing method chaining
   */
  public Query setParameter(int pos, ParameterBinder binder) {
    queryParameters.set(pos, binder);
    return this;
  }

  /**
   * Retrieves the list of {@link ParameterBinder} objects currently associated with the query.
   * This method provides access to the internal list of parameter binders, allowing inspection or modification.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE id = ?",...);
   * query.addParameter(12345L);
   * ArrayList<ParameterBinder> parameters = query.getQueryParameters();
   * }</pre>
   *
   * @return the list of {@link ParameterBinder} objects representing the query parameters
   */
  public ArrayList<ParameterBinder> getQueryParameters() {
    return queryParameters;
  }

  /**
   * Clears all parameters currently associated with the query.
   * This method removes all {@link ParameterBinder} objects from the internal parameter list,
   * effectively resetting the query for reuse.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE id = ?",...);
   * query.addParameter(12345L);
   * query.clearParameters();
   * }</pre>
   */
  public void clearParameters() {
    queryParameters.clear();
  }

  //

  @Override
  protected void postProcessStatement(PreparedStatement statement) {
    // parameters assignation to query
    int paramIdx = 1;
    for (ParameterBinder binder : queryParameters) {
      try {
        binder.bind(statement, paramIdx++);
      }
      catch (SQLException e) {
        throw new ParameterBindFailedException(
                "Error binding parameter index: '" + (paramIdx - 1) + "' - " + e.getMessage(), e);
      }
    }
  }

  //

  /**
   * Sets whether the query should be case-sensitive when performing operations such as column mapping.
   * This method overrides the default behavior inherited from {@link AbstractQuery}.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE name = ?",...);
   * query.setCaseSensitive(true);
   * }</pre>
   *
   * @param caseSensitive whether the query should be case-sensitive
   * @return the current {@link Query} instance, allowing method chaining
   */
  @Override
  public Query setCaseSensitive(boolean caseSensitive) {
    super.setCaseSensitive(caseSensitive);
    return this;
  }

  /**
   * Sets whether the query should automatically derive column mappings based on the query structure.
   * This method overrides the default behavior inherited from {@link AbstractQuery}.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users",...);
   * query.setAutoDerivingColumns(true);
   * }</pre>
   *
   * @param autoDerivingColumns whether automatic column derivation is enabled
   * @return the current {@link Query} instance, allowing method chaining
   */
  @Override
  public Query setAutoDerivingColumns(boolean autoDerivingColumns) {
    super.setAutoDerivingColumns(autoDerivingColumns);
    return this;
  }

  /**
   * Sets whether the query should throw an exception if column mapping fails during execution.
   * This method overrides the default behavior inherited from {@link AbstractQuery}.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users",...);
   * query.throwOnMappingFailure(false);
   * }</pre>
   *
   * @param throwOnMappingFailure whether exceptions should be thrown on mapping failures
   * @return the current {@link Query} instance, allowing method chaining
   */
  @Override
  public Query throwOnMappingFailure(boolean throwOnMappingFailure) {
    super.throwOnMappingFailure(throwOnMappingFailure);
    return this;
  }

  /**
   * Adds a custom column-to-property mapping for the query.
   * This method specifies how a column in the result set should map to a property in the target object.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT user_name AS name FROM users",...);
   * query.addColumnMapping("name", "userName");
   * }</pre>
   *
   * @param columnName the name of the column in the result set
   * @param propertyName the name of the property in the target object
   * @return the current {@link Query} instance, allowing method chaining
   */
  @Override
  public Query addColumnMapping(String columnName, String propertyName) {
    super.addColumnMapping(columnName, propertyName);
    return this;
  }

  /**
   * Registers a callback to process the {@link PreparedStatement} before execution.
   * This method allows additional customization or validation of the prepared statement.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "SELECT * FROM users WHERE id = ?",...);
   * query.processStatement(statement -> {
   *   statement.setQueryTimeout(10);
   * });
   * }</pre>
   *
   * @param callback the {@link QueryStatementCallback} to process the statement
   * @return the current {@link Query} instance, allowing method chaining
   */
  @Override
  public Query processStatement(@Nullable QueryStatementCallback callback) {
    super.processStatement(callback);
    return this;
  }

  /**
   * Adds the current query to a batch for batch execution.
   * This method prepares the query for inclusion in a batch operation, which can improve performance
   * when executing multiple similar queries.
   *
   * <p>Example usage:
   * <pre>{@code
   * Query query = new Query(connection, "INSERT INTO users (name) VALUES (?)",...);
   * query.addParameter("John Doe").addToBatch();
   * query.addParameter("Jane Doe").addToBatch();
   * }</pre>
   *
   * @return the current {@link Query} instance, allowing method chaining
   */
  @Override
  public Query addToBatch() {
    super.addToBatch();
    return this;
  }

}
