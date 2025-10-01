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

package infra.jdbc.core.simple;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import infra.jdbc.core.RowMapper;
import infra.jdbc.core.SqlParameter;
import infra.jdbc.core.namedparam.SqlParameterSource;

/**
 * Interface specifying the API for a Simple JDBC Call implemented by {@link SimpleJdbcCall}.
 * This interface is not often used directly, but provides the option to enhance testability,
 * as it can easily be mocked or stubbed.
 *
 * @author Thomas Risberg
 * @author Stephane Nicoll
 * @since 4.0
 */
public interface SimpleJdbcCallOperations {

  /**
   * Specify the procedure name to be used - this implies that we will be calling a stored procedure.
   *
   * @param procedureName the name of the stored procedure
   * @return the instance of this SimpleJdbcCall
   */
  SimpleJdbcCallOperations withProcedureName(String procedureName);

  /**
   * Specify the procedure name to be used - this implies that we will be calling a stored function.
   *
   * @param functionName the name of the stored function
   * @return the instance of this SimpleJdbcCall
   */
  SimpleJdbcCallOperations withFunctionName(String functionName);

  /**
   * Optionally, specify the name of the schema that contins the stored procedure.
   *
   * @param schemaName the name of the schema
   * @return the instance of this SimpleJdbcCall
   */
  SimpleJdbcCallOperations withSchemaName(String schemaName);

  /**
   * Optionally, specify the name of the catalog that contins the stored procedure.
   * <p>To provide consistency with the Oracle DatabaseMetaData, this is used to specify the
   * package name if the procedure is declared as part of a package.
   *
   * @param catalogName the catalog or package name
   * @return the instance of this SimpleJdbcCall
   */
  SimpleJdbcCallOperations withCatalogName(String catalogName);

  /**
   * Indicates the procedure's return value should be included in the results returned.
   *
   * @return the instance of this SimpleJdbcCall
   */
  SimpleJdbcCallOperations withReturnValue();

  /**
   * Specify one or more parameters if desired. These parameters will be supplemented with
   * any parameter information retrieved from the database meta-data.
   * <p>Note that only parameters declared as {@code SqlParameter} and {@code SqlInOutParameter}
   * will be used to provide input values. This is different from the {@code StoredProcedure}
   * class which - for backwards compatibility reasons - allows input values to be provided
   * for parameters declared as {@code SqlOutParameter}.
   *
   * @param sqlParameters the parameters to use
   * @return the instance of this SimpleJdbcCall
   */
  SimpleJdbcCallOperations declareParameters(@Nullable SqlParameter... sqlParameters);

  /** Not used yet. */
  SimpleJdbcCallOperations useInParameterNames(String... inParameterNames);

  /**
   * Used to specify when a ResultSet is returned by the stored procedure and you want it
   * mapped by a {@link RowMapper}. The results will be returned using the parameter name
   * specified. Multiple ResultSets must be declared in the correct order.
   * <p>If the database you are using uses ref cursors then the name specified must match
   * the name of the parameter declared for the procedure in the database.
   *
   * @param parameterName the name of the returned results and/or the name of the ref cursor parameter
   * @param rowMapper the RowMapper implementation that will map the data returned for each row
   */
  SimpleJdbcCallOperations returningResultSet(String parameterName, RowMapper<?> rowMapper);

  /**
   * Turn off any processing of parameter meta-data information obtained via JDBC.
   *
   * @return the instance of this SimpleJdbcCall
   */
  SimpleJdbcCallOperations withoutProcedureColumnMetaDataAccess();

  /**
   * Indicates that parameters should be bound by name.
   *
   * @return the instance of this SimpleJdbcCall
   * @since 4.0
   */
  SimpleJdbcCallOperations withNamedBinding();

  /**
   * Execute the stored function and return the results obtained as an Object of the
   * specified return type.
   *
   * @param returnType the type of the value to return
   * @param args optional array containing the in parameter values to be used in the call.
   * Parameter values must be provided in the same order as the parameters are defined
   * for the stored procedure.
   */
  <T> T executeFunction(Class<T> returnType, Object... args);

  /**
   * Execute the stored function and return the results obtained as an Object of the
   * specified return type.
   *
   * @param returnType the type of the value to return
   * @param args a Map containing the parameter values to be used in the call
   */
  <T> T executeFunction(Class<T> returnType, Map<String, ?> args);

  /**
   * Execute the stored function and return the results obtained as an Object of the
   * specified return type.
   *
   * @param returnType the type of the value to return
   * @param args the MapSqlParameterSource containing the parameter values to be used in the call
   */
  <T> T executeFunction(Class<T> returnType, SqlParameterSource args);

  /**
   * Execute the stored procedure and return the single out parameter as an Object
   * of the specified return type. In the case where there are multiple out parameters,
   * the first one is returned and additional out parameters are ignored.
   *
   * @param returnType the type of the value to return
   * @param args optional array containing the in parameter values to be used in the call.
   * Parameter values must be provided in the same order as the parameters are defined for
   * the stored procedure.
   */
  <T> T executeObject(Class<T> returnType, Object... args);

  /**
   * Execute the stored procedure and return the single out parameter as an Object
   * of the specified return type. In the case where there are multiple out parameters,
   * the first one is returned and additional out parameters are ignored.
   *
   * @param returnType the type of the value to return
   * @param args a Map containing the parameter values to be used in the call
   */
  <T> T executeObject(Class<T> returnType, Map<String, ?> args);

  /**
   * Execute the stored procedure and return the single out parameter as an Object
   * of the specified return type. In the case where there are multiple out parameters,
   * the first one is returned and additional out parameters are ignored.
   *
   * @param returnType the type of the value to return
   * @param args the MapSqlParameterSource containing the parameter values to be used in the call
   */
  <T> T executeObject(Class<T> returnType, SqlParameterSource args);

  /**
   * Execute the stored procedure and return a map of output params, keyed by name
   * as in parameter declarations.
   *
   * @param args optional array containing the in parameter values to be used in the call.
   * Parameter values must be provided in the same order as the parameters are defined for
   * the stored procedure.
   * @return a Map of output params
   */
  Map<String, Object> execute(Object... args);

  /**
   * Execute the stored procedure and return a map of output params, keyed by name
   * as in parameter declarations.
   *
   * @param args a Map containing the parameter values to be used in the call
   * @return a Map of output params
   */
  Map<String, Object> execute(Map<String, ?> args);

  /**
   * Execute the stored procedure and return a map of output params, keyed by name
   * as in parameter declarations.
   *
   * @param args the SqlParameterSource containing the parameter values to be used in the call
   * @return a Map of output params
   */
  Map<String, Object> execute(SqlParameterSource args);

}
