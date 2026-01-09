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

package infra.jdbc;

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
