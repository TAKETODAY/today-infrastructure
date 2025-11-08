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

package infra.jdbc.core;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.rowset.CachedRowSet;

import infra.jdbc.support.rowset.SqlRowSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:25
 */
class SqlRowSetResultSetExtractorTests {

  @Test
  void shouldCreateNewCachedRowSet() throws SQLException {
    SqlRowSetResultSetExtractor extractor = new SqlRowSetResultSetExtractor();

    CachedRowSet rowSet = extractor.newCachedRowSet();

    assertThat(rowSet).isNotNull();
  }

  @Test
  void shouldExtractDataFromResultSet() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    CachedRowSet cachedRowSet = mock(CachedRowSet.class);

    SqlRowSetResultSetExtractor extractor = new SqlRowSetResultSetExtractor() {
      @Override
      protected CachedRowSet newCachedRowSet() throws SQLException {
        return cachedRowSet;
      }
    };

    SqlRowSet result = extractor.extractData(resultSet);

    assertThat(result).isNotNull();
    verify(cachedRowSet).populate(resultSet);
  }

  @Test
  void shouldCreateSqlRowSet() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    CachedRowSet cachedRowSet = mock(CachedRowSet.class);

    SqlRowSetResultSetExtractor extractor = new SqlRowSetResultSetExtractor() {
      @Override
      protected CachedRowSet newCachedRowSet() throws SQLException {
        return cachedRowSet;
      }
    };

    SqlRowSet result = extractor.createSqlRowSet(resultSet);

    assertThat(result).isNotNull();
    verify(cachedRowSet).populate(resultSet);
  }

  @Test
  void shouldHandleSQLExceptionWhenPopulatingRowSet() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    CachedRowSet cachedRowSet = mock(CachedRowSet.class);

    SqlRowSetResultSetExtractor extractor = new SqlRowSetResultSetExtractor() {
      @Override
      protected CachedRowSet newCachedRowSet() throws SQLException {
        return cachedRowSet;
      }
    };

    SQLException sqlException = new SQLException("Population failed");

    doThrow(sqlException).when(cachedRowSet).populate(resultSet);

    assertThatThrownBy(() -> extractor.createSqlRowSet(resultSet))
            .isInstanceOf(SQLException.class)
            .hasMessage("Population failed");
  }

  @Test
  void shouldOverrideNewCachedRowSet() throws SQLException {
    CachedRowSet customRowSet = mock(CachedRowSet.class);

    SqlRowSetResultSetExtractor extractor = new SqlRowSetResultSetExtractor() {
      @Override
      protected CachedRowSet newCachedRowSet() throws SQLException {
        return customRowSet;
      }
    };

    CachedRowSet result = extractor.newCachedRowSet();

    assertThat(result).isEqualTo(customRowSet);
  }

}