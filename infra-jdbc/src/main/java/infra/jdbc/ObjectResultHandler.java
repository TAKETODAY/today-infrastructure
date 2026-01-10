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

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

import infra.jdbc.core.ResultSetExtractor;

/**
 * @param <T> value type
 * @author TODAY 2021/1/2 18:28
 */
final class ObjectResultHandler<T> implements ResultSetExtractor<T> {

  private final int columnCount;

  private final JdbcBeanMetadata metadata;

  private final @Nullable ObjectPropertySetter[] setters;

  ObjectResultHandler(JdbcBeanMetadata metadata, @Nullable ObjectPropertySetter[] setters, int columnCount) {
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
    final @Nullable ObjectPropertySetter[] setters = this.setters;
    for (int colIdx = 1; colIdx <= columnCount; colIdx++) {
      ObjectPropertySetter setter = setters[colIdx - 1];
      if (setter != null) {
        setter.setTo(pojo, resultSet, colIdx);
      }
    }
    return (T) pojo;
  }

}
