/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.jdbc;

import java.io.InputStream;
import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.jdbc.parsing.QueryParameter;
import infra.jdbc.type.TypeHandler;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * A {@code NamedQuery} represents a parameterized SQL query with named parameters.
 * It extends {@link AbstractQuery} and provides methods to add parameters, bind objects,
 * and execute the query. The query text is parsed to identify named parameters, which
 * are then replaced with actual values when the query is executed.
 *
 * <p>This class supports various types of parameters, including primitive types, arrays,
 * collections, and POJOs. It also provides methods to configure query behavior, such as
 * case sensitivity, auto-deriving columns, and handling mapping failures.</p>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * <p>1. Creating a query with named parameters and executing it:</p>
 * <pre>{@code
 *   NamedQuery query = connection.createNamedQuery(
 *       "SELECT * FROM users WHERE age > :age AND status = :status"
 *   );
 *   query.addParameter("age", 25)
 *        .addParameter("status", "active")
 *        .fetch(User.class);
 * }</pre>
 *
 * <p>2. Using array parameters for IN clauses:</p>
 * <pre>{@code
 *   NamedQuery query = connection.createNamedQuery(
 *       "SELECT * FROM users WHERE id IN(:ids)"
 *   );
 *   query.addParameters("ids", 1, 2, 3)
 *        .fetch(User.class);
 * }</pre>
 *
 * <p>3. Binding a POJO to a query:</p>
 * <pre>{@code
 *   UserFilter filter = new UserFilter();
 *   filter.setAge(30);
 *   filter.setStatus("inactive");
 *
 *   NamedQuery query = connection.createNamedQuery(
 *       "SELECT * FROM users WHERE age > :age AND status = :status"
 *   );
 *   query.bind(filter)
 *        .fetch(User.class);
 * }</pre>
 *
 * <p>4. Adding multiple parameters using a map:</p>
 * <pre>{@code
 *   Map<String, Object> params = new HashMap<>();
 *   params.put("age", 25);
 *   params.put("status", "active");
 *
 *   NamedQuery query = connection.createNamedQuery(
 *       "SELECT * FROM users WHERE age > :age AND status = :status"
 *   );
 *   query.addParameters(params)
 *        .fetch(User.class);
 * }</pre>
 *
 * <p>5. Configuring query behavior:</p>
 * <pre>{@code
 *   NamedQuery query = connection.createNamedQuery(
 *       "SELECT * FROM users WHERE age > :age"
 *   );
 *   query.setCaseSensitive(true)
 *        .setAutoDerivingColumns(false)
 *        .throwOnMappingFailure(true)
 *        .addParameter("age", 25)
 *        .fetch(User.class);
 * }</pre>
 *
 * <p><b>Note:</b> Array parameters are not compatible with batch execution mode because
 * the generated SQL depends on the number of elements in the array. If an array parameter
 * is empty, it will be treated as {@code NULL} in the query.</p>
 *
 * <p><b>Thread Safety:</b> Instances of this class are not thread-safe. Each thread should
 * use its own instance of {@code NamedQuery}.</p>
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public final class NamedQuery extends AbstractQuery {

  private static final Logger log = LoggerFactory.getLogger(NamedQuery.class);

  /** parameter name to parameter index and setter */
  private final HashMap<String, QueryParameter> queryParameters = new HashMap<>();

  private String parsedQuery;

  private boolean hasArrayParameter = false;

  public NamedQuery(JdbcConnection connection, String queryText, boolean generatedKeys) {
    this(connection, queryText, generatedKeys, null);
  }

  public NamedQuery(JdbcConnection connection, String queryText, @Nullable String[] columnNames) {
    this(connection, queryText, false, columnNames);
  }

  private NamedQuery(JdbcConnection connection, String queryText, boolean generatedKeys, @Nullable String[] columnNames) {
    super(connection, queryText, generatedKeys, columnNames);
    RepositoryManager manager = connection.getManager();
    setColumnMappings(manager.getDefaultColumnMappings());
    this.parsedQuery = manager.parse(queryText, queryParameters);
  }

  @Override
  public NamedQuery setCaseSensitive(boolean caseSensitive) {
    super.setCaseSensitive(caseSensitive);
    return this;
  }

  @Override
  public NamedQuery setAutoDerivingColumns(boolean autoDerivingColumns) {
    super.setAutoDerivingColumns(autoDerivingColumns);
    return this;
  }

  @Override
  public NamedQuery throwOnMappingFailure(boolean throwOnMappingFailure) {
    super.throwOnMappingFailure(throwOnMappingFailure);
    return this;
  }

  //---------------------------------------------------------------------
  // Add Parameters
  //---------------------------------------------------------------------

  /**
   * Adds a parameter to the query by associating a {@link ParameterBinder} with the specified parameter name.
   * If the parameter name is not declared in the SQL query, a {@link PersistenceException} is thrown.
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   * NamedQuery query = new NamedQuery(connection, "SELECT * FROM users WHERE age > :age", false);
   * query.addParameter("age", (statement, index) -> statement.setInt(index, 25));
   * }</pre>
   *
   * <p>In this example, the parameter named "age" is associated with a binder that sets its value to 25.</p>
   *
   * <p><b>Note:</b> Ensure that the parameter name exists in the SQL query; otherwise, a
   * {@link PersistenceException} will be thrown.</p>
   *
   * @param name the name of the parameter to be added. This name must match a parameter declared
   * in the SQL query.
   * @param parameterBinder the {@link ParameterBinder} instance responsible for setting the parameter value
   * in the query.
   */
  public void addParameter(String name, ParameterBinder parameterBinder) {
    QueryParameter queryParameter = queryParameters.get(name);
    if (queryParameter == null) {
      throw new PersistenceException("Failed to add parameter with name '%s'. No parameter with that name is declared in the sql."
              .formatted(name));
    }
    queryParameter.setSetter(parameterBinder);
  }

  /**
   * Adds a parameter to the query with the specified name, type, and value.
   * The method handles different types of parameters, including arrays and collections,
   * by delegating to appropriate internal methods. If the parameter is an array or a collection,
   * it is treated as a multi-valued parameter (e.g., for use in SQL `IN` clauses).
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   * NamedQuery query = new NamedQuery(connection, "SELECT * FROM users WHERE age > :age", false);
   * query.addParameter("age", Integer.class, 25);
   * }</pre>
   *
   * <p>In this example, the parameter named "age" is associated with the value 25 using the {@code Integer} type handler.</p>
   *
   * <p>For array or collection parameters:</p>
   *
   * <pre>
   * NamedQuery query = new NamedQuery(connection, "SELECT * FROM users WHERE id IN (:ids)", false);
   * query.addParameter("ids", Integer[].class, new Integer[] {1, 2, 3});
   * </pre>
   *
   * <p>This will generate a query like {@code SELECT * FROM users WHERE id IN (1, 2, 3)}.</p>
   *
   * <p><b>Note:</b> Ensure that the parameter name exists in the SQL query; otherwise, a
   * {@link PersistenceException} will be thrown.</p>
   *
   * @param <T> the type of the parameter value
   * @param name the name of the parameter to be added; must match a parameter declared in the SQL query
   * @param parameterClass the class of the parameter value, used to determine the appropriate type handler
   * @param value the value of the parameter; can be a single value, an array, or a collection
   * @return the current {@link NamedQuery} instance, allowing method chaining
   */
  public <T> NamedQuery addParameter(String name, Class<T> parameterClass, T value) {
    if (parameterClass.isArray()
            // byte[] is used for blob already
            && parameterClass != byte[].class) {
      return addParameters(name, toObjectArray(value));
    }

    if (Collection.class.isAssignableFrom(parameterClass)) {
      return addParameter(name, (Collection<?>) value);
    }
    TypeHandler<T> typeHandler = getTypeHandlerManager().getTypeHandler(parameterClass);
    final class TypeHandlerParameterBinder extends ParameterBinder {
      @Override
      public void bind(PreparedStatement statement, int paramIdx) throws SQLException {
        typeHandler.setParameter(statement, paramIdx, value);
      }
    }
    addParameter(name, new TypeHandlerParameterBinder());
    return this;
  }

  /**
   * Adds multiple parameters to the query using positional naming (e.g., "p1", "p2", etc.).
   * Each parameter value is associated with a generated name in the order they are provided.
   * This method internally calls {@link #addParameter(String, Object)} for each value.
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   * NamedQuery query = new NamedQuery(connection, "SELECT * FROM users WHERE age > :p1 AND name = :p2", false);
   * query.withParams(25, "John");
   * }</pre>
   *
   * <p>In this example, the first parameter ("p1") is set to 25, and the second parameter ("p2") is set to "John".</p>
   *
   * <p>For array or collection values:</p>
   *
   * <pre>{@code
   * NamedQuery query = new NamedQuery(connection, "SELECT * FROM users WHERE id IN (:p1)", false);
   * query.withParams(new Integer[] {1, 2, 3});
   * }</pre>
   *
   * <p>This will generate a query like {@code SELECT * FROM users WHERE id IN (1, 2, 3)}.</p>
   *
   * <p><b>Note:</b> Ensure that the number of parameters matches the placeholders in the SQL query;
   * otherwise, a {@link PersistenceException} may be thrown during execution.</p>
   *
   * @param paramValues a varargs array of parameter values to be added to the query.
   * Each value is assigned a generated name (e.g., "p1", "p2", etc.).
   * @return the current {@link NamedQuery} instance, allowing method chaining.
   */
  public NamedQuery withParams(Object... paramValues) {
    int i = 0;
    for (Object paramValue : paramValues) {
      addParameter("p" + (++i), paramValue);
    }
    return this;
  }

  /**
   * Adds a parameter to the query with the specified name and value. If the value is {@code null},
   * the method delegates to {@link #addNullParameter(String)}. Otherwise, it determines the type
   * of the value and delegates to {@link #addParameter(String, Class, Object)}.
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   * NamedQuery query = new NamedQuery(connection, "SELECT * FROM users WHERE age > :age", false);
   * query.addParameter("age", 25);
   * }</pre>
   *
   * <p>In this example, the parameter named "age" is associated with the value 25.</p>
   *
   * <p>For {@code null} values:</p>
   *
   * <pre>{@code
   * NamedQuery query = new NamedQuery(connection, "SELECT * FROM users WHERE name = :name", false);
   * query.addParameter("name", null);
   * }</pre>
   *
   * <p>This will bind {@code null} to the parameter named "name".</p>
   *
   * <p><b>Note:</b> Ensure that the parameter name exists in the SQL query; otherwise, a
   * {@link PersistenceException} will be thrown.</p>
   *
   * @param name the name of the parameter to be added; must match a parameter declared in the SQL query
   * @param value the value of the parameter; can be {@code null}
   * @return the current {@link NamedQuery} instance, allowing method chaining
   */
  @SuppressWarnings("unchecked")
  public NamedQuery addParameter(String name, @Nullable Object value) {
    return value == null
            ? addNullParameter(name)
            : addParameter(name, (Class<Object>) value.getClass(), value);
  }

  /**
   * Adds a null parameter to the named query with the specified name.
   * This method is useful when you need to bind a null value to a parameter
   * in a query. The {@code ParameterBinder.null_binder} is used internally
   * to handle the null binding.
   *
   * <p>Example usage:
   * <pre>{@code
   * NamedQuery query = new NamedQuery(...);
   * query.addNullParameter("status")
   *      .addNullParameter("category");
   * }</pre>
   *
   * @param name the name of the parameter to be added; must not be null
   * @return the current {@code NamedQuery} instance, allowing method chaining
   */
  public NamedQuery addNullParameter(String name) {
    addParameter(name, ParameterBinder.null_binder);
    return this;
  }

  /**
   * Adds a parameter to the named query, binding it to the provided binary stream.
   * This method is useful when dealing with large binary data, such as files or
   * images, that need to be passed as query parameters.
   *
   * <p>Example usage:
   * <pre>{@code
   * NamedQuery query = new NamedQuery(...);
   * InputStream dataStream = new FileInputStream("example.bin");
   * query.addParameter("binaryData", dataStream);
   * }</pre>
   *
   * <p>In this example, the parameter "binaryData" is bound to the binary stream
   * from the file "example.bin". The returned {@code NamedQuery} instance allows
   * for method chaining to add additional parameters or execute the query.
   *
   * @param name the name of the parameter to be added; must not be null
   * @param value the binary stream representing the parameter's value; must not be null
   * @return the current {@code NamedQuery} instance, enabling method chaining
   */
  public NamedQuery addParameter(String name, InputStream value) {
    addParameter(name, ParameterBinder.forBinaryStream(value));
    return this;
  }

  /**
   * Adds a named integer parameter to the query and returns the current
   * {@code NamedQuery} instance for method chaining.
   *
   * This method is useful when constructing queries with named parameters.
   * The provided name is associated with the given integer value, which will
   * be bound to the query during execution.
   *
   * Example usage:
   *
   * NamedQuery query = new NamedQuery();
   * query.addParameter("age", 25)
   * .addParameter("limit", 100);
   *
   * // The above code creates a query with two parameters:
   * // "age" bound to 25 and "limit" bound to 100.
   *
   * @param name the name of the parameter to be added; must not be null
   * @param value the integer value to bind to the parameter
   * @return the current {@code NamedQuery} instance, enabling method chaining
   */
  public NamedQuery addParameter(String name, int value) {
    addParameter(name, ParameterBinder.forInt(value));
    return this;
  }

  /**
   * Adds a named parameter with a long value to the current query.
   * This method is used to bind a long value to a named parameter,
   * which can then be utilized in the query execution.
   *
   * <p>Example usage:
   * <pre>{@code
   * NamedQuery query = new NamedQuery(...);
   * query.addParameter("age", 25L)
   *      .addParameter("salary", 50000L);
   * }</pre>
   *
   * @param name the name of the parameter to be added; must not be null
   * @param value the long value to bind to the parameter
   * @return the current {@code NamedQuery} instance, enabling method chaining
   */
  public NamedQuery addParameter(String name, long value) {
    addParameter(name, ParameterBinder.forLong(value));
    return this;
  }

  /**
   * Adds a named parameter with a string value to the query and returns the
   * current {@code NamedQuery} instance for method chaining.
   *
   * <p>This method internally uses {@code ParameterBinder.forString(value)} to
   * bind the provided string value to the parameter. It is useful when building
   * queries dynamically with named parameters.</p>
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   * NamedQuery query = new NamedQuery(...);
   * query.addParameter("username", "john_doe")
   *      .addParameter("status", "active");
   * }</pre>
   *
   * @param name the name of the parameter to be added; must not be null or empty
   * @param value the string value to bind to the parameter; can be null
   * @return the current {@code NamedQuery} instance, allowing for method chaining
   */
  public NamedQuery addParameter(String name, String value) {
    addParameter(name, ParameterBinder.forString(value));
    return this;
  }

  /**
   * Adds a named boolean parameter to the query and returns the current
   * {@code NamedQuery} instance for method chaining.
   *
   * <p>This method internally uses {@code ParameterBinder.forBoolean(value)}
   * to bind the boolean value to the parameter name.</p>
   *
   * <p>Example usage:</p>
   *
   * <pre>{@code
   * NamedQuery query = new NamedQuery();
   * query.addParameter("isActive", true)
   *      .addParameter("isDeleted", false);
   * }</pre>
   *
   * @param name the name of the parameter to be added; must not be null
   * @param value the boolean value to bind to the parameter
   * @return the current {@code NamedQuery} instance for method chaining
   */
  public NamedQuery addParameter(String name, boolean value) {
    addParameter(name, ParameterBinder.forBoolean(value));
    return this;
  }

  /**
   * Adds a named parameter with a {@link LocalDateTime} value to the query.
   * This method binds the provided value to the specified parameter name
   * using an appropriate binder internally. The method supports fluent
   * chaining for adding multiple parameters.
   *
   * <p>Example usage:
   * <pre>{@code
   * NamedQuery query = new NamedQuery();
   * query.addParameter("startDate", LocalDateTime.of(2023, 1, 1, 0, 0))
   *      .addParameter("endDate", LocalDateTime.of(2023, 12, 31, 23, 59));
   * }</pre>
   *
   * @param name the name of the parameter to be added; must not be null
   * @param value the {@link LocalDateTime} value to bind to the parameter;
   * can be null if the parameter allows null values
   * @return the current {@link NamedQuery} instance, allowing for method
   * chaining to add additional parameters
   */
  public NamedQuery addParameter(String name, LocalDateTime value) {
    addParameter(name, ParameterBinder.forObject(value));
    return this;
  }

  /**
   * Adds a named parameter with a {@link LocalDate} value to the query.
   * This method binds the provided name and value using an internal parameter binder,
   * allowing the query to utilize the parameter during execution.
   *
   * <p>Example usage:
   * <pre>{@code
   * NamedQuery query = new NamedQuery();
   * query.addParameter("startDate", LocalDate.of(2023, 1, 1))
   *      .addParameter("endDate", LocalDate.of(2023, 12, 31));
   * }</pre>
   *
   * @param name the name of the parameter to be added; must not be null
   * @param value the {@link LocalDate} value of the parameter; must not be null
   * @return the current {@link NamedQuery} instance, enabling method chaining
   */
  public NamedQuery addParameter(String name, LocalDate value) {
    addParameter(name, ParameterBinder.forObject(value));
    return this;
  }

  /**
   * Adds a parameter with the specified name and {@link LocalTime} value to this query.
   * The method internally uses a parameter binder to bind the given value to the query.
   * This allows for type-safe parameter handling and ensures proper formatting when
   * executing the query.
   *
   * <p>Example usage:
   * <pre>{@code
   * NamedQuery query = new NamedQuery();
   * query.addParameter("startTime", LocalTime.of(9, 0))
   *      .addParameter("endTime", LocalTime.of(17, 0));
   * }</pre>
   *
   * @param name the name of the parameter to be added; must not be null or empty
   * @param value the {@link LocalTime} value of the parameter; must not be null
   * @return this {@link NamedQuery} instance, allowing for method chaining
   */
  public NamedQuery addParameter(String name, LocalTime value) {
    addParameter(name, ParameterBinder.forObject(value));
    return this;
  }

  /**
   * Set an array parameter.<br>
   * For example: <pre>
   *     createNamedQuery("SELECT * FROM user WHERE id IN(:ids)")
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
  public NamedQuery addParameters(String name, Object... values) {
    addParameter(name, new ArrayParameterBinder(values));
    this.hasArrayParameter = true;
    return this;
  }

  /**
   * Adds multiple parameters to the current {@code NamedQuery} instance
   * using a map of key-value pairs. Each entry in the map corresponds to
   * a parameter name and its associated value.
   *
   * <p>This method iterates over the provided map and delegates the addition
   * of each parameter to the {@code addParameter(String, Object)} method.
   * After processing all entries, it returns the current instance to allow
   * method chaining.</p>
   *
   * <p><b>Example Usage:</b></p>
   * <pre>{@code
   * NamedQuery query = new NamedQuery(...);
   * Map<String, Object> params = new HashMap<>();
   * params.put("id", 123);
   * params.put("status", "active");
   *
   * query.addParameters(params);
   * }</pre>
   *
   * @param parameters a map containing parameter names as keys and their
   * corresponding values. Must not be null.
   * @return the current {@code NamedQuery} instance, enabling method chaining.
   */
  public NamedQuery addParameters(Map<String, Object> parameters) {
    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      addParameter(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Adds a parameter with a collection of values to the current query.
   * This method is useful when you need to bind multiple values to a single
   * parameter, typically for operations like "IN" clauses in SQL queries.
   * <p>
   * Example usage:
   * <pre>{@code
   * NamedQuery query = new NamedQuery(...);
   * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
   * query.addParameter("names", names);
   * }</pre>
   * In this example, the parameter "names" is bound to a list of strings.
   * The resulting query can use this parameter for operations that require
   * multiple values.
   *
   * @param name the name of the parameter to be added; must not be null
   * @param values the collection of values to bind to the parameter;
   * must not be null or empty
   * @return the current {@link NamedQuery} instance, allowing for method
   * chaining to add more parameters or execute the query
   */
  public NamedQuery addParameter(String name, Collection<?> values) {
    addParameter(name, new ArrayParameterBinder(values));
    this.hasArrayParameter = true;
    return this;
  }

  /**
   * Binds the properties of the given POJO (Plain Old Java Object) to the query parameters
   * defined in this {@code NamedQuery} instance. The method iterates over the properties
   * of the provided POJO and matches them with the query parameters by name. If a match
   * is found, the property's value is added as a parameter to the query.
   *
   * <p>Example usage:
   * <pre>{@code
   * NamedQuery query = new NamedQuery("SELECT * FROM users WHERE name = :name AND age = :age",...);
   * Person person = new Person("John Doe", 30);
   * query.bind(person);
   *
   * // After binding, the query will have parameters:
   * // "name" -> "John Doe"
   * // "age" -> 30
   * }</pre>
   *
   * @param pojo the Plain Old Java Object whose properties are to be bound to the query
   * parameters. Must not be null.
   * @return this {@code NamedQuery} instance, allowing for method chaining.
   * @see BeanMetadata#forInstance(Object)
   * @see QueryParameter
   */
  @SuppressWarnings("unchecked")
  public NamedQuery bind(Object pojo) {
    HashMap<String, QueryParameter> queryParameters = this.queryParameters;
    for (BeanProperty property : BeanMetadata.forInstance(pojo)) {
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

  //---------------------------------------------------------------------
  // Execute
  //---------------------------------------------------------------------

  @Override
  protected String getQuerySQL(boolean allowArrayParameters) {
    if (hasArrayParameter) {
      // array parameter handling
      this.parsedQuery = ArrayParameters.updateQueryAndParametersIndexes(
              parsedQuery,
              queryParameters,
              allowArrayParameters
      );
    }
    return parsedQuery;
  }

  @Override
  protected void postProcessStatement(PreparedStatement statement) {
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
  }

  @Override
  public NamedQuery addColumnMapping(String columnName, String propertyName) {
    super.addColumnMapping(columnName, propertyName);
    return this;
  }

  /**
   * add a Statement processor when {@link  #buildStatement() build a PreparedStatement}
   */
  @Override
  public NamedQuery processStatement(@Nullable QueryStatementCallback callback) {
    super.processStatement(callback);
    return this;
  }

  @Override
  public NamedQuery addToBatch() {
    super.addToBatch();
    return this;
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
        statement.setObject(paramIdx, null);
      }
      else {
        TypeHandler<Object> typeHandler = getTypeHandlerManager().getUnknownTypeHandler();
        for (Object value : values) {
          typeHandler.setParameter(statement, paramIdx++, value);
        }
      }
    }
  }

}
