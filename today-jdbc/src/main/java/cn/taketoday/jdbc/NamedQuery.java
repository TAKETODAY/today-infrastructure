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

import java.io.InputStream;
import java.lang.reflect.Array;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.beans.BeanMetadata;
import cn.taketoday.beans.BeanProperty;
import cn.taketoday.jdbc.parsing.QueryParameter;
import cn.taketoday.jdbc.type.TypeHandler;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * Represents a sql statement.
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

  public NamedQuery(JdbcConnection connection, String queryText, String[] columnNames) {
    this(connection, queryText, false, columnNames);
  }

  private NamedQuery(JdbcConnection connection, String queryText, boolean generatedKeys, String[] columnNames) {
    super(connection, queryText, generatedKeys, columnNames);
    RepositoryManager manager = connection.getManager();
    setColumnMappings(manager.getDefaultColumnMappings());
    this.parsedQuery = manager.parse(queryText, queryParameters);
  }

  public HashMap<String, QueryParameter> getQueryParameters() {
    return queryParameters;
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

  public void addParameter(String name, ParameterBinder parameterBinder) {
    QueryParameter queryParameter = queryParameters.get(name);
    if (queryParameter == null) {
      throw new PersistenceException(
              "Failed to add parameter with name '"
                      + name + "'. No parameter with that name is declared in the sql.");
    }
    queryParameter.setSetter(parameterBinder);
  }

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

  public NamedQuery withParams(Object... paramValues) {
    int i = 0;
    for (Object paramValue : paramValues) {
      addParameter("p" + (++i), paramValue);
    }
    return this;
  }

  @SuppressWarnings("unchecked")
  public NamedQuery addParameter(String name, @Nullable Object value) {
    return value == null
           ? addNullParameter(name)
           : addParameter(name, (Class<Object>) value.getClass(), value);
  }

  public NamedQuery addNullParameter(String name) {
    addParameter(name, ParameterBinder.null_binder);
    return this;
  }

  public NamedQuery addParameter(String name, InputStream value) {
    addParameter(name, ParameterBinder.forBinaryStream(value));
    return this;
  }

  public NamedQuery addParameter(String name, int value) {
    addParameter(name, ParameterBinder.forInt(value));
    return this;
  }

  public NamedQuery addParameter(String name, long value) {
    addParameter(name, ParameterBinder.forLong(value));
    return this;
  }

  public NamedQuery addParameter(String name, String value) {
    addParameter(name, ParameterBinder.forString(value));
    return this;
  }

  public NamedQuery addParameter(String name, boolean value) {
    addParameter(name, ParameterBinder.forBoolean(value));
    return this;
  }

  public NamedQuery addParameter(String name, LocalDateTime value) {
    addParameter(name, ParameterBinder.forTimestamp(Timestamp.valueOf(value)));
    return this;
  }

  public NamedQuery addParameter(String name, LocalDate value) {
    addParameter(name, ParameterBinder.forDate(Date.valueOf(value)));
    return this;
  }

  public NamedQuery addParameter(String name, LocalTime value) {
    addParameter(name, ParameterBinder.forTime(Time.valueOf(value)));
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
   * add map of parameters
   *
   * @param parameters map of parameters
   * @see #addParameter(String, Object)
   */
  public NamedQuery addParameters(Map<String, Object> parameters) {
    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
      addParameter(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * Set an array parameter.<br>
   * See {@link #addParameters(String, Object...)} for details
   */
  public NamedQuery addParameter(String name, Collection<?> values) {
    addParameter(name, new ArrayParameterBinder(values));
    this.hasArrayParameter = true;
    return this;
  }

  @SuppressWarnings("unchecked")
  public NamedQuery bind(Object pojo) {
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
