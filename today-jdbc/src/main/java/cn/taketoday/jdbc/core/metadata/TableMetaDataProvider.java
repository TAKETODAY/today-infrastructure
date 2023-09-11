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

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import cn.taketoday.dao.DataAccessResourceFailureException;
import cn.taketoday.jdbc.support.JdbcUtils;
import cn.taketoday.jdbc.support.MetaDataAccessException;
import cn.taketoday.lang.Nullable;

/**
 * Interface specifying the API to be implemented by a class providing table meta-data.
 * This is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface TableMetaDataProvider {

  /**
   * Initialize using the database meta-data provided.
   *
   * @param databaseMetaData used to retrieve database specific information
   * @throws SQLException in case of initialization failure
   */
  void initializeWithMetaData(DatabaseMetaData databaseMetaData) throws SQLException;

  /**
   * Initialize using provided database meta-data, table and column information.
   * <p>This initialization can be turned off by specifying that column meta-data
   * should not be used.
   *
   * @param databaseMetaData used to retrieve database specific information
   * @param catalogName name of catalog to use (or {@code null} if none)
   * @param schemaName name of schema name to use (or {@code null} if none)
   * @param tableName name of the table
   * @throws SQLException in case of initialization failure
   */
  void initializeWithTableColumnMetaData(DatabaseMetaData databaseMetaData, @Nullable String catalogName,
          @Nullable String schemaName, @Nullable String tableName) throws SQLException;

  /**
   * Get the table name formatted based on meta-data information.
   * <p>This could include altering the case.
   */
  @Nullable
  String tableNameToUse(@Nullable String tableName);

  /**
   * Get the column name formatted based on meta-data information.
   * <p>This could include altering the case.
   */
  @Nullable
  String columnNameToUse(@Nullable String columnName);

  /**
   * Get the catalog name formatted based on meta-data information.
   * <p>This could include altering the case.
   */
  @Nullable
  String catalogNameToUse(@Nullable String catalogName);

  /**
   * Get the schema name formatted based on meta-data information.
   * <p>This could include altering the case.
   */
  @Nullable
  String schemaNameToUse(@Nullable String schemaName);

  /**
   * Provide any modification of the catalog name passed in to match the meta-data
   * currently used.
   * <p>The returned value will be used for meta-data lookups.
   * <p>This could include altering the case used or providing a base catalog
   * if none is provided.
   */
  @Nullable
  String metaDataCatalogNameToUse(@Nullable String catalogName);

  /**
   * Provide any modification of the schema name passed in to match the meta-data
   * currently used.
   * <p>The returned value will be used for meta-data lookups.
   * <p>This could include altering the case used or providing a base schema
   * if none is provided.
   */
  @Nullable
  String metaDataSchemaNameToUse(@Nullable String schemaName);

  /**
   * Are we using the meta-data for the table columns?
   */
  boolean isTableColumnMetaDataUsed();

  /**
   * Does this database support the JDBC feature for retrieving generated keys?
   *
   * @see java.sql.DatabaseMetaData#supportsGetGeneratedKeys()
   */
  boolean isGetGeneratedKeysSupported();

  /**
   * Does this database support a simple query to retrieve generated keys when
   * the JDBC feature for retrieving generated keys is not supported?
   *
   * @see #isGetGeneratedKeysSupported()
   * @see #getSimpleQueryForGetGeneratedKey(String, String)
   */
  boolean isGetGeneratedKeysSimulated();

  /**
   * Get the simple query to retrieve generated keys when the JDBC feature for
   * retrieving generated keys is not supported.
   *
   * @see #isGetGeneratedKeysSimulated()
   */
  @Nullable
  String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName);

  /**
   * Does this database support a column name String array for retrieving generated
   * keys?
   *
   * @see java.sql.Connection#createStruct(String, Object[])
   */
  boolean isGeneratedKeysColumnNameArraySupported();

  /**
   * Get the table parameter meta-data that is currently used.
   *
   * @return a List of {@link TableParameterMetaData}
   */
  List<TableParameterMetaData> getTableParameterMetaData();

  /**
   * Get the string used to quote SQL identifiers.
   * <p>This method returns a space ({@code " "}) if identifier quoting is not
   * supported.
   *
   * @return database identifier quote string
   * @see DatabaseMetaData#getIdentifierQuoteString()
   */
  String getIdentifierQuoteString();

  /**
   * Create a {@link TableMetaDataProvider} based on the database meta-data.
   *
   * @param dataSource used to retrieve meta-data
   * @param context the class that holds configuration and meta-data
   * @return instance of the TableMetaDataProvider implementation to be used
   * @throws DataAccessResourceFailureException Error retrieving database meta-data
   */
  static TableMetaDataProvider create(DataSource dataSource, TableMetaDataContext context) {
    try {
      return JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> {
        String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
        TableMetaDataProvider provider;

        if ("Oracle".equals(databaseProductName)) {
          provider = new OracleTableMetaDataProvider(
                  databaseMetaData, context.isOverrideIncludeSynonymsDefault());
        }
        else if ("PostgreSQL".equals(databaseProductName)) {
          provider = new PostgresTableMetaDataProvider(databaseMetaData);
        }
        else if ("Apache Derby".equals(databaseProductName)) {
          provider = new DerbyTableMetaDataProvider(databaseMetaData);
        }
        else if ("HSQL Database Engine".equals(databaseProductName)) {
          provider = new HsqlTableMetaDataProvider(databaseMetaData);
        }
        else {
          provider = new GenericTableMetaDataProvider(databaseMetaData);
        }

        provider.initializeWithMetaData(databaseMetaData);

        if (context.isAccessTableColumnMetaData()) {
          provider.initializeWithTableColumnMetaData(databaseMetaData,
                  context.getCatalogName(), context.getSchemaName(), context.getTableName());
        }
        return provider;
      });
    }
    catch (MetaDataAccessException ex) {
      throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
    }
  }

}
