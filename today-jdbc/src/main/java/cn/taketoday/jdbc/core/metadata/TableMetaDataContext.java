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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.jdbc.core.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;
import cn.taketoday.jdbc.core.SqlTypeValue;
import cn.taketoday.jdbc.core.namedparam.SqlParameterSource;
import cn.taketoday.jdbc.core.namedparam.SqlParameterSourceUtils;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;

/**
 * Class to manage context meta-data used for the configuration
 * and execution of operations on a database table.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
public class TableMetaDataContext {
  private static final Logger logger = LoggerFactory.getLogger(TableMetaDataContext.class);

  // Name of table for this context
  @Nullable
  private String tableName;

  // Name of catalog for this context
  @Nullable
  private String catalogName;

  // Name of schema for this context
  @Nullable
  private String schemaName;

  // List of columns objects to be used in this context
  private List<String> tableColumns = new ArrayList<>();

  // Should we access insert parameter meta-data info or not
  private boolean accessTableColumnMetaData = true;

  // Should we override default for including synonyms for meta-data lookups
  private boolean overrideIncludeSynonymsDefault = false;

  // Are we quoting identifiers?
  private boolean quoteIdentifiers = false;

  // The provider of table meta-data
  @Nullable
  private TableMetaDataProvider metaDataProvider;

  // Are we using generated key columns
  private boolean generatedKeyColumnsUsed = false;

  /**
   * Set the name of the table for this context.
   */
  public void setTableName(@Nullable String tableName) {
    this.tableName = tableName;
  }

  /**
   * Get the name of the table for this context.
   */
  @Nullable
  public String getTableName() {
    return this.tableName;
  }

  /**
   * Set the name of the catalog for this context.
   */
  public void setCatalogName(@Nullable String catalogName) {
    this.catalogName = catalogName;
  }

  /**
   * Get the name of the catalog for this context.
   */
  @Nullable
  public String getCatalogName() {
    return this.catalogName;
  }

  /**
   * Set the name of the schema for this context.
   */
  public void setSchemaName(@Nullable String schemaName) {
    this.schemaName = schemaName;
  }

  /**
   * Get the name of the schema for this context.
   */
  @Nullable
  public String getSchemaName() {
    return this.schemaName;
  }

  /**
   * Specify whether we should access table column meta-data.
   */
  public void setAccessTableColumnMetaData(boolean accessTableColumnMetaData) {
    this.accessTableColumnMetaData = accessTableColumnMetaData;
  }

  /**
   * Are we accessing table meta-data?
   */
  public boolean isAccessTableColumnMetaData() {
    return this.accessTableColumnMetaData;
  }

  /**
   * Specify whether we should override default for accessing synonyms.
   */
  public void setOverrideIncludeSynonymsDefault(boolean override) {
    this.overrideIncludeSynonymsDefault = override;
  }

  /**
   * Are we overriding include synonyms default?
   */
  public boolean isOverrideIncludeSynonymsDefault() {
    return this.overrideIncludeSynonymsDefault;
  }

  /**
   * Specify whether we are quoting SQL identifiers.
   * <p>Defaults to {@code false}. If set to {@code true}, the identifier
   * quote string for the underlying database will be used to quote SQL
   * identifiers in generated SQL statements.
   *
   * @param quoteIdentifiers whether identifiers should be quoted
   * @see java.sql.DatabaseMetaData#getIdentifierQuoteString()
   */
  public void setQuoteIdentifiers(boolean quoteIdentifiers) {
    this.quoteIdentifiers = quoteIdentifiers;
  }

  /**
   * Are we quoting identifiers?
   *
   * @see #setQuoteIdentifiers(boolean)
   */
  public boolean isQuoteIdentifiers() {
    return this.quoteIdentifiers;
  }

  /**
   * Get a List of the table column names.
   */
  public List<String> getTableColumns() {
    return this.tableColumns;
  }

  /**
   * Process the current meta-data with the provided configuration options.
   *
   * @param dataSource the DataSource being used
   * @param declaredColumns any columns that are declared
   * @param generatedKeyNames name of generated keys
   */
  public void processMetaData(DataSource dataSource, List<String> declaredColumns, String[] generatedKeyNames) {
    this.metaDataProvider = TableMetaDataProvider.create(dataSource, this);
    this.tableColumns = reconcileColumnsToUse(declaredColumns, generatedKeyNames);
  }

  private TableMetaDataProvider obtainMetaDataProvider() {
    Assert.state(this.metaDataProvider != null, "No TableMetaDataProvider - call processMetaData first");
    return this.metaDataProvider;
  }

  /**
   * Compare columns created from meta-data with declared columns and return a reconciled list.
   *
   * @param declaredColumns declared column names
   * @param generatedKeyNames names of generated key columns
   */
  protected List<String> reconcileColumnsToUse(List<String> declaredColumns, String[] generatedKeyNames) {
    if (generatedKeyNames.length > 0) {
      this.generatedKeyColumnsUsed = true;
    }
    if (!declaredColumns.isEmpty()) {
      return new ArrayList<>(declaredColumns);
    }
    LinkedHashSet<String> keys = new LinkedHashSet<>(generatedKeyNames.length);
    for (String key : generatedKeyNames) {
      keys.add(key.toUpperCase());
    }
    ArrayList<String> columns = new ArrayList<>();
    for (TableParameterMetaData meta : obtainMetaDataProvider().getTableParameterMetaData()) {
      if (!keys.contains(meta.getParameterName().toUpperCase())) {
        columns.add(meta.getParameterName());
      }
    }
    return columns;
  }

  /**
   * Match the provided column names and values with the list of columns used.
   *
   * @param parameterSource the parameter names and values
   */
  public List<Object> matchInParameterValuesWithInsertColumns(SqlParameterSource parameterSource) {
    ArrayList<Object> values = new ArrayList<>();
    // For parameter source lookups we need to provide case-insensitive lookup support since the
    // database meta-data is not necessarily providing case-sensitive column names
    Map<String, String> caseInsensitiveParameterNames =
            SqlParameterSourceUtils.extractCaseInsensitiveParameterNames(parameterSource);
    for (String column : this.tableColumns) {
      if (parameterSource.hasValue(column)) {
        values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, column));
      }
      else {
        String lowerCaseName = column.toLowerCase();
        if (parameterSource.hasValue(lowerCaseName)) {
          values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, lowerCaseName));
        }
        else {
          String propertyName = JdbcUtils.convertUnderscoreNameToPropertyName(column);
          if (parameterSource.hasValue(propertyName)) {
            values.add(SqlParameterSourceUtils.getTypedValue(parameterSource, propertyName));
          }
          else {
            if (caseInsensitiveParameterNames.containsKey(lowerCaseName)) {
              values.add(SqlParameterSourceUtils.getTypedValue(
                      parameterSource, caseInsensitiveParameterNames.get(lowerCaseName)));
            }
            else {
              values.add(null);
            }
          }
        }
      }
    }
    return values;
  }

  /**
   * Match the provided column names and values with the list of columns used.
   *
   * @param inParameters the parameter names and values
   */
  public List<Object> matchInParameterValuesWithInsertColumns(Map<String, ?> inParameters) {
    ArrayList<Object> values = new ArrayList<>(inParameters.size());
    for (String column : this.tableColumns) {
      Object value = inParameters.get(column);
      if (value == null) {
        value = inParameters.get(column.toLowerCase());
        if (value == null) {
          for (Map.Entry<String, ?> entry : inParameters.entrySet()) {
            if (column.equalsIgnoreCase(entry.getKey())) {
              value = entry.getValue();
              break;
            }
          }
        }
      }
      values.add(value);
    }
    return values;
  }

  /**
   * Build the insert string based on configuration and meta-data information.
   *
   * @return the insert string to be used
   */
  public String createInsertString(String... generatedKeyNames) {
    Set<String> keys = new LinkedHashSet<>(generatedKeyNames.length);
    for (String key : generatedKeyNames) {
      keys.add(key.toUpperCase());
    }

    String identifierQuoteString = (isQuoteIdentifiers() ?
                                    obtainMetaDataProvider().getIdentifierQuoteString() : null);
    boolean quoting = StringUtils.hasText(identifierQuoteString);

    StringBuilder insertStatement = new StringBuilder();
    insertStatement.append("INSERT INTO ");

    String schemaName = getSchemaName();
    if (schemaName != null) {
      if (quoting) {
        insertStatement.append(identifierQuoteString);
        insertStatement.append(this.metaDataProvider.schemaNameToUse(schemaName));
        insertStatement.append(identifierQuoteString);
      }
      else {
        insertStatement.append(schemaName);
      }
      insertStatement.append('.');
    }

    String tableName = getTableName();
    if (quoting) {
      insertStatement.append(identifierQuoteString);
      insertStatement.append(this.metaDataProvider.tableNameToUse(tableName));
      insertStatement.append(identifierQuoteString);
    }
    else {
      insertStatement.append(tableName);
    }

    insertStatement.append(" (");
    int columnCount = 0;
    for (String columnName : getTableColumns()) {
      if (!keys.contains(columnName.toUpperCase())) {
        columnCount++;
        if (columnCount > 1) {
          insertStatement.append(", ");
        }
        if (quoting) {
          insertStatement.append(identifierQuoteString);
          insertStatement.append(this.metaDataProvider.columnNameToUse(columnName));
          insertStatement.append(identifierQuoteString);
        }
        else {
          insertStatement.append(columnName);
        }
      }
    }
    insertStatement.append(") VALUES(");
    if (columnCount < 1) {
      if (this.generatedKeyColumnsUsed) {
        if (logger.isDebugEnabled()) {
          logger.debug("Unable to locate non-key columns for table '" +
                  tableName + "' so an empty insert statement is generated");
        }
      }
      else {
        String message = "Unable to locate columns for table '" + tableName
                + "' so an insert statement can't be generated.";
        if (isAccessTableColumnMetaData()) {
          message += " Consider specifying explicit column names -- for example, via SimpleJdbcInsert#usingColumns().";
        }
        throw new InvalidDataAccessApiUsageException(message);
      }
    }
    String params = String.join(", ", Collections.nCopies(columnCount, "?"));
    insertStatement.append(params);
    insertStatement.append(')');
    return insertStatement.toString();
  }

  /**
   * Build the array of {@link java.sql.Types} based on configuration and meta-data information.
   *
   * @return the array of types to be used
   */
  public int[] createInsertTypes() {
    int[] types = new int[getTableColumns().size()];
    List<TableParameterMetaData> parameters = obtainMetaDataProvider().getTableParameterMetaData();
    Map<String, TableParameterMetaData> parameterMap = CollectionUtils.newLinkedHashMap(parameters.size());
    for (TableParameterMetaData tpmd : parameters) {
      parameterMap.put(tpmd.getParameterName().toUpperCase(), tpmd);
    }
    int typeIndx = 0;
    for (String column : getTableColumns()) {
      if (column == null) {
        types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
      }
      else {
        TableParameterMetaData tpmd = parameterMap.get(column.toUpperCase());
        if (tpmd != null) {
          types[typeIndx] = tpmd.getSqlType();
        }
        else {
          types[typeIndx] = SqlTypeValue.TYPE_UNKNOWN;
        }
      }
      typeIndx++;
    }
    return types;
  }

  /**
   * Does this database support the JDBC 3.0 feature of retrieving generated keys:
   * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
   */
  public boolean isGetGeneratedKeysSupported() {
    return obtainMetaDataProvider().isGetGeneratedKeysSupported();
  }

  /**
   * Does this database support simple query to retrieve generated keys
   * when the JDBC 3.0 feature is not supported:
   * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
   */
  public boolean isGetGeneratedKeysSimulated() {
    return obtainMetaDataProvider().isGetGeneratedKeysSimulated();
  }

  /**
   * Does this database support a simple query to retrieve generated keys
   * when the JDBC 3.0 feature is not supported:
   * {@link java.sql.DatabaseMetaData#supportsGetGeneratedKeys()}?
   */
  @Nullable
  public String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName) {
    return obtainMetaDataProvider().getSimpleQueryForGetGeneratedKey(tableName, keyColumnName);
  }

  /**
   * Is a column name String array for retrieving generated keys supported:
   * {@link java.sql.Connection#createStruct(String, Object[])}?
   */
  public boolean isGeneratedKeysColumnNameArraySupported() {
    return obtainMetaDataProvider().isGeneratedKeysColumnNameArraySupported();
  }

}
