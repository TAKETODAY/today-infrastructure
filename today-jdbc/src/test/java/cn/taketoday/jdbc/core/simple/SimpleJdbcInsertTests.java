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

package cn.taketoday.jdbc.core.simple;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;

import javax.sql.DataSource;

import cn.taketoday.dao.InvalidDataAccessApiUsageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for {@link SimpleJdbcInsert}.
 *
 * @author Thomas Risberg
 * @author Sam Brannen
 */
class SimpleJdbcInsertTests {

  private final Connection connection = mock();

  private final DatabaseMetaData databaseMetaData = mock();

  private final DataSource dataSource = mock();

  @BeforeEach
  void setUp() throws Exception {
    given(connection.getMetaData()).willReturn(databaseMetaData);
    given(dataSource.getConnection()).willReturn(connection);
  }

  @AfterEach
  void verifyClosed() throws Exception {
    verify(connection).close();
  }

  @Test
  void missingTableName() throws Exception {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource);

    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(insert::compile)
            .withMessage("Table name is required");

    // Appease the @AfterEach checks.
    connection.close();
  }

  @Test
    // gh-24013 and gh-31208
  void usingQuotedIdentifiersWithoutSupplyingColumnNames() throws Exception {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
            .withTableName("my_table")
            .usingQuotedIdentifiers();

    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(insert::compile)
            .withMessage("Explicit column names must be provided when using quoted identifiers");

    // Appease the @AfterEach checks.
    connection.close();
  }

  /**
   * This method does not test any functionality but rather only that
   * configuration methods can be chained without compiler errors.
   */
  @Test
  // gh-31177
  void methodChaining() throws Exception {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
            .withCatalogName("my_catalog")
            .withSchemaName("my_schema")
            .withTableName("my_table")
            .usingColumns("col1", "col2")
            .usingGeneratedKeyColumns("id")
            .usingQuotedIdentifiers()
            .withoutTableColumnMetaDataAccess()
            .includeSynonymsForTableColumnMetaData();

    assertThat(insert).isNotNull();

    // Satisfy the @AfterEach mock verification.
    connection.close();
  }

  @Test
  void noSuchTable() throws Exception {
    ResultSet resultSet = mock();
    given(resultSet.next()).willReturn(false);

    given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
    given(databaseMetaData.getDatabaseProductVersion()).willReturn("1.0");
    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);
    given(databaseMetaData.getTables(null, null, "x", null)).willReturn(resultSet);

    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("x");
    // Shouldn't succeed in inserting into table which doesn't exist
    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(() -> insert.execute(Collections.emptyMap()))
            .withMessageStartingWith("Unable to locate columns for table 'x' so an insert statement can't be generated");

    verify(resultSet).close();
  }

  @Test
    // gh-26486
  void retrieveColumnNamesFromMetadata() throws Exception {
    ResultSet tableResultSet = mock();
    given(tableResultSet.next()).willReturn(true, false);

    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.getTables(null, null, "me", null)).willReturn(tableResultSet);

    ResultSet columnResultSet = mock();
    given(databaseMetaData.getColumns(null, "me", null, null)).willReturn(columnResultSet);
    given(columnResultSet.next()).willReturn(true, true, false);
    given(columnResultSet.getString("COLUMN_NAME")).willReturn("col1", "col2");
    given(columnResultSet.getInt("DATA_TYPE")).willReturn(Types.VARCHAR);
    given(columnResultSet.getBoolean("NULLABLE")).willReturn(false);

    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("me");
    insert.compile();
    assertThat(insert.getInsertString()).isEqualTo("INSERT INTO me (col1, col2) VALUES(?, ?)");

    verify(columnResultSet).close();
    verify(tableResultSet).close();
  }

  @Test
    // gh-26486
  void exceptionThrownWhileRetrievingColumnNamesFromMetadata() throws Exception {
    ResultSet tableResultSet = mock();
    given(tableResultSet.next()).willReturn(true, false);

    given(databaseMetaData.getUserName()).willReturn("me");
    given(databaseMetaData.getTables(null, null, "me", null)).willReturn(tableResultSet);

    ResultSet columnResultSet = mock();
    given(databaseMetaData.getColumns(null, "me", null, null)).willReturn(columnResultSet);
    // true, true, false --> simulates processing of two columns
    given(columnResultSet.next()).willReturn(true, true, false);
    given(columnResultSet.getString("COLUMN_NAME"))
            // Return a column name the first time.
            .willReturn("col1")
            // Second time, simulate an error while retrieving metadata.
            .willThrow(new SQLException("error with col2"));
    given(columnResultSet.getInt("DATA_TYPE")).willReturn(Types.VARCHAR);
    given(columnResultSet.getBoolean("NULLABLE")).willReturn(false);

    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource).withTableName("me");

    assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
            .isThrownBy(insert::compile)
            .withMessage("Unable to locate columns for table 'me' so an insert statement can't be generated. " +
                    "Consider specifying explicit column names -- for example, via SimpleJdbcInsert#usingColumns().");

    verify(columnResultSet).close();
    verify(tableResultSet).close();
  }

  @Test
  void usingColumns() {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
            .withTableName("my_table")
            .usingColumns("col1", "col2");

    insert.compile();

    assertThat(insert.getInsertString()).isEqualTo("INSERT INTO my_table (col1, col2) VALUES(?, ?)");
  }

  @Test
    //  gh-24013
  void usingColumnsAndQuotedIdentifiers() throws Exception {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
            .withTableName("my_table")
            .usingColumns("col1", "col2")
            .usingQuotedIdentifiers();

    given(databaseMetaData.getIdentifierQuoteString()).willReturn("`");

    insert.compile();
    assertThat(insert.getInsertString()).isEqualTo("INSERT INTO `my_table` (`col1`, `col2`) VALUES(?, ?)");
  }

  @Test
    //  gh-24013
  void usingColumnsAndQuotedIdentifiersWithSchemaName() throws Exception {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
            .withSchemaName("my_schema")
            .withTableName("my_table")
            .usingColumns("col1", "col2")
            .usingQuotedIdentifiers();

    given(databaseMetaData.getIdentifierQuoteString()).willReturn("`");

    insert.compile();
    assertThat(insert.getInsertString()).isEqualTo("INSERT INTO `my_schema`.`my_table` (`col1`, `col2`) VALUES(?, ?)");
  }

  @Test
  void usingSchema() {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
            .withTableName("my_table")
            .withSchemaName("my_schema")
            .usingColumns("col1", "col2");

    insert.compile();

    assertThat(insert.getInsertString()).isEqualTo("INSERT INTO my_schema.my_table (col1, col2) VALUES(?, ?)");
  }

  @Test
  void usingCatalog() {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
            .withTableName("my_table")
            .withCatalogName("my_catalog")
            .usingColumns("col1", "col2");

    insert.compile();

    assertThat(insert.getInsertString()).isEqualTo("INSERT INTO my_catalog.my_table (col1, col2) VALUES(?, ?)");
  }

  @Test
  void usingSchemaAndCatalog() {
    SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
            .withTableName("my_table")
            .withSchemaName("my_schema")
            .withCatalogName("my_catalog")
            .usingColumns("col1", "col2");

    insert.compile();

    assertThat(insert.getInsertString()).isEqualTo("INSERT INTO my_catalog.my_schema.my_table (col1, col2) VALUES(?, ?)");
  }

}
