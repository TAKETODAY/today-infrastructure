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

import cn.taketoday.lang.Nullable;

/**
 * Interface specifying the API to be implemented by a class providing table meta-data.
 * This is intended for internal use by the Simple JDBC classes.
 *
 * @author Thomas Risberg
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
   * This initialization can be turned off by specifying that column meta-data should not be used.
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
   * This could include altering the case.
   */
  @Nullable
  String tableNameToUse(@Nullable String tableName);

  /**
   * Get the catalog name formatted based on meta-data information.
   * This could include altering the case.
   */
  @Nullable
  String catalogNameToUse(@Nullable String catalogName);

  /**
   * Get the schema name formatted based on meta-data information.
   * This could include altering the case.
   */
  @Nullable
  String schemaNameToUse(@Nullable String schemaName);

  /**
   * Provide any modification of the catalog name passed in to match the meta-data currently used.
   * The returned value will be used for meta-data lookups.
   * This could include altering the case used or providing a base catalog if none is provided.
   */
  @Nullable
  String metaDataCatalogNameToUse(@Nullable String catalogName);

  /**
   * Provide any modification of the schema name passed in to match the meta-data currently used.
   * The returned value will be used for meta-data lookups.
   * This could include altering the case used or providing a base schema if none is provided.
   */
  @Nullable
  String metaDataSchemaNameToUse(@Nullable String schemaName);

  /**
   * Are we using the meta-data for the table columns?
   */
  boolean isTableColumnMetaDataUsed();

  /**
   * Does this database support the JDBC 3.0 feature of retrieving generated keys:
   * {@link DatabaseMetaData#supportsGetGeneratedKeys()}?
   */
  boolean isGetGeneratedKeysSupported();

  /**
   * Does this database support a simple query to retrieve the generated key when
   * the JDBC 3.0 feature of retrieving generated keys is not supported?
   *
   * @see #isGetGeneratedKeysSupported()
   */
  boolean isGetGeneratedKeysSimulated();

  /**
   * Get the simple query to retrieve a generated key.
   */
  @Nullable
  String getSimpleQueryForGetGeneratedKey(String tableName, String keyColumnName);

  /**
   * Does this database support a column name String array for retrieving generated keys:
   * {@link java.sql.Connection#createStruct(String, Object[])}?
   */
  boolean isGeneratedKeysColumnNameArraySupported();

  /**
   * Get the table parameter meta-data that is currently used.
   *
   * @return a List of {@link TableParameterMetaData}
   */
  List<TableParameterMetaData> getTableParameterMetaData();

}
