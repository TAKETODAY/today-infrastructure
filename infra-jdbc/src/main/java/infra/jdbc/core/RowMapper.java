/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.jdbc.core;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

import infra.jdbc.object.MappingSqlQuery;

/**
 * An interface used by {@link JdbcTemplate} for mapping rows of a
 * {@link ResultSet} on a per-row basis. Implementations of this
 * interface perform the actual work of mapping each row to a result object,
 * but don't need to worry about exception handling.
 * {@link SQLException SQLExceptions} will be caught and handled
 * by the calling JdbcTemplate.
 *
 * <p>Typically used either for {@link JdbcTemplate}'s query methods
 * or for out parameters of stored procedures. RowMapper objects are
 * typically stateless and thus reusable; they are an ideal choice for
 * implementing row-mapping logic in a single place.
 *
 * <p>Alternatively, consider subclassing
 * {@link MappingSqlQuery} from the
 * {@code jdbc.object} package: Instead of working with separate
 * JdbcTemplate and RowMapper objects, you can build executable query
 * objects (containing row-mapping logic) in that style.
 *
 * @param <T> the result type
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see ResultSetExtractor
 * @see MappingSqlQuery
 */
@FunctionalInterface
public interface RowMapper<T extends @Nullable Object> {

  /**
   * Implementations must implement this method to map each row of data
   * in the ResultSet. This method should not call {@code next()} on
   * the ResultSet; it is only supposed to map values of the current row.
   *
   * @param rs the ResultSet to map (pre-initialized for the current row)
   * @param rowNum the number of the current row
   * @return the result object for the current row (may be {@code null})
   * @throws SQLException if an SQLException is encountered getting
   * column values (that is, there's no need to catch SQLException)
   */
  T mapRow(ResultSet rs, int rowNum) throws SQLException;

  // Static Factory Methods

  /**
   * Static factory method to create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  static <T extends @Nullable Object> BeanPropertyRowMapper<T> forMappedClass(Class<T> mappedClass) {
    return new BeanPropertyRowMapper<>(mappedClass);
  }

  /**
   * Static factory method to create a new {@code DataClassRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  static <T extends @Nullable Object> DataClassRowMapper<T> forDataClass(Class<T> mappedClass) {
    return new DataClassRowMapper<>(mappedClass);
  }

  /**
   * Static factory method to create a new {@code ColumnMapRowMapper}.
   *
   * @see ColumnMapRowMapper
   */
  static ColumnMapRowMapper forColumnMap() {
    return new ColumnMapRowMapper();
  }

  /**
   * Static factory method to create a new {@code SingleColumnRowMapper}.
   *
   * @param requiredType the type that each result object is expected to match
   */
  static <T extends @Nullable Object> SingleColumnRowMapper<T> forSingleColumn(Class<T> requiredType) {
    return new SingleColumnRowMapper<>(requiredType);
  }

}
