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

package cn.taketoday.jdbc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.core.conversion.ConversionException;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.jdbc.PersistenceException;

/**
 * Represents a result set row.
 */
public final class Row {

  private final Object[] values;
  private final boolean isCaseSensitive;
  private final ConversionService conversionService;
  private final Map<String, Integer> columnNameToIdxMap;

  public Row(
          Map<String, Integer> columnNameToIdxMap,
          int columnCnt, boolean isCaseSensitive, ConversionService conversionService) {
    this.columnNameToIdxMap = columnNameToIdxMap;
    this.isCaseSensitive = isCaseSensitive;
    // lol. array works better
    this.values = new Object[columnCnt];
    this.conversionService = conversionService;
  }

  void addValue(int columnIndex, Object value) {
    values[columnIndex] = value;
  }

  public Object getObject(int columnIndex) {
    return values[columnIndex];
  }

  public Object getObject(String columnName) {
    Integer index = columnNameToIdxMap.get(
            isCaseSensitive ? columnName
                            : columnName.toLowerCase());
    if (index != null) {
      return getObject(index);
    }
    throw new PersistenceException(String.format("Column with name '%s' does not exist", columnName));
  }

  public <V> V getObject(int columnIndex, Class<V> clazz) {
    try {
      return conversionService.convert(getObject(columnIndex), clazz);
    }
    catch (ConversionException ex) {
      throw new PersistenceException("Error converting value", ex);
    }
  }

  public <V> V getObject(String columnName, Class<V> clazz) {
    try {
      return conversionService.convert(getObject(columnName), clazz);
    }
    catch (ConversionException ex) {
      throw new PersistenceException("Error converting value", ex);
    }
  }

  public BigDecimal getBigDecimal(int columnIndex) {
    return getObject(columnIndex, BigDecimal.class);
  }

  public BigDecimal getBigDecimal(String columnName) {
    return getObject(columnName, BigDecimal.class);
  }

  public Boolean getBoolean(int columnIndex) {
    return getObject(columnIndex, Boolean.class);
  }

  public Boolean getBoolean(String columnName) {
    return getObject(columnName, Boolean.class);
  }

  public Double getDouble(int columnIndex) {
    return getObject(columnIndex, Double.class);
  }

  public Double getDouble(String columnName) {
    return getObject(columnName, Double.class);
  }

  public Float getFloat(int columnIndex) {
    return getObject(columnIndex, Float.class);
  }

  public Float getFloat(String columnName) {
    return getObject(columnName, Float.class);
  }

  public Long getLong(int columnIndex) {
    return getObject(columnIndex, Long.class);
  }

  public Long getLong(String columnName) {
    return getObject(columnName, Long.class);
  }

  public Integer getInteger(int columnIndex) {
    return getObject(columnIndex, Integer.class);
  }

  public Integer getInteger(String columnName) {
    return getObject(columnName, Integer.class);
  }

  public Short getShort(int columnIndex) {
    return getObject(columnIndex, Short.class);
  }

  public Short getShort(String columnName) {
    return getObject(columnName, Short.class);
  }

  public Byte getByte(int columnIndex) {
    return getObject(columnIndex, Byte.class);
  }

  public Byte getByte(String columnName) {
    return getObject(columnName, Byte.class);
  }

  public Date getDate(int columnIndex) {
    return getObject(columnIndex, Date.class);
  }

  public Date getDate(String columnName) {
    return getObject(columnName, Date.class);
  }

  public String getString(int columnIndex) {
    return getObject(columnIndex, String.class);
  }

  public String getString(String columnName) {
    return getObject(columnName, String.class);
  }

  /**
   * View row as a simple map.
   */
  public Map<String, Object> asMap() {
    final Object[] values = this.values;
    final HashMap<String, Object> map = new HashMap<>();
    for (final Map.Entry<String, Integer> entry : columnNameToIdxMap.entrySet()) {
      map.put(entry.getKey(), values[entry.getValue()]);
    }
    return map;
  }
}
