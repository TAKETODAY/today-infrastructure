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

import java.util.AbstractList;
import java.util.List;
import java.util.Map;

/**
 * Represents an offline result set with columns and rows and data.
 */
public final class Table {
  private final String name;
  private final List<Row> rows;
  private final List<Column> columns;

  public Table(String name, List<Row> rows, List<Column> columns) {
    this.name = name;
    this.rows = rows;
    this.columns = columns;
  }

  public String getName() {
    return name;
  }

  public List<Row> rows() {
    return rows;
  }

  public List<Column> columns() {
    return columns;
  }

  public List<Map<String, Object>> asList() {
    return new AbstractList<>() {
      @Override
      public Map<String, Object> get(int index) {
        return rows.get(index).asMap();
      }

      @Override
      public int size() {
        return rows.size();
      }
    };
  }
}
