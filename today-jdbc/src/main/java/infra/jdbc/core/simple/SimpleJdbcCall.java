/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.core.simple;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;

import javax.sql.DataSource;

import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.RowMapper;
import infra.jdbc.core.SqlParameter;
import infra.jdbc.core.namedparam.SqlParameterSource;

/**
 * A SimpleJdbcCall is a multi-threaded, reusable object representing a call
 * to a stored procedure or a stored function. It provides meta-data processing
 * to simplify the code needed to access basic stored procedures/functions.
 * All you need to provide is the name of the procedure/function and a Map
 * containing the parameters when you execute the call. The names of the
 * supplied parameters will be matched up with in and out parameters declared
 * when the stored procedure was created.
 *
 * <p>The meta-data processing is based on the DatabaseMetaData provided by
 * the JDBC driver. Since we rely on the JDBC driver, this "auto-detection"
 * can only be used for databases that are known to provide accurate meta-data.
 * These currently include Derby, MySQL, Microsoft SQL Server, Oracle, DB2,
 * Sybase and PostgreSQL. For any other databases you are required to declare
 * all parameters explicitly. You can of course declare all parameters
 * explicitly even if the database provides the necessary meta-data. In that
 * case your declared parameters will take precedence. You can also turn off
 * any meta-data processing if you want to use parameter names that do not
 * match what is declared during the stored procedure compilation.
 *
 * <p>The actual insert is being handled using Framework's {@link JdbcTemplate}.
 *
 * <p>Many of the configuration methods return the current instance of the
 * SimpleJdbcCall in order to provide the ability to chain multiple ones
 * together in a "fluent" interface style.
 *
 * @author Thomas Risberg
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see java.sql.DatabaseMetaData
 * @see JdbcTemplate
 * @since 4.0
 */
public class SimpleJdbcCall extends AbstractJdbcCall implements SimpleJdbcCallOperations {

  /**
   * Constructor that takes one parameter with the JDBC DataSource to use when
   * creating the underlying JdbcTemplate.
   *
   * @param dataSource the {@code DataSource} to use
   * @see JdbcTemplate#setDataSource
   */
  public SimpleJdbcCall(DataSource dataSource) {
    super(dataSource);
  }

  /**
   * Alternative Constructor that takes one parameter with the JdbcTemplate to be used.
   *
   * @param jdbcTemplate the {@code JdbcTemplate} to use
   * @see JdbcTemplate#setDataSource
   */
  public SimpleJdbcCall(JdbcTemplate jdbcTemplate) {
    super(jdbcTemplate);
  }

  @Override
  public SimpleJdbcCall withProcedureName(String procedureName) {
    setProcedureName(procedureName);
    setFunction(false);
    return this;
  }

  @Override
  public SimpleJdbcCall withFunctionName(String functionName) {
    setProcedureName(functionName);
    setFunction(true);
    return this;
  }

  @Override
  public SimpleJdbcCall withSchemaName(String schemaName) {
    setSchemaName(schemaName);
    return this;
  }

  @Override
  public SimpleJdbcCall withCatalogName(String catalogName) {
    setCatalogName(catalogName);
    return this;
  }

  @Override
  public SimpleJdbcCall withReturnValue() {
    setReturnValueRequired(true);
    return this;
  }

  @Override
  public SimpleJdbcCall declareParameters(@Nullable SqlParameter... sqlParameters) {
    for (SqlParameter sqlParameter : sqlParameters) {
      if (sqlParameter != null) {
        addDeclaredParameter(sqlParameter);
      }
    }
    return this;
  }

  @Override
  public SimpleJdbcCall useInParameterNames(String... inParameterNames) {
    setInParameterNames(new LinkedHashSet<>(Arrays.asList(inParameterNames)));
    return this;
  }

  @Override
  public SimpleJdbcCall returningResultSet(String parameterName, RowMapper<?> rowMapper) {
    addDeclaredRowMapper(parameterName, rowMapper);
    return this;
  }

  @Override
  public SimpleJdbcCall withoutProcedureColumnMetaDataAccess() {
    setAccessCallParameterMetaData(false);
    return this;
  }

  @Override
  public SimpleJdbcCall withNamedBinding() {
    setNamedBinding(true);
    return this;
  }

  @Override
  @SuppressWarnings({ "unchecked", "NullAway" })
  public <T> T executeFunction(Class<T> returnType, Object... args) {
    return (T) doExecute(args).get(getScalarOutParameterName());
  }

  @Override
  @SuppressWarnings({ "unchecked", "NullAway" })
  public <T> T executeFunction(Class<T> returnType, Map<String, ?> args) {
    return (T) doExecute(args).get(getScalarOutParameterName());
  }

  @Override
  @SuppressWarnings({ "unchecked", "NullAway" })
  public <T> T executeFunction(Class<T> returnType, SqlParameterSource args) {
    return (T) doExecute(args).get(getScalarOutParameterName());
  }

  @Override
  @SuppressWarnings({ "unchecked", "NullAway" })
  public <T> T executeObject(Class<T> returnType, Object... args) {
    return (T) doExecute(args).get(getScalarOutParameterName());
  }

  @Override
  @SuppressWarnings({ "unchecked", "NullAway" })
  public <T> T executeObject(Class<T> returnType, Map<String, ?> args) {
    return (T) doExecute(args).get(getScalarOutParameterName());
  }

  @Override
  @SuppressWarnings({ "unchecked", "NullAway" })
  public <T> T executeObject(Class<T> returnType, SqlParameterSource args) {
    return (T) doExecute(args).get(getScalarOutParameterName());
  }

  @Override
  public Map<String, Object> execute(Object... args) {
    return doExecute(args);
  }

  @Override
  public Map<String, Object> execute(Map<String, ?> args) {
    return doExecute(args);
  }

  @Override
  public Map<String, Object> execute(SqlParameterSource parameterSource) {
    return doExecute(parameterSource);
  }

}
