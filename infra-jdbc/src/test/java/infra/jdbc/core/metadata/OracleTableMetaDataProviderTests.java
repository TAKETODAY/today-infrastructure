/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.jdbc.core.metadata;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import infra.dao.InvalidDataAccessApiUsageException;
import oracle.jdbc.OracleConnection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OracleTableMetaDataProviderTests {

  private static final String CURRENT_SCHEMA_CALL = "{? = call sys_context('USERENV', 'CURRENT_SCHEMA')}";

  private final DatabaseMetaData metadata = mock(DatabaseMetaData.class);

  private final Connection connection = mock(Connection.class);

  @Test
  void readsSessionSchemaInsteadOfUserName() throws SQLException {
    var statement = mock(CallableStatement.class);
    given(metadata.getUserName()).willReturn("APP_USER");
    given(metadata.getConnection()).willReturn(connection);
    given(connection.prepareCall(CURRENT_SCHEMA_CALL)).willReturn(statement);
    given(statement.getString(1)).willReturn("APP_SCHEMA");

    var provider = new OracleTableMetaDataProvider(metadata);
    assertThat(provider.metaDataSchemaNameToUse(null)).isEqualTo("APP_SCHEMA");

    InOrder order = inOrder(statement);
    order.verify(statement).registerOutParameter(1, Types.VARCHAR);
    order.verify(statement).execute();
    order.verify(statement).getString(1);
    order.verify(statement).close();
  }

  @Test
  void fallsBackToUserNameAndClosesFailedSchemaStatement() throws SQLException {
    var statement = mock(CallableStatement.class);
    given(metadata.getUserName()).willReturn("APP_USER");
    given(metadata.getConnection()).willReturn(connection);
    given(connection.prepareCall(CURRENT_SCHEMA_CALL)).willReturn(statement);
    given(statement.execute()).willThrow(new SQLException("schema lookup failed"));

    var provider = new OracleTableMetaDataProvider(metadata);
    assertThat(provider.metaDataSchemaNameToUse(null)).isEqualTo("APP_USER");
    verify(statement).close();
  }

  @Test
  void performsOrdinaryTableLookupWhenSynonymsAreDisabled() throws SQLException {
    var provider = new OracleTableMetaDataProvider(metadata);
    provider.initializeWithTableColumnMetaData(metadata, "catalog", "schema", "users");

    verify(metadata).getTables("CATALOG", "SCHEMA", "USERS", null);
  }

  @Test
  void fallsBackToOrdinaryLookupWhenNoConnectionIsAvailable() throws SQLException {
    given(metadata.getUserName()).willReturn("APP_USER");
    var provider = new OracleTableMetaDataProvider(metadata, true);

    assertThat(provider.metaDataSchemaNameToUse(null)).isEqualTo("APP_USER");
    provider.initializeWithTableColumnMetaData(metadata, null, "schema", "users");
    verify(metadata).getTables(null, "SCHEMA", "USERS", null);
  }

  @Test
  void fallsBackToOrdinaryLookupWhenDriverCannotUnwrapConnection() throws SQLException {
    var provider = new OracleTableMetaDataProvider(metadata, true);
    given(metadata.getConnection()).willReturn(connection);
    given(connection.unwrap(OracleConnection.class)).willThrow(new SQLException("not an Oracle connection"));

    provider.initializeWithTableColumnMetaData(metadata, null, "schema", "users");

    verify(metadata).getTables(null, "SCHEMA", "USERS", null);
  }

  @Test
  void enablesSynonymsOnlyDuringMetadataLookup() throws SQLException {
    var provider = new OracleTableMetaDataProvider(metadata, true);
    OracleConnection oracle = oracleConnection();
    given(oracle.getIncludeSynonyms()).willReturn(false);

    provider.initializeWithTableColumnMetaData(metadata, null, "schema", "users");

    InOrder order = inOrder(oracle, metadata);
    order.verify(oracle).getIncludeSynonyms();
    order.verify(oracle).setIncludeSynonyms(true);
    order.verify(metadata).getTables(null, "SCHEMA", "USERS", null);
    order.verify(oracle).setIncludeSynonyms(false);
  }

  @Test
  void reportsFailureToEnableSynonyms() throws SQLException {
    var provider = new OracleTableMetaDataProvider(metadata, true);
    OracleConnection oracle = oracleConnection();
    var failure = new IllegalStateException("cannot enable synonyms");
    willThrow(failure).given(oracle).setIncludeSynonyms(true);

    assertThatThrownBy(() -> provider.initializeWithTableColumnMetaData(metadata, null, "schema", "users"))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessageContaining("prepare Oracle Connection").hasRootCause(failure);
  }

  @Test
  void reportsFailureToRestoreSynonymOption() throws SQLException {
    var provider = new OracleTableMetaDataProvider(metadata, true);
    OracleConnection oracle = oracleConnection();
    var failure = new IllegalStateException("cannot restore synonym option");
    willThrow(failure).given(oracle).setIncludeSynonyms(false);

    assertThatThrownBy(() -> provider.initializeWithTableColumnMetaData(metadata, null, "schema", "users"))
            .isInstanceOf(InvalidDataAccessApiUsageException.class)
            .hasMessageContaining("reset Oracle Connection").hasRootCause(failure);
    verify(metadata).getTables(null, "SCHEMA", "USERS", null);
  }

  private OracleConnection oracleConnection() throws SQLException {
    var oracle = mock(OracleConnection.class);
    given(metadata.getConnection()).willReturn(connection);
    given(connection.unwrap(OracleConnection.class)).willReturn(oracle);
    return oracle;
  }

}
