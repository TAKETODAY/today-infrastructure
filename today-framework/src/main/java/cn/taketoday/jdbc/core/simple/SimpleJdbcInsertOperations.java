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

package cn.taketoday.jdbc.core.simple;

import java.util.Map;

import cn.taketoday.jdbc.core.namedparam.SqlParameterSource;
import cn.taketoday.jdbc.support.KeyHolder;

/**
 * Interface specifying the API for a Simple JDBC Insert implemented by {@link SimpleJdbcInsert}.
 * This interface is not often used directly, but provides the option to enhance testability,
 * as it can easily be mocked or stubbed.
 *
 * @author Thomas Risberg
 * @since 4.0
 */
public interface SimpleJdbcInsertOperations {

  /**
   * Specify the table name to be used for the insert.
   *
   * @param tableName the name of the stored table
   * @return the instance of this SimpleJdbcInsert
   */
  SimpleJdbcInsertOperations withTableName(String tableName);

  /**
   * Specify the schema name, if any, to be used for the insert.
   *
   * @param schemaName the name of the schema
   * @return the instance of this SimpleJdbcInsert
   */
  SimpleJdbcInsertOperations withSchemaName(String schemaName);

  /**
   * Specify the catalog name, if any, to be used for the insert.
   *
   * @param catalogName the name of the catalog
   * @return the instance of this SimpleJdbcInsert
   */
  SimpleJdbcInsertOperations withCatalogName(String catalogName);

  /**
   * Specify the column names that the insert statement should be limited to use.
   *
   * @param columnNames one or more column names
   * @return the instance of this SimpleJdbcInsert
   */
  SimpleJdbcInsertOperations usingColumns(String... columnNames);

  /**
   * Specify the names of any columns that have auto generated keys.
   *
   * @param columnNames one or more column names
   * @return the instance of this SimpleJdbcInsert
   */
  SimpleJdbcInsertOperations usingGeneratedKeyColumns(String... columnNames);

  /**
   * Turn off any processing of column meta-data information obtained via JDBC.
   *
   * @return the instance of this SimpleJdbcInsert
   */
  SimpleJdbcInsertOperations withoutTableColumnMetaDataAccess();

  /**
   * Include synonyms for the column meta-data lookups via JDBC.
   * <p>Note: This is only necessary to include for Oracle since other databases
   * supporting synonyms seems to include the synonyms automatically.
   *
   * @return the instance of this SimpleJdbcInsert
   */
  SimpleJdbcInsertOperations includeSynonymsForTableColumnMetaData();

  /**
   * Execute the insert using the values passed in.
   *
   * @param args a Map containing column names and corresponding value
   * @return the number of rows affected as returned by the JDBC driver
   */
  int execute(Map<String, ?> args);

  /**
   * Execute the insert using the values passed in.
   *
   * @param parameterSource the SqlParameterSource containing values to use for insert
   * @return the number of rows affected as returned by the JDBC driver
   */
  int execute(SqlParameterSource parameterSource);

  /**
   * Execute the insert using the values passed in and return the generated key.
   * <p>This requires that the name of the columns with auto generated keys have been specified.
   * This method will always return a KeyHolder but the caller must verify that it actually
   * contains the generated keys.
   *
   * @param args a Map containing column names and corresponding value
   * @return the generated key value
   */
  Number executeAndReturnKey(Map<String, ?> args);

  /**
   * Execute the insert using the values passed in and return the generated key.
   * <p>This requires that the name of the columns with auto generated keys have been specified.
   * This method will always return a KeyHolder but the caller must verify that it actually
   * contains the generated keys.
   *
   * @param parameterSource the SqlParameterSource containing values to use for insert
   * @return the generated key value.
   */
  Number executeAndReturnKey(SqlParameterSource parameterSource);

  /**
   * Execute the insert using the values passed in and return the generated keys.
   * <p>This requires that the name of the columns with auto generated keys have been specified.
   * This method will always return a KeyHolder but the caller must verify that it actually
   * contains the generated keys.
   *
   * @param args a Map containing column names and corresponding value
   * @return the KeyHolder containing all generated keys
   */
  KeyHolder executeAndReturnKeyHolder(Map<String, ?> args);

  /**
   * Execute the insert using the values passed in and return the generated keys.
   * <p>This requires that the name of the columns with auto generated keys have been specified.
   * This method will always return a KeyHolder but the caller must verify that it actually
   * contains the generated keys.
   *
   * @param parameterSource the SqlParameterSource containing values to use for insert
   * @return the KeyHolder containing all generated keys
   */
  KeyHolder executeAndReturnKeyHolder(SqlParameterSource parameterSource);

  /**
   * Execute a batch insert using the batch of values passed in.
   *
   * @param batch an array of Maps containing a batch of column names and corresponding value
   * @return the array of number of rows affected as returned by the JDBC driver
   */
  @SuppressWarnings("unchecked")
  int[] executeBatch(Map<String, ?>... batch);

  /**
   * Execute a batch insert using the batch of values passed in.
   *
   * @param batch an array of SqlParameterSource containing values for the batch
   * @return the array of number of rows affected as returned by the JDBC driver
   */
  int[] executeBatch(SqlParameterSource... batch);

}
