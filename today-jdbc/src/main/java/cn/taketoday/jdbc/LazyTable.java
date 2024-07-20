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

import java.util.List;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public final class LazyTable implements AutoCloseable {

  private final String tableName;

  private final ResultSetIterable<Row> rows;

  private final List<Column> columns;

  LazyTable(String tableName, ResultSetIterable<Row> rows, List<Column> columns) {
    this.tableName = tableName;
    this.rows = rows;
    this.columns = columns;
  }

  public String getName() {
    return tableName;
  }

  public Iterable<Row> rows() {
    return rows;
  }

  public List<Column> columns() {
    return columns;
  }

  @Override
  public void close() {
    this.rows.close();
  }

}
