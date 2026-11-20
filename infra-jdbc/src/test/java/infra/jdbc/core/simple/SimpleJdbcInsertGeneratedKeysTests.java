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

package infra.jdbc.core.simple;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import javax.sql.DataSource;

import infra.dao.DataIntegrityViolationException;
import infra.dao.InvalidDataAccessApiUsageException;
import infra.dao.InvalidDataAccessResourceUsageException;
import infra.jdbc.core.JdbcTemplate;
import infra.jdbc.core.namedparam.MapSqlParameterSource;
import infra.jdbc.core.namedparam.SqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SimpleJdbcInsertGeneratedKeysTests {

  private static final String INSERT_SQL = "INSERT INTO users (name) VALUES(?)";

  private final DataSource dataSource = mock(DataSource.class);

  private final Connection connection = mock(Connection.class);

  private final DatabaseMetaData metadata = mock(DatabaseMetaData.class);

  private final PreparedStatement insertStatement = mock(PreparedStatement.class);

  @BeforeEach
  void configureConnection() throws SQLException {
    given(dataSource.getConnection()).willReturn(connection);
    given(connection.getMetaData()).willReturn(metadata);
  }

  @Test
  void usesReturningClauseWhenPostgresDriverDoesNotSupportGeneratedKeys() throws SQLException {
    given(metadata.getDatabaseProductName()).willReturn("PostgreSQL");
    SimpleJdbcInsert insert = insert().usingGeneratedKeyColumns("id");
    var keys = mock(ResultSet.class);
    given(connection.prepareStatement(INSERT_SQL + " RETURNING id")).willReturn(insertStatement);
    given(insertStatement.executeQuery()).willReturn(keys);
    given(keys.next()).willReturn(true, false);
    var columns = mock(ResultSetMetaData.class);
    given(keys.getMetaData()).willReturn(columns);
    given(columns.getColumnCount()).willReturn(1);
    given(keys.getLong(1)).willReturn(23L);

    var keyHolder = insert.executeAndReturnKeyHolder(new MapSqlParameterSource("name", "Ada"));

    assertThat(keyHolder.getKeys()).containsExactlyEntriesOf(Map.of("id", 23L));
    verify(insertStatement).setString(1, "Ada");
    verify(keys).close();
    verify(insertStatement).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void executesHsqlIdentityQueryOnInsertConnection(boolean hasKey) throws SQLException {
    given(metadata.getDatabaseProductName()).willReturn("HSQL Database Engine");
    SimpleJdbcInsert insert = insert().usingGeneratedKeyColumns("id");
    var keyStatement = mock(Statement.class);
    var keys = mock(ResultSet.class);
    given(connection.prepareStatement(INSERT_SQL)).willReturn(insertStatement);
    given(connection.createStatement()).willReturn(keyStatement);
    given(keyStatement.executeQuery("select max(identity()) from users")).willReturn(keys);
    given(keys.next()).willReturn(hasKey);
    given(keys.getLong(1)).willReturn(23L);
    var arguments = new MapSqlParameterSource("name", "Ada");

    if (hasKey) {
      assertThat(insert.executeAndReturnKey(arguments)).isEqualTo(23L);
    }
    else {
      assertThatThrownBy(() -> insert.executeAndReturnKey(arguments))
              .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("Unable to retrieve the generated key");
    }

    var order = inOrder(connection, insertStatement, keyStatement, keys);
    order.verify(connection).prepareStatement(INSERT_SQL);
    order.verify(insertStatement).setString(1, "Ada");
    order.verify(insertStatement).executeUpdate();
    order.verify(insertStatement).close();
    order.verify(connection).createStatement();
    order.verify(keyStatement).executeQuery("select max(identity()) from users");
    order.verify(keys).close();
    order.verify(keyStatement).close();
    order.verify(connection).close();
  }

  @Test
  void usesReturnGeneratedKeysWhenDriverDoesNotAcceptColumnNames() throws SQLException {
    given(metadata.getDatabaseProductName()).willReturn("HSQL Database Engine");
    given(metadata.supportsGetGeneratedKeys()).willReturn(true);
    given(connection.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)).willReturn(insertStatement);
    given(insertStatement.getGeneratedKeys()).willReturn(mock(ResultSet.class));

    var keyHolder = insert().usingGeneratedKeyColumns("id").executeAndReturnKeyHolder(Map.of("name", "Ada"));

    assertThat(keyHolder.getKeyList()).isEmpty();
    verify(insertStatement).setString(1, "Ada");
    verify(insertStatement).executeUpdate();
    verify(insertStatement).getGeneratedKeys();
  }

  @Test
  void rejectsGeneratedKeysWhenNeitherNativeNorSimulatedSupportExists() throws SQLException {
    given(metadata.getDatabaseProductName()).willReturn("OtherDB");
    SimpleJdbcInsert insert = insert().usingGeneratedKeyColumns("id");

    assertThatThrownBy(() -> insert.executeAndReturnKey(Map.of("name", "Ada")))
            .isInstanceOf(InvalidDataAccessResourceUsageException.class).hasMessageContaining("not supported");
    verifyNoInteractions(insertStatement);
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void requiresGeneratedColumnNamesEvenWhenTableColumnsAreExplicit(boolean nativeSupport) throws SQLException {
    given(metadata.getDatabaseProductName()).willReturn("HSQL Database Engine");
    given(metadata.supportsGetGeneratedKeys()).willReturn(nativeSupport);

    assertThatThrownBy(() -> insert().executeAndReturnKey(Map.of("name", "Ada")))
            .isInstanceOf(InvalidDataAccessApiUsageException.class).hasMessageContaining("Generated Key Name(s) not specified");
    verifyNoInteractions(insertStatement);
  }

  @Test
  void refusesToSimulateMultipleGeneratedColumns() throws SQLException {
    given(metadata.getDatabaseProductName()).willReturn("PostgreSQL");
    SimpleJdbcInsert insert = insert().usingGeneratedKeyColumns("id", "version");

    assertThatThrownBy(() -> insert.executeAndReturnKeyHolder(Map.of("name", "Ada")))
            .isInstanceOf(InvalidDataAccessApiUsageException.class).hasMessageContaining("single column");
    verifyNoInteractions(insertStatement);
  }

  @Test
  void requestsGeneratedColumnsByNameWhenSupported() throws SQLException {
    given(metadata.getDatabaseProductName()).willReturn("TestDB");
    given(metadata.supportsGetGeneratedKeys()).willReturn(true);
    given(connection.prepareStatement(INSERT_SQL, new String[] { "id" })).willReturn(insertStatement);
    var keys = mock(ResultSet.class);
    var columns = mock(ResultSetMetaData.class);
    given(insertStatement.getGeneratedKeys()).willReturn(keys);
    given(keys.next()).willReturn(true, false);
    given(keys.getMetaData()).willReturn(columns);
    given(columns.getColumnCount()).willReturn(1);
    given(columns.getColumnLabel(1)).willReturn("id");
    given(keys.getObject(1)).willReturn(23L);

    assertThat(insert().usingGeneratedKeyColumns("id").executeAndReturnKey(Map.of("name", "Ada")))
            .isEqualTo(23L);
    verify(keys).close();
    verify(insertStatement).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void executesInsertWithNamedArguments(boolean useParameterSource) throws SQLException {
    given(connection.prepareStatement(INSERT_SQL)).willReturn(insertStatement);
    given(insertStatement.executeUpdate()).willReturn(1);
    var insert = insert();

    int count = useParameterSource ? insert.execute(new MapSqlParameterSource("name", "Ada"))
            : insert.execute(Map.of("name", "Ada"));

    assertThat(count).isEqualTo(1);
    var order = inOrder(insertStatement);
    order.verify(insertStatement).setString(1, "Ada");
    order.verify(insertStatement).executeUpdate();
    order.verify(insertStatement).close();
  }

  @ParameterizedTest
  @ValueSource(booleans = { true, false })
  void bindsEachRowInBatchOrder(boolean useParameterSources) throws SQLException {
    given(connection.prepareStatement(INSERT_SQL)).willReturn(insertStatement);
    given(metadata.supportsBatchUpdates()).willReturn(true);
    given(insertStatement.getConnection()).willReturn(connection);
    given(insertStatement.executeBatch()).willReturn(new int[] { 1, Statement.SUCCESS_NO_INFO });
    var insert = insert();

    int[] counts = useParameterSources ? insert.executeBatch(new SqlParameterSource[] {
        new MapSqlParameterSource("name", "Ada"), new MapSqlParameterSource("name", "Grace") })
            : insert.executeBatch(Map.of("name", "Ada"), Map.of("name", "Grace"));

    assertThat(counts).containsExactly(1, Statement.SUCCESS_NO_INFO);
    var order = inOrder(insertStatement);
    order.verify(insertStatement).setString(1, "Ada");
    order.verify(insertStatement).addBatch();
    order.verify(insertStatement).setString(1, "Grace");
    order.verify(insertStatement).addBatch();
    order.verify(insertStatement).executeBatch();
    order.verify(insertStatement).close();
  }

  private SimpleJdbcInsert insert() {
    return new SimpleJdbcInsert(new JdbcTemplate(dataSource)).withTableName("users")
            .usingColumns("name").withoutTableColumnMetaDataAccess();
  }

}
