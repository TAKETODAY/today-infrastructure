/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.jdbc.result;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.jdbc.ResultSetHandler;
import cn.taketoday.jdbc.reflection.JdbcBeanMetadata;

/**
 * @author TODAY 2021/1/2 18:28
 */
public class ObjectResultHandler<T> implements ResultSetHandler<T> {

  final int columnCount;
  final JdbcBeanMetadata metadata;
  final JdbcPropertyAccessor[] propertyAccessors;

  public ObjectResultHandler(final JdbcBeanMetadata metadata, final JdbcPropertyAccessor[] accessors, int columnCount) {
    this.metadata = metadata;
    this.columnCount = columnCount;
    this.propertyAccessors = accessors;
  }

  @Override
  @SuppressWarnings("unchecked")
  public T handle(final ResultSet resultSet) throws SQLException {
    // otherwise we want executeAndFetch with object mapping
    Object pojo = metadata.newInstance();
    final JdbcPropertyAccessor[] accessors = propertyAccessors;
    for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
      final JdbcPropertyAccessor setter = accessors[colIdx - 1];
      if (setter != null) {
        setter.set(pojo, resultSet, colIdx);
      }
    }
    return (T) pojo;
  }

}
