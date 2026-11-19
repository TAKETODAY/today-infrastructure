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

package infra.jdbc.support.rowset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.function.Function;
import java.util.stream.Stream;

import infra.jdbc.InvalidResultSetAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ResultSetWrappingSqlRowSetMetaDataTests {

  private final ResultSetMetaData delegate = mock(ResultSetMetaData.class);

  private final SqlRowSetMetaData metadata = new ResultSetWrappingSqlRowSetMetaData(delegate);

  @ParameterizedTest(name = "{0}")
  @MethodSource("properties")
  <T> void readsDriverMetadata(Property<T> property) throws SQLException {
    given(property.readFromDriver().read(delegate)).willReturn(property.value());

    assertThat(property.readFromRowSet().apply(metadata)).isEqualTo(property.value());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("properties")
  <T> void translatesDriverFailures(Property<T> property) throws SQLException {
    SQLException failure = new SQLException("metadata unavailable");
    given(property.readFromDriver().read(delegate)).willThrow(failure);

    assertThatThrownBy(() -> property.readFromRowSet().apply(metadata))
            .isInstanceOf(InvalidResultSetAccessException.class).hasCause(failure);
  }

  @Test
  void cachesColumnNamesInJdbcOrder() throws SQLException {
    given(delegate.getColumnCount()).willReturn(2);
    given(delegate.getColumnName(1)).willReturn("ID");
    given(delegate.getColumnName(2)).willReturn("NAME");

    assertThat(metadata.getColumnNames()).containsExactly("ID", "NAME");
    assertThat(metadata.getColumnNames()).containsExactly("ID", "NAME");

    verify(delegate).getColumnName(1);
    verify(delegate).getColumnName(2);
  }

  static Stream<Property<?>> properties() {
    return Stream.of(
            new Property<>("catalog", it -> it.getCatalogName(2), it -> it.getCatalogName(2), "catalog"),
            new Property<>("class", it -> it.getColumnClassName(2), it -> it.getColumnClassName(2), "java.lang.String"),
            new Property<>("count", ResultSetMetaData::getColumnCount, SqlRowSetMetaData::getColumnCount, 3),
            new Property<>("display size", it -> it.getColumnDisplaySize(2), it -> it.getColumnDisplaySize(2), 20),
            new Property<>("label", it -> it.getColumnLabel(2), it -> it.getColumnLabel(2), "alias"),
            new Property<>("name", it -> it.getColumnName(2), it -> it.getColumnName(2), "name"),
            new Property<>("type", it -> it.getColumnType(2), it -> it.getColumnType(2), Types.VARCHAR),
            new Property<>("type name", it -> it.getColumnTypeName(2), it -> it.getColumnTypeName(2), "VARCHAR"),
            new Property<>("precision", it -> it.getPrecision(2), it -> it.getPrecision(2), 12),
            new Property<>("scale", it -> it.getScale(2), it -> it.getScale(2), 2),
            new Property<>("schema", it -> it.getSchemaName(2), it -> it.getSchemaName(2), "public"),
            new Property<>("table", it -> it.getTableName(2), it -> it.getTableName(2), "users"),
            new Property<>("case sensitivity", it -> it.isCaseSensitive(2), it -> it.isCaseSensitive(2), true),
            new Property<>("currency", it -> it.isCurrency(2), it -> it.isCurrency(2), true),
            new Property<>("signed", it -> it.isSigned(2), it -> it.isSigned(2), true));
  }

  @FunctionalInterface
  private interface DriverRead<T> {

    T read(ResultSetMetaData metadata) throws SQLException;
  }

  private record Property<T>(String name, DriverRead<T> readFromDriver,
                             Function<SqlRowSetMetaData, T> readFromRowSet, T value) {

    @Override
    public String toString() {
      return name;
    }
  }

}
