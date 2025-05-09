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

package infra.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import infra.core.conversion.ConversionException;
import infra.core.conversion.ConversionService;
import infra.jdbc.type.TypeHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/5/9 10:42
 */
class UpdateResultTests {

  @Test
  void getKeys_ReturnsCorrectKeys_WhenKeysAreGenerated() throws SQLException {
    JdbcConnection connection = mock(JdbcConnection.class);
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, connection);

    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, true, false);
    when(resultSet.getObject(1)).thenReturn(1, 2);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    when(handler.getResult(resultSet, 1)).thenReturn(1, 2);

    updateResult.setKeys(resultSet, handler);
    Object[] keys = updateResult.getKeys();

    assertThat(keys).containsExactly(1, 2);
  }

  @Test
  void getKeys_ThrowsGeneratedKeysException_WhenKeysNotFetched() {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));

    assertThatThrownBy(updateResult::getKeys)
            .isInstanceOf(GeneratedKeysException.class)
            .hasMessageContaining("Keys where not fetched from database");
  }

  @Test
  void getFirstKeyWithType_ReturnsConvertedValue() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    when(handler.getResult(resultSet, 1)).thenReturn(123);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert(123, String.class)).thenReturn("123");

    updateResult.setKeys(resultSet, handler);

    String key = updateResult.getFirstKey(String.class, conversionService);
    assertThat(key).isEqualTo("123");
  }

  @Test
  void getFirstKeyWithType_ThrowsException_WhenConversionFails() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    when(handler.getResult(resultSet, 1)).thenReturn(123);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert(123, Double.class)).thenThrow(new ConversionException("Failed") { });

    updateResult.setKeys(resultSet, handler);

    assertThatThrownBy(() -> updateResult.getFirstKey(Double.class, conversionService))
            .isInstanceOf(GeneratedKeysConversionException.class)
            .hasMessageContaining("Exception occurred while converting value");
  }

  @Test
  void getKeysArray_ReturnsTypedArray() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, true, false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    when(handler.getResult(resultSet, 1)).thenReturn(1, 2);

    updateResult.setKeys(resultSet, handler);
    Integer[] keys = updateResult.getKeysArray(Integer.class);

    assertThat(keys).containsExactly(1, 2);
  }

  @Test
  void getKeys_ConvertsAllValues() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(true, true, false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    when(handler.getResult(resultSet, 1)).thenReturn(1, 2);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert(1, String.class)).thenReturn("1");
    when(conversionService.convert(2, String.class)).thenReturn("2");

    updateResult.setKeys(resultSet, handler);
    List<String> convertedKeys = updateResult.getKeys(String.class, conversionService);

    assertThat(convertedKeys).containsExactly("1", "2");
  }

  @Test
  void getAffectedRows_ThrowsException_WhenNotExecuted() {
    UpdateResult<Integer> updateResult = new UpdateResult<>(null, mock(JdbcConnection.class));

    assertThatThrownBy(updateResult::getAffectedRows)
            .isInstanceOf(PersistenceException.class)
            .hasMessageContaining("executeUpdate()");
  }

  @Test
  void getKeys_ReturnEmptyArray_WhenNoKeysPresent() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    updateResult.setKeys(resultSet, handler);

    Object[] keys = updateResult.getKeys();
    assertThat(keys).isEmpty();
  }

  @Test
  void getKeysArray_ReturnEmptyArray_WhenNoKeysPresent() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    updateResult.setKeys(resultSet, handler);

    Integer[] keys = updateResult.getKeysArray(Integer.class);
    assertThat(keys).isEmpty();
  }

  @Test
  void getKeys_WithType_ReturnEmptyList_WhenNoKeysPresent() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    ConversionService conversionService = mock(ConversionService.class);

    updateResult.setKeys(resultSet, handler);
    List<String> keys = updateResult.getKeys(String.class, conversionService);

    assertThat(keys).isEmpty();
  }

  @Test
  void setKeys_CloseResultSet_AfterReading() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    updateResult.setKeys(resultSet, handler);

    verify(resultSet).close();
  }

  @Test
  void affectedRows_ReturnsCorrectValue() {
    UpdateResult<Integer> updateResult = new UpdateResult<>(5, mock(JdbcConnection.class));
    assertThat(updateResult.getAffectedRows()).isEqualTo(5);
  }

  @Test
  void getFirstKey_ReturnsNull_WhenKeysListIsEmpty() throws SQLException {
    UpdateResult<Integer> updateResult = new UpdateResult<>(1, mock(JdbcConnection.class));
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.next()).thenReturn(false);

    TypeHandler<Integer> handler = mock(TypeHandler.class);
    updateResult.setKeys(resultSet, handler);

    assertThat(updateResult.getFirstKey()).isNull();
  }

}