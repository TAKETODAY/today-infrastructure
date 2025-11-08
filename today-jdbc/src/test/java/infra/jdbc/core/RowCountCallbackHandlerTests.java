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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:19
 */
class RowCountCallbackHandlerTests {

  @Test
  void shouldInitializeWithZeroCounts() {
    RowCountCallbackHandler handler = new RowCountCallbackHandler();

    assertThat(handler.getRowCount()).isEqualTo(0);
    assertThat(handler.getColumnCount()).isEqualTo(0);
    assertThat(handler.getColumnTypes()).isNull();
    assertThat(handler.getColumnNames()).isNull();
  }

  @Test
  void shouldProcessFirstRowAndExtractMetadata() throws SQLException {
    RowCountCallbackHandler handler = new RowCountCallbackHandler();
    ResultSet rs = mock(ResultSet.class);
    ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

    when(rs.getMetaData()).thenReturn(rsmd);
    when(rsmd.getColumnCount()).thenReturn(2);
    when(rsmd.getColumnType(1)).thenReturn(java.sql.Types.INTEGER);
    when(rsmd.getColumnType(2)).thenReturn(java.sql.Types.VARCHAR);
    when(rsmd.getColumnName(1)).thenReturn("id");
    when(rsmd.getColumnName(2)).thenReturn("name");

    handler.processRow(rs);

    assertThat(handler.getRowCount()).isEqualTo(1);
    assertThat(handler.getColumnCount()).isEqualTo(2);
    assertThat(handler.getColumnTypes()).containsExactly(java.sql.Types.INTEGER, java.sql.Types.VARCHAR);
    assertThat(handler.getColumnNames()).containsExactly("id", "name");
  }

  @Test
  void shouldProcessMultipleRows() throws SQLException {
    RowCountCallbackHandler handler = new RowCountCallbackHandler();
    ResultSet rs = mock(ResultSet.class);
    ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

    when(rs.getMetaData()).thenReturn(rsmd);
    when(rsmd.getColumnCount()).thenReturn(1);
    when(rsmd.getColumnType(1)).thenReturn(java.sql.Types.INTEGER);
    when(rsmd.getColumnName(1)).thenReturn("id");

    // Process first row (extracts metadata)
    handler.processRow(rs);

    // Process additional rows
    handler.processRow(rs);
    handler.processRow(rs);

    assertThat(handler.getRowCount()).isEqualTo(3);
    assertThat(handler.getColumnCount()).isEqualTo(1);
    assertThat(handler.getColumnTypes()).containsExactly(java.sql.Types.INTEGER);
    assertThat(handler.getColumnNames()).containsExactly("id");
  }

  @Test
  void shouldNotReExtractMetadataOnSubsequentRows() throws SQLException {
    RowCountCallbackHandler handler = new RowCountCallbackHandler();
    ResultSet rs1 = mock(ResultSet.class);
    ResultSet rs2 = mock(ResultSet.class);
    ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

    when(rs1.getMetaData()).thenReturn(rsmd);
    when(rsmd.getColumnCount()).thenReturn(1);
    when(rsmd.getColumnType(1)).thenReturn(java.sql.Types.INTEGER);
    when(rsmd.getColumnName(1)).thenReturn("id");

    // First call - should extract metadata
    handler.processRow(rs1);

    // Second call - should not extract metadata again
    handler.processRow(rs2);

    // Verify getMetaData was called only once
    verify(rs1).getMetaData();
    verify(rs2, never()).getMetaData();
  }

  @Test
  void shouldHandleProcessRowSubclassOverride() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

    RowCountCallbackHandler handler = new RowCountCallbackHandler() {
      @Override
      protected void processRow(ResultSet rs, int rowNum) throws SQLException {
        // Custom processing logic
        super.processRow(rs, rowNum);
      }
    };

    when(rs.getMetaData()).thenReturn(rsmd);
    when(rsmd.getColumnCount()).thenReturn(1);
    when(rsmd.getColumnType(1)).thenReturn(java.sql.Types.INTEGER);
    when(rsmd.getColumnName(1)).thenReturn("id");

    handler.processRow(rs);

    assertThat(handler.getRowCount()).isEqualTo(1);
    assertThat(handler.getColumnCount()).isEqualTo(1);
  }

  @Test
  void shouldReturnCorrectColumnTypesArray() throws SQLException {
    RowCountCallbackHandler handler = new RowCountCallbackHandler();
    ResultSet rs = mock(ResultSet.class);
    ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

    when(rs.getMetaData()).thenReturn(rsmd);
    when(rsmd.getColumnCount()).thenReturn(3);
    when(rsmd.getColumnType(1)).thenReturn(java.sql.Types.BIGINT);
    when(rsmd.getColumnType(2)).thenReturn(java.sql.Types.DECIMAL);
    when(rsmd.getColumnType(3)).thenReturn(java.sql.Types.DATE);
    when(rsmd.getColumnName(1)).thenReturn("id");
    when(rsmd.getColumnName(2)).thenReturn("amount");
    when(rsmd.getColumnName(3)).thenReturn("created_date");

    handler.processRow(rs);

    int[] columnTypes = handler.getColumnTypes();
    assertThat(columnTypes).isNotNull();
    assertThat(columnTypes).hasSize(3);
    assertThat(columnTypes[0]).isEqualTo(java.sql.Types.BIGINT);
    assertThat(columnTypes[1]).isEqualTo(java.sql.Types.DECIMAL);
    assertThat(columnTypes[2]).isEqualTo(java.sql.Types.DATE);
  }

  @Test
  void shouldReturnCorrectColumnNamesArray() throws SQLException {
    RowCountCallbackHandler handler = new RowCountCallbackHandler();
    ResultSet rs = mock(ResultSet.class);
    ResultSetMetaData rsmd = mock(ResultSetMetaData.class);

    when(rs.getMetaData()).thenReturn(rsmd);
    when(rsmd.getColumnCount()).thenReturn(2);
    when(rsmd.getColumnType(1)).thenReturn(java.sql.Types.VARCHAR);
    when(rsmd.getColumnType(2)).thenReturn(java.sql.Types.TIMESTAMP);
    when(rsmd.getColumnName(1)).thenReturn("username");
    when(rsmd.getColumnName(2)).thenReturn("last_login");

    handler.processRow(rs);

    String[] columnNames = handler.getColumnNames();
    assertThat(columnNames).isNotNull();
    assertThat(columnNames).hasSize(2);
    assertThat(columnNames[0]).isEqualTo("username");
    assertThat(columnNames[1]).isEqualTo("last_login");
  }

}