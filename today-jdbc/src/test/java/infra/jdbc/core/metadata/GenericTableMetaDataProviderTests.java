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

package infra.jdbc.core.metadata;

import org.junit.jupiter.api.Test;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:45
 */
class GenericTableMetaDataProviderTests {

  @Test
  void shouldCreateGenericTableMetaDataProvider() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThat(provider).isNotNull();
  }

  @Test
  void shouldSetAndGetStoresUpperCaseIdentifiers() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    provider.setStoresUpperCaseIdentifiers(true);
    assertThat(provider.isStoresUpperCaseIdentifiers()).isTrue();

    provider.setStoresUpperCaseIdentifiers(false);
    assertThat(provider.isStoresUpperCaseIdentifiers()).isFalse();
  }

  @Test
  void shouldSetAndGetStoresLowerCaseIdentifiers() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    provider.setStoresLowerCaseIdentifiers(true);
    assertThat(provider.isStoresLowerCaseIdentifiers()).isTrue();

    provider.setStoresLowerCaseIdentifiers(false);
    assertThat(provider.isStoresLowerCaseIdentifiers()).isFalse();
  }

  @Test
  void shouldReturnTableColumnMetaDataUsage() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThat(provider.isTableColumnMetaDataUsed()).isFalse();
  }

  @Test
  void shouldGetTableParameterMetaData() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    List<TableParameterMetaData> metaData = provider.getTableParameterMetaData();

    assertThat(metaData).isNotNull();
    assertThat(metaData).isEmpty();
  }

  @Test
  void shouldReturnGetGeneratedKeysSupport() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThat(provider.isGetGeneratedKeysSupported()).isTrue();
  }

  @Test
  void shouldReturnGetGeneratedKeysSimulation() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThat(provider.isGetGeneratedKeysSimulated()).isFalse();
  }

  @Test
  void shouldReturnNullForSimpleQueryForGetGeneratedKey() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    String result = provider.getSimpleQueryForGetGeneratedKey("test_table", "id");

    assertThat(result).isNull();
  }

  @Test
  void shouldSetGetGeneratedKeysSupported() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    provider.setGetGeneratedKeysSupported(false);
    assertThat(provider.isGetGeneratedKeysSupported()).isFalse();

    provider.setGetGeneratedKeysSupported(true);
    assertThat(provider.isGetGeneratedKeysSupported()).isTrue();
  }

  @Test
  void shouldSetGeneratedKeysColumnNameArraySupported() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    provider.setGeneratedKeysColumnNameArraySupported(false);
    assertThat(provider.isGeneratedKeysColumnNameArraySupported()).isFalse();

    provider.setGeneratedKeysColumnNameArraySupported(true);
    assertThat(provider.isGeneratedKeysColumnNameArraySupported()).isTrue();
  }

  @Test
  void shouldInitializeWithMetaData() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.supportsGetGeneratedKeys()).thenReturn(true);
    when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");
    when(databaseMetaData.storesUpperCaseIdentifiers()).thenReturn(true);
    when(databaseMetaData.storesLowerCaseIdentifiers()).thenReturn(false);
    when(databaseMetaData.getIdentifierQuoteString()).thenReturn("`");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.initializeWithMetaData(databaseMetaData);

    assertThat(provider.isGetGeneratedKeysSupported()).isTrue();
    assertThat(provider.isGeneratedKeysColumnNameArraySupported()).isTrue();
    assertThat(provider.isStoresUpperCaseIdentifiers()).isTrue();
    assertThat(provider.isStoresLowerCaseIdentifiers()).isFalse();
    assertThat(provider.getIdentifierQuoteString()).isEqualTo("`");
  }

  @Test
  void shouldHandleExceptionWhenInitializingWithMetaData() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.supportsGetGeneratedKeys()).thenThrow(new SQLException("Test exception"));

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    assertThatCode(() -> provider.initializeWithMetaData(databaseMetaData)).doesNotThrowAnyException();
  }

  @Test
  void shouldHandleUnsupportedGeneratedKeys() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.supportsGetGeneratedKeys()).thenReturn(false);
    when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.initializeWithMetaData(databaseMetaData);

    assertThat(provider.isGetGeneratedKeysSupported()).isFalse();
    assertThat(provider.isGeneratedKeysColumnNameArraySupported()).isFalse();
  }

  @Test
  void shouldHandleProductsNotSupportingGeneratedKeysColumnNameArray() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.supportsGetGeneratedKeys()).thenReturn(true);
    when(databaseMetaData.getDatabaseProductName()).thenReturn("Apache Derby");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.initializeWithMetaData(databaseMetaData);

    assertThat(provider.isGeneratedKeysColumnNameArraySupported()).isFalse();
  }

  @Test
  void shouldTableNameToUseWithUpperCaseIdentifiers() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresUpperCaseIdentifiers(true);
    provider.setStoresLowerCaseIdentifiers(false);

    String result = provider.tableNameToUse("test_table");

    assertThat(result).isEqualTo("TEST_TABLE");
  }

  @Test
  void shouldTableNameToUseWithLowerCaseIdentifiers() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresUpperCaseIdentifiers(false);
    provider.setStoresLowerCaseIdentifiers(true);

    String result = provider.tableNameToUse("TEST_TABLE");

    assertThat(result).isEqualTo("test_table");
  }

  @Test
  void shouldTableNameToUseWithMixedCaseIdentifiers() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresUpperCaseIdentifiers(false);
    provider.setStoresLowerCaseIdentifiers(false);

    String tableName = "Test_Table";
    String result = provider.tableNameToUse(tableName);

    assertThat(result).isEqualTo(tableName);
  }

  @Test
  void shouldColumnNameToUse() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresUpperCaseIdentifiers(true);

    String result = provider.columnNameToUse("id");

    assertThat(result).isEqualTo("ID");
  }

  @Test
  void shouldCatalogNameToUse() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresLowerCaseIdentifiers(true);

    String result = provider.catalogNameToUse("CATALOG");

    assertThat(result).isEqualTo("CATALOG");
  }

  @Test
  void shouldSchemaNameToUse() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresUpperCaseIdentifiers(true);

    String result = provider.schemaNameToUse("schema");

    assertThat(result).isEqualTo("SCHEMA");
  }

  @Test
  void shouldReturnMetaDataCatalogNameToUse() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresLowerCaseIdentifiers(true);

    String result = provider.metaDataCatalogNameToUse("CATALOG");

    assertThat(result).isEqualTo("CATALOG");
  }

  @Test
  void shouldReturnMetaDataSchemaNameToUse() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresUpperCaseIdentifiers(true);
    when(databaseMetaData.getUserName()).thenReturn("testuser");

    String result = provider.metaDataSchemaNameToUse("schema");

    assertThat(result).isEqualTo("SCHEMA");
  }

  @Test
  void shouldReturnDefaultSchemaNameToUse() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.setStoresUpperCaseIdentifiers(true);

    String result = provider.metaDataSchemaNameToUse(null);

    assertThat(result).isEqualTo("TESTUSER");
  }

  @Test
  void shouldGetIdentifierQuoteString() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.getIdentifierQuoteString()).thenReturn("\"");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.initializeWithMetaData(databaseMetaData);

    assertThat(provider.getIdentifierQuoteString()).isEqualTo("\"");
  }

  @Test
  void shouldInitializeWithTableColumnMetaData() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThatCode(() -> provider.initializeWithTableColumnMetaData(databaseMetaData, null, null, "test_table"))
            .doesNotThrowAnyException();

    assertThat(provider.isTableColumnMetaDataUsed()).isTrue();
  }

  @Test
  void shouldHandleExceptionDuringTableColumnMetaDataInitialization() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.getTables(null, null, "TEST_TABLE", null))
            .thenThrow(new SQLException("Database error"));

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThatCode(() -> provider.initializeWithTableColumnMetaData(databaseMetaData, null, null, "test_table"))
            .doesNotThrowAnyException();

    assertThat(provider.isTableColumnMetaDataUsed()).isTrue();
    assertThat(provider.getTableParameterMetaData()).isEmpty();
  }

  @Test
  void shouldTableNameToUseReturnNullForNullInput() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    String result = provider.tableNameToUse(null);

    assertThat(result).isNull();
  }

  @Test
  void shouldColumnNameToUseReturnNullForNullInput() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    String result = provider.columnNameToUse(null);

    assertThat(result).isNull();
  }

  @Test
  void shouldCatalogNameToUseReturnNullForNullInput() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    String result = provider.catalogNameToUse(null);

    assertThat(result).isNull();
  }

  @Test
  void shouldSchemaNameToUseReturnNullForNullInput() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    String result = provider.schemaNameToUse(null);

    assertThat(result).isNull();
  }

  @Test
  void shouldMetaDataCatalogNameToUseReturnNullForNullInput() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    String result = provider.metaDataCatalogNameToUse(null);

    assertThat(result).isNull();
  }

  @Test
  void shouldGetDefaultSchema() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    String defaultSchema = provider.getDefaultSchema();

    assertThat(defaultSchema).isEqualTo("testuser");
  }

  @Test
  void shouldGetDatabaseVersion() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.getDatabaseProductVersion()).thenReturn("8.0.0");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);
    provider.initializeWithMetaData(databaseMetaData);

    String version = provider.getDatabaseVersion();

    assertThat(version).isEqualTo("8.0.0");
  }

  @Test
  void shouldGetDatabaseVersionReturnNullWhenNotInitialized() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    String version = provider.getDatabaseVersion();

    assertThat(version).isNull();
  }

  @Test
  void shouldHandleSQLExceptionWhenGettingDatabaseVersion() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.getDatabaseProductVersion()).thenThrow(new SQLException("Error"));

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThatCode(() -> provider.initializeWithMetaData(databaseMetaData)).doesNotThrowAnyException();

    String version = provider.getDatabaseVersion();
    assertThat(version).isNull();
  }

  @Test
  void shouldHandleSQLExceptionWhenGettingIdentifierQuoteString() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.getIdentifierQuoteString()).thenThrow(new SQLException("Error"));

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThatCode(() -> provider.initializeWithMetaData(databaseMetaData)).doesNotThrowAnyException();

    assertThat(provider.getIdentifierQuoteString()).isEqualTo(" ");
  }

  @Test
  void shouldHandleSQLExceptionWhenGettingStoresUpperCaseIdentifiers() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.storesUpperCaseIdentifiers()).thenThrow(new SQLException("Error"));

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThatCode(() -> provider.initializeWithMetaData(databaseMetaData)).doesNotThrowAnyException();

    // Default value should remain
    assertThat(provider.isStoresUpperCaseIdentifiers()).isTrue();
  }

  @Test
  void shouldHandleSQLExceptionWhenGettingStoresLowerCaseIdentifiers() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.storesLowerCaseIdentifiers()).thenThrow(new SQLException("Error"));

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThatCode(() -> provider.initializeWithMetaData(databaseMetaData)).doesNotThrowAnyException();

    // Default value should remain
    assertThat(provider.isStoresLowerCaseIdentifiers()).isFalse();
  }

  @Test
  void shouldHandleSQLExceptionWhenGettingSupportsGetGeneratedKeys() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.supportsGetGeneratedKeys()).thenThrow(new SQLException("Error"));
    when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThatCode(() -> provider.initializeWithMetaData(databaseMetaData)).doesNotThrowAnyException();

    // Default value should remain
    assertThat(provider.isGetGeneratedKeysSupported()).isTrue();
  }

  @Test
  void shouldHandleSQLExceptionWhenGettingDatabaseProductName() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");
    when(databaseMetaData.supportsGetGeneratedKeys()).thenReturn(true);
    when(databaseMetaData.getDatabaseProductName()).thenThrow(new SQLException("Error"));

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    assertThatCode(() -> provider.initializeWithMetaData(databaseMetaData)).doesNotThrowAnyException();

    // Should fallback to default behavior
    assertThat(provider.isGeneratedKeysColumnNameArraySupported()).isTrue();
  }

  @Test
  void shouldLocateTableAndProcessMetaData() throws SQLException {
    DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
    when(databaseMetaData.getUserName()).thenReturn("testuser");

    GenericTableMetaDataProvider provider = new GenericTableMetaDataProvider(databaseMetaData);

    ResultSet tables = mock(ResultSet.class);
    when(tables.next()).thenReturn(true, false);
    when(tables.getString("TABLE_CAT")).thenReturn(null);
    when(tables.getString("TABLE_SCHEM")).thenReturn("testuser");
    when(tables.getString("TABLE_NAME")).thenReturn("test_table");

    when(databaseMetaData.getTables(null, "TESTUSER", "TEST_TABLE", null)).thenReturn(tables);

    ResultSet tableColumns = mock(ResultSet.class);
    when(tableColumns.next()).thenReturn(false);

    when(databaseMetaData.getColumns(null, "TESTUSER", "TEST_TABLE", null)).thenReturn(tableColumns);

    provider.locateTableAndProcessMetaData(databaseMetaData, null, null, "test_table");

  }

}