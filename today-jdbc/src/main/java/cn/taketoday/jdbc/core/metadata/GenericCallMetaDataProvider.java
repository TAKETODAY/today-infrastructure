/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.core.SqlInOutParameter;
import cn.taketoday.jdbc.core.SqlOutParameter;
import cn.taketoday.jdbc.core.SqlParameter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * A generic implementation of the {@link CallMetaDataProvider} interface.
 * This class can be extended to provide database specific behavior.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class GenericCallMetaDataProvider implements CallMetaDataProvider {

  /** Logger available to subclasses. */
  protected static final Logger logger = LoggerFactory.getLogger(CallMetaDataProvider.class);

  private final String userName;

  private boolean supportsCatalogsInProcedureCalls = true;

  private boolean supportsSchemasInProcedureCalls = true;

  private boolean storesUpperCaseIdentifiers = true;

  private boolean storesLowerCaseIdentifiers = false;

  private boolean procedureColumnMetaDataUsed = false;

  private final ArrayList<CallParameterMetaData> callParameterMetaData = new ArrayList<>();

  /**
   * Constructor used to initialize with provided database meta-data.
   *
   * @param databaseMetaData meta-data to be used
   */
  protected GenericCallMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    this.userName = databaseMetaData.getUserName();
  }

  @Override
  public void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException {
    try {
      setSupportsCatalogsInProcedureCalls(databaseMetaData.supportsCatalogsInProcedureCalls());
    }
    catch (SQLException ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Error retrieving 'DatabaseMetaData.supportsCatalogsInProcedureCalls': " + ex.getMessage());
      }
    }
    try {
      setSupportsSchemasInProcedureCalls(databaseMetaData.supportsSchemasInProcedureCalls());
    }
    catch (SQLException ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Error retrieving 'DatabaseMetaData.supportsSchemasInProcedureCalls': " + ex.getMessage());
      }
    }
    try {
      setStoresUpperCaseIdentifiers(databaseMetaData.storesUpperCaseIdentifiers());
    }
    catch (SQLException ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Error retrieving 'DatabaseMetaData.storesUpperCaseIdentifiers': " + ex.getMessage());
      }
    }
    try {
      setStoresLowerCaseIdentifiers(databaseMetaData.storesLowerCaseIdentifiers());
    }
    catch (SQLException ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Error retrieving 'DatabaseMetaData.storesLowerCaseIdentifiers': " + ex.getMessage());
      }
    }
  }

  @Override
  public void initializeWithProcedureColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
          @Nullable String schemaName, @Nullable String procedureName) throws SQLException {

    this.procedureColumnMetaDataUsed = true;
    processProcedureColumns(databaseMetaData, catalogName, schemaName, procedureName);
  }

  @Override
  public List<CallParameterMetaData> getCallParameterMetaData() {
    return this.callParameterMetaData;
  }

  @Override
  @Nullable
  public String procedureNameToUse(@Nullable String procedureName) {
    return identifierNameToUse(procedureName);
  }

  @Override
  @Nullable
  public String catalogNameToUse(@Nullable String catalogName) {
    return identifierNameToUse(catalogName);
  }

  @Override
  @Nullable
  public String schemaNameToUse(@Nullable String schemaName) {
    return identifierNameToUse(schemaName);
  }

  @Override
  @Nullable
  public String metaDataCatalogNameToUse(@Nullable String catalogName) {
    if (isSupportsCatalogsInProcedureCalls()) {
      return catalogNameToUse(catalogName);
    }
    else {
      return null;
    }
  }

  @Override
  @Nullable
  public String metaDataSchemaNameToUse(@Nullable String schemaName) {
    if (isSupportsSchemasInProcedureCalls()) {
      return schemaNameToUse(schemaName);
    }
    else {
      return null;
    }
  }

  @Override
  @Nullable
  public String parameterNameToUse(@Nullable String parameterName) {
    return identifierNameToUse(parameterName);
  }

  @Override
  public String namedParameterBindingToUse(@Nullable String parameterName) {
    return parameterName + " => ?";
  }

  @Override
  public boolean byPassReturnParameter(String parameterName) {
    return false;
  }

  @Override
  public SqlParameter createDefaultOutParameter(String parameterName, CallParameterMetaData meta) {
    return new SqlOutParameter(parameterName, meta.getSqlType());
  }

  @Override
  public SqlParameter createDefaultInOutParameter(String parameterName, CallParameterMetaData meta) {
    return new SqlInOutParameter(parameterName, meta.getSqlType());
  }

  @Override
  public SqlParameter createDefaultInParameter(String parameterName, CallParameterMetaData meta) {
    return new SqlParameter(parameterName, meta.getSqlType());
  }

  @Override
  public String getUserName() {
    return this.userName;
  }

  @Override
  public boolean isReturnResultSetSupported() {
    return true;
  }

  @Override
  public boolean isRefCursorSupported() {
    return false;
  }

  @Override
  public int getRefCursorSqlType() {
    return Types.OTHER;
  }

  @Override
  public boolean isProcedureColumnMetaDataUsed() {
    return this.procedureColumnMetaDataUsed;
  }

  /**
   * Specify whether the database supports the use of catalog name in procedure calls.
   */
  protected void setSupportsCatalogsInProcedureCalls(boolean supportsCatalogsInProcedureCalls) {
    this.supportsCatalogsInProcedureCalls = supportsCatalogsInProcedureCalls;
  }

  /**
   * Does the database support the use of catalog name in procedure calls?
   */
  @Override
  public boolean isSupportsCatalogsInProcedureCalls() {
    return this.supportsCatalogsInProcedureCalls;
  }

  /**
   * Specify whether the database supports the use of schema name in procedure calls.
   */
  protected void setSupportsSchemasInProcedureCalls(boolean supportsSchemasInProcedureCalls) {
    this.supportsSchemasInProcedureCalls = supportsSchemasInProcedureCalls;
  }

  /**
   * Does the database support the use of schema name in procedure calls?
   */
  @Override
  public boolean isSupportsSchemasInProcedureCalls() {
    return this.supportsSchemasInProcedureCalls;
  }

  /**
   * Specify whether the database uses upper case for identifiers.
   */
  protected void setStoresUpperCaseIdentifiers(boolean storesUpperCaseIdentifiers) {
    this.storesUpperCaseIdentifiers = storesUpperCaseIdentifiers;
  }

  /**
   * Does the database use upper case for identifiers?
   */
  protected boolean isStoresUpperCaseIdentifiers() {
    return this.storesUpperCaseIdentifiers;
  }

  /**
   * Specify whether the database uses lower case for identifiers.
   */
  protected void setStoresLowerCaseIdentifiers(boolean storesLowerCaseIdentifiers) {
    this.storesLowerCaseIdentifiers = storesLowerCaseIdentifiers;
  }

  /**
   * Does the database use lower case for identifiers?
   */
  protected boolean isStoresLowerCaseIdentifiers() {
    return this.storesLowerCaseIdentifiers;
  }

  @Nullable
  private String identifierNameToUse(@Nullable String identifierName) {
    if (identifierName == null) {
      return null;
    }
    else if (isStoresUpperCaseIdentifiers()) {
      return identifierName.toUpperCase();
    }
    else if (isStoresLowerCaseIdentifiers()) {
      return identifierName.toLowerCase();
    }
    else {
      return identifierName;
    }
  }

  /**
   * Process the procedure column meta-data.
   */
  private void processProcedureColumns(DatabaseMetaData databaseMetaData,
          @Nullable String catalogName, @Nullable String schemaName, @Nullable String procedureName) {

    String metaDataCatalogName = metaDataCatalogNameToUse(catalogName);
    String metaDataSchemaName = metaDataSchemaNameToUse(schemaName);
    String metaDataProcedureName = procedureNameToUse(procedureName);

    try {
      String searchStringEscape = databaseMetaData.getSearchStringEscape();
      String escapedSchemaName = escapeNamePattern(metaDataSchemaName, searchStringEscape);
      String escapedProcedureName = escapeNamePattern(metaDataProcedureName, searchStringEscape);
      if (logger.isDebugEnabled()) {
        String schemaInfo = (Objects.equals(escapedSchemaName, metaDataSchemaName)
                             ? metaDataSchemaName : metaDataCatalogName + "(" + escapedSchemaName + ")");
        String procedureInfo = (Objects.equals(escapedProcedureName, metaDataProcedureName)
                                ? metaDataProcedureName : metaDataProcedureName + "(" + escapedProcedureName + ")");
        logger.debug("Retrieving meta-data for " + metaDataCatalogName + '/' +
                schemaInfo + '/' + procedureInfo);
      }

      List<String> found = new ArrayList<>();
      boolean function = false;

      try (ResultSet procedures = databaseMetaData.getProcedures(
              metaDataCatalogName, escapedSchemaName, escapedProcedureName)) {
        while (procedures.next()) {
          found.add(procedures.getString("PROCEDURE_CAT") + '.' + procedures.getString("PROCEDURE_SCHEM") +
                  '.' + procedures.getString("PROCEDURE_NAME"));
        }
      }

      if (found.isEmpty()) {
        // Functions not exposed as procedures anymore on PostgreSQL driver 42.2.11
        try (ResultSet functions = databaseMetaData.getFunctions(
                metaDataCatalogName, escapedSchemaName, escapedProcedureName)) {
          while (functions.next()) {
            found.add(functions.getString("FUNCTION_CAT") + '.' + functions.getString("FUNCTION_SCHEM") +
                    '.' + functions.getString("FUNCTION_NAME"));
            function = true;
          }
        }
      }

      if (found.size() > 1) {
        throw new InvalidDataAccessApiUsageException(
                "Unable to determine the correct call signature - multiple signatures for '" +
                        metaDataProcedureName + "': found " + found + " " + (function ? "functions" : "procedures"));
      }
      else if (found.isEmpty()) {
        if (metaDataProcedureName != null && metaDataProcedureName.contains(".") &&
                !StringUtils.hasText(metaDataCatalogName)) {
          String packageName = metaDataProcedureName.substring(0, metaDataProcedureName.indexOf('.'));
          throw new InvalidDataAccessApiUsageException(
                  "Unable to determine the correct call signature for '" + metaDataProcedureName +
                          "' - package name should be specified separately using '.withCatalogName(\"" +
                          packageName + "\")'");
        }
        else if ("Oracle".equals(databaseMetaData.getDatabaseProductName())) {
          if (logger.isDebugEnabled()) {
            logger.debug("Oracle JDBC driver did not return procedure/function/signature for '{}' - assuming a non-exposed synonym",
                    metaDataProcedureName);
          }
        }
        else {
          throw new InvalidDataAccessApiUsageException(
                  "Unable to determine the correct call signature - no " +
                          "procedure/function/signature for '" + metaDataProcedureName + "'");
        }
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Retrieving column meta-data for {} {}/{}/",
                (function ? "function" : "procedure"), metaDataCatalogName, metaDataSchemaName, metaDataProcedureName);
      }
      try (ResultSet columns = function ?
                               databaseMetaData.getFunctionColumns(metaDataCatalogName, escapedSchemaName, escapedProcedureName, null) :
                               databaseMetaData.getProcedureColumns(metaDataCatalogName, escapedSchemaName, escapedProcedureName, null)) {
        while (columns.next()) {
          String columnName = columns.getString("COLUMN_NAME");
          int columnType = columns.getInt("COLUMN_TYPE");
          if (columnName == null && isInOrOutColumn(columnType, function)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Skipping meta-data for: {} {} {} {} (probably a member of a collection)",
                      columnType, columns.getInt("DATA_TYPE"),
                      columns.getString("TYPE_NAME"), columns.getInt("NULLABLE"));
            }
          }
          else {
            int nullable = (function ? DatabaseMetaData.functionNullable : DatabaseMetaData.procedureNullable);
            CallParameterMetaData meta = new CallParameterMetaData(function, columnName, columnType,
                    columns.getInt("DATA_TYPE"), columns.getString("TYPE_NAME"),
                    columns.getInt("NULLABLE") == nullable);
            this.callParameterMetaData.add(meta);
            if (logger.isDebugEnabled()) {
              logger.debug("Retrieved meta-data: {} {} {} {} {}",
                      meta.getParameterName(), meta.getParameterType(), meta.getSqlType(), meta.getTypeName(), meta.isNullable());
            }
          }
        }
      }
    }
    catch (SQLException ex) {
      if (logger.isWarnEnabled()) {
        logger.warn("Error while retrieving meta-data for procedure columns. " +
                "Consider declaring explicit parameters -- for example, via SimpleJdbcCall#addDeclaredParameter().", ex);
      }
      // Although we could invoke `this.callParameterMetaData.clear()` so that
      // we don't retain a partial list of column names (like we do in
      // GenericTableMetaDataProvider.processTableColumns(...)), we choose
      // not to do that here, since invocation of the stored procedure will
      // likely fail anyway with an incorrect argument list.
    }
  }

  @Nullable
  private static String escapeNamePattern(@Nullable String name, @Nullable String escape) {
    if (name == null || escape == null) {
      return name;
    }
    return name.replace(escape, escape + escape)
            .replace("_", escape + "_")
            .replace("%", escape + "%");
  }

  private static boolean isInOrOutColumn(int columnType, boolean function) {
    if (function) {
      return (columnType == DatabaseMetaData.functionColumnIn ||
              columnType == DatabaseMetaData.functionColumnInOut ||
              columnType == DatabaseMetaData.functionColumnOut);
    }
    else {
      return (columnType == DatabaseMetaData.procedureColumnIn ||
              columnType == DatabaseMetaData.procedureColumnInOut ||
              columnType == DatabaseMetaData.procedureColumnOut);
    }
  }

}
