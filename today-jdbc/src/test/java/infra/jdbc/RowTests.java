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

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import infra.core.conversion.ConversionException;
import infra.core.conversion.ConversionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/8 21:05
 */
class RowTests {

  @Test
  void shouldCreateRow() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);
    columnNameToIdxMap.put("name", 1);

    ConversionService conversionService = mock(ConversionService.class);
    Row row = new Row(columnNameToIdxMap, 2, true, conversionService);

    assertThat(row).isNotNull();
  }

  @Test
  void shouldAddValue() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);
    columnNameToIdxMap.put("name", 1);

    ConversionService conversionService = mock(ConversionService.class);
    Row row = new Row(columnNameToIdxMap, 2, true, conversionService);

    row.addValue(0, 1L);
    row.addValue(1, "test");

    assertThat(row.getObject(0)).isEqualTo(1L);
    assertThat(row.getObject(1)).isEqualTo("test");
  }

  @Test
  void shouldGetObjectByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);
    columnNameToIdxMap.put("name", 1);

    ConversionService conversionService = mock(ConversionService.class);
    Row row = new Row(columnNameToIdxMap, 2, true, conversionService);

    Object value = "testValue";
    row.addValue(1, value);

    assertThat(row.getObject(1)).isEqualTo(value);
  }

  @Test
  void shouldGetObjectByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);
    columnNameToIdxMap.put("name", 1);

    ConversionService conversionService = mock(ConversionService.class);
    Row row = new Row(columnNameToIdxMap, 2, true, conversionService);

    Object value = "testValue";
    row.addValue(1, value);

    assertThat(row.getObject("name")).isEqualTo(value);
  }

  @Test
  void shouldThrowExceptionWhenColumnNotFound() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);
    columnNameToIdxMap.put("name", 1);

    ConversionService conversionService = mock(ConversionService.class);
    Row row = new Row(columnNameToIdxMap, 2, true, conversionService);

    assertThatThrownBy(() -> row.getObject("nonexistent"))
            .isInstanceOf(PersistenceException.class)
            .hasMessage("Column with name 'nonexistent' does not exist");
  }

  @Test
  void shouldGetObjectWithConversionByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123", Integer.class)).thenReturn(123);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123");

    Integer result = row.getObject(0, Integer.class);
    assertThat(result).isEqualTo(123);

    verify(conversionService).convert("123", Integer.class);
  }

  @Test
  void shouldGetObjectWithConversionByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123", Integer.class)).thenReturn(123);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123");

    Integer result = row.getObject("id", Integer.class);
    assertThat(result).isEqualTo(123);

    verify(conversionService).convert("123", Integer.class);
  }

  @Test
  void shouldThrowExceptionWhenConversionFails() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("invalid", Integer.class))
            .thenThrow(new ConversionException("Conversion failed") { });

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "invalid");

    assertThatThrownBy(() -> row.getObject("id", Integer.class))
            .isInstanceOf(PersistenceException.class)
            .hasMessage("Error converting value");
  }

  @Test
  void shouldGetBigDecimalByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("amount", 0);

    ConversionService conversionService = mock(ConversionService.class);
    BigDecimal value = new BigDecimal("123.45");
    when(conversionService.convert("123.45", BigDecimal.class)).thenReturn(value);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123.45");

    BigDecimal result = row.getBigDecimal(0);
    assertThat(result).isEqualTo(value);
  }

  @Test
  void shouldGetBigDecimalByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("amount", 0);

    ConversionService conversionService = mock(ConversionService.class);
    BigDecimal value = new BigDecimal("123.45");
    when(conversionService.convert("123.45", BigDecimal.class)).thenReturn(value);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123.45");

    BigDecimal result = row.getBigDecimal("amount");
    assertThat(result).isEqualTo(value);
  }

  @Test
  void shouldGetBooleanByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("active", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("true", Boolean.class)).thenReturn(true);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "true");

    Boolean result = row.getBoolean(0);
    assertThat(result).isTrue();
  }

  @Test
  void shouldGetBooleanByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("active", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("true", Boolean.class)).thenReturn(true);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "true");

    Boolean result = row.getBoolean("active");
    assertThat(result).isTrue();
  }

  @Test
  void shouldGetDoubleByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("price", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123.45", Double.class)).thenReturn(123.45);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123.45");

    Double result = row.getDouble(0);
    assertThat(result).isEqualTo(123.45);
  }

  @Test
  void shouldGetDoubleByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("price", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123.45", Double.class)).thenReturn(123.45);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123.45");

    Double result = row.getDouble("price");
    assertThat(result).isEqualTo(123.45);
  }

  @Test
  void shouldGetFloatByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("rate", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123.45", Float.class)).thenReturn(123.45f);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123.45");

    Float result = row.getFloat(0);
    assertThat(result).isEqualTo(123.45f);
  }

  @Test
  void shouldGetFloatByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("rate", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123.45", Float.class)).thenReturn(123.45f);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123.45");

    Float result = row.getFloat("rate");
    assertThat(result).isEqualTo(123.45f);
  }

  @Test
  void shouldGetLongByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123456", Long.class)).thenReturn(123456L);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123456");

    Long result = row.getLong(0);
    assertThat(result).isEqualTo(123456L);
  }

  @Test
  void shouldGetLongByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123456", Long.class)).thenReturn(123456L);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123456");

    Long result = row.getLong("id");
    assertThat(result).isEqualTo(123456L);
  }

  @Test
  void shouldGetIntegerByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("count", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123", Integer.class)).thenReturn(123);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123");

    Integer result = row.getInteger(0);
    assertThat(result).isEqualTo(123);
  }

  @Test
  void shouldGetIntegerByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("count", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123", Integer.class)).thenReturn(123);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123");

    Integer result = row.getInteger("count");
    assertThat(result).isEqualTo(123);
  }

  @Test
  void shouldGetShortByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("code", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123", Short.class)).thenReturn((short) 123);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123");

    Short result = row.getShort(0);
    assertThat(result).isEqualTo((short) 123);
  }

  @Test
  void shouldGetShortByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("code", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123", Short.class)).thenReturn((short) 123);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123");

    Short result = row.getShort("code");
    assertThat(result).isEqualTo((short) 123);
  }

  @Test
  void shouldGetByteByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("flag", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123", Byte.class)).thenReturn((byte) 123);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123");

    Byte result = row.getByte(0);
    assertThat(result).isEqualTo((byte) 123);
  }

  @Test
  void shouldGetByteByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("flag", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert("123", Byte.class)).thenReturn((byte) 123);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "123");

    Byte result = row.getByte("flag");
    assertThat(result).isEqualTo((byte) 123);
  }

  @Test
  void shouldGetDateByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("created", 0);

    ConversionService conversionService = mock(ConversionService.class);
    Date date = new Date();
    when(conversionService.convert("2023-01-01", Date.class)).thenReturn(date);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "2023-01-01");

    Date result = row.getDate(0);
    assertThat(result).isEqualTo(date);
  }

  @Test
  void shouldGetDateByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("created", 0);

    ConversionService conversionService = mock(ConversionService.class);
    Date date = new Date();
    when(conversionService.convert("2023-01-01", Date.class)).thenReturn(date);

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, "2023-01-01");

    Date result = row.getDate("created");
    assertThat(result).isEqualTo(date);
  }

  @Test
  void shouldGetStringByIndex() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("name", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert(123, String.class)).thenReturn("123");

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, 123);

    String result = row.getString(0);
    assertThat(result).isEqualTo("123");
  }

  @Test
  void shouldGetStringByName() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("name", 0);

    ConversionService conversionService = mock(ConversionService.class);
    when(conversionService.convert(123, String.class)).thenReturn("123");

    Row row = new Row(columnNameToIdxMap, 1, true, conversionService);
    row.addValue(0, 123);

    String result = row.getString("name");
    assertThat(result).isEqualTo("123");
  }

  @Test
  void shouldConvertToMap() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);
    columnNameToIdxMap.put("name", 1);

    ConversionService conversionService = mock(ConversionService.class);
    Row row = new Row(columnNameToIdxMap, 2, true, conversionService);

    row.addValue(0, 1L);
    row.addValue(1, "test");

    Map<String, Object> map = row.asMap();
    assertThat(map).hasSize(2);
    assertThat(map.get("id")).isEqualTo(1L);
    assertThat(map.get("name")).isEqualTo("test");
  }

  @Test
  void shouldHandleCaseInsensitiveColumnNames() {
    Map<String, Integer> columnNameToIdxMap = new HashMap<>();
    columnNameToIdxMap.put("id", 0);
    columnNameToIdxMap.put("name", 1);

    ConversionService conversionService = mock(ConversionService.class);
    Row row = new Row(columnNameToIdxMap, 2, false, conversionService);

    row.addValue(1, "testValue");

    assertThat(row.getObject("NAME")).isEqualTo("testValue");
    assertThat(row.getObject("Name")).isEqualTo("testValue");
    assertThat(row.getObject("name")).isEqualTo("testValue");
  }

}