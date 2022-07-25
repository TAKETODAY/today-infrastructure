/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.lang.Nullable;

/**
 * Interface specifying the API to be implemented by a class providing call meta-data.
 *
 * <p>This is intended for internal use by Framework's
 * {@link cn.taketoday.jdbc.core.simple.SimpleJdbcCall}.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
public interface CallMetaDataProvider {

  /**
   * Initialize using the provided DatabaseMetData.
   *
   * @param databaseMetaData used to retrieve database specific information
   * @throws SQLException in case of initialization failure
   */
  void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

  /**
   * Initialize the database specific management of procedure column meta-data.
   * This is only called for databases that are supported. This initialization
   * can be turned off by specifying that column meta-data should not be used.
   *
   * @param databaseMetaData used to retrieve database specific information
   * @param catalogName name of catalog to use (or {@code null} if none)
   * @param schemaName name of schema name to use (or {@code null} if none)
   * @param procedureName name of the stored procedure
   * @throws SQLException in case of initialization failure
   * @see cn.taketoday.jdbc.core.simple.SimpleJdbcCall#withoutProcedureColumnMetaDataAccess()
   */
  void initializeWithProcedureColumnMetaData(
          DatabaseMetaData databaseMetaData, @Nullable String catalogName,
          @Nullable String schemaName, @Nullable String procedureName) throws SQLException;

  /**
   * Provide any modification of the procedure name passed in to match the meta-data currently used.
   * This could include altering the case.
   */
  @Nullable
  String procedureNameToUse(@Nullable String procedureName);

  /**
   * Provide any modification of the catalog name passed in to match the meta-data currently used.
   * This could include altering the case.
   */
  @Nullable
  String catalogNameToUse(@Nullable String catalogName);

  /**
   * Provide any modification of the schema name passed in to match the meta-data currently used.
   * This could include altering the case.
   */
  @Nullable
  String schemaNameToUse(@Nullable String schemaName);

  /**
   * Provide any modification of the catalog name passed in to match the meta-data currently used.
   * The returned value will be used for meta-data lookups. This could include altering the case
   * used or providing a base catalog if none is provided.
   */
  @Nullable
  String metaDataCatalogNameToUse(@Nullable String catalogName);

  /**
   * Provide any modification of the schema name passed in to match the meta-data currently used.
   * The returned value will be used for meta-data lookups. This could include altering the case
   * used or providing a base schema if none is provided.
   */
  @Nullable
  String metaDataSchemaNameToUse(@Nullable String schemaName);

  /**
   * Provide any modification of the column name passed in to match the meta-data currently used.
   * This could include altering the case.
   *
   * @param parameterName name of the parameter of column
   */
  @Nullable
  String parameterNameToUse(@Nullable String parameterName);

  /**
   * Create a default out parameter based on the provided meta-data.
   * This is used when no explicit parameter declaration has been made.
   *
   * @param parameterName the name of the parameter
   * @param meta meta-data used for this call
   * @return the configured SqlOutParameter
   */
  SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta);

  /**
   * Create a default in/out parameter based on the provided meta-data.
   * This is used when no explicit parameter declaration has been made.
   *
   * @param parameterName the name of the parameter
   * @param meta meta-data used for this call
   * @return the configured SqlInOutParameter
   */
  SqlParameter createDefaultInOutParameter(String parameterName, CallParameterMetaData meta);

  /**
   * Create a default in parameter based on the provided meta-data.
   * This is used when no explicit parameter declaration has been made.
   *
   * @param parameterName the name of the parameter
   * @param meta meta-data used for this call
   * @return the configured SqlParameter
   */
  SqlParameter createDefaultInParameter(String parameterName, CallParameterMetaData meta);

  /**
   * Get the name of the current user. Useful for meta-data lookups etc.
   *
   * @return current user name from database connection
   */
  @Nullable
  String getUserName();

  /**
   * Does this database support returning ResultSets that should be retrieved with the JDBC call:
   * {@link java.sql.Statement#getResultSet()}?
   */
  boolean isReturnResultSetSupported();

  /**
   * Does this database support returning ResultSets as ref cursors to be retrieved with
   * {@link java.sql.CallableStatement#getObject(int)} for the specified column.
   */
  boolean isRefCursorSupported();

  /**
   * Get the {@link java.sql.Types} type for columns that return ResultSets as ref cursors
   * if this feature is supported.
   */
  int getRefCursorSqlType();

  /**
   * Are we using the meta-data for the procedure columns?
   */
  boolean isProcedureColumnMetaDataUsed();

  /**
   * Should we bypass the return parameter with the specified name.
   * This allows the database specific implementation to skip the processing
   * for specific results returned by the database call.
   */
  boolean byPassReturnParameter(String parameterName);

  /**
   * Get the call parameter meta-data that is currently used.
   *
   * @return a List of {@link CallParameterMetaData}
   */
  List<CallParameterMetaData> getCallParameterMetaData();

  /**
   * Does the database support the use of catalog name in procedure calls?
   */
  boolean isSupportsCatalogsInProcedureCalls();

  /**
   * Does the database support the use of schema name in procedure calls?
   */
  boolean isSupportsSchemasInProcedureCalls();

}
