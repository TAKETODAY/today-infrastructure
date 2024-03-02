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

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.jdbc.core.ResultSetExtractor;

/**
 * @author TODAY 2021/1/2 18:28
 */
final class ObjectResultHandler<T> implements ResultSetExtractor<T> {

  private final int columnCount;

  private final JdbcBeanMetadata metadata;

  private final ObjectPropertySetter[] setters;

  ObjectResultHandler(JdbcBeanMetadata metadata, ObjectPropertySetter[] setters, int columnCount) {
    this.metadata = metadata;
    this.setters = setters;
    this.columnCount = columnCount;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T extractData(final ResultSet resultSet) throws SQLException {
    // otherwise we want executeAndFetch with object mapping
    final int columnCount = this.columnCount;
    final Object pojo = metadata.newInstance();
    final ObjectPropertySetter[] setters = this.setters;
    for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
      ObjectPropertySetter setter = setters[colIdx - 1];
      if (setter != null) {
        setter.setTo(pojo, resultSet, colIdx);
      }
    }
    return (T) pojo;
  }

}
