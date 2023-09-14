/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

import cn.taketoday.lang.Nullable;

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
 * {@link cn.taketoday.jdbc.object.MappingSqlQuery} from the
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
 * @see cn.taketoday.jdbc.object.MappingSqlQuery
 */
@FunctionalInterface
public interface RowMapper<T> {

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
  @Nullable
  T mapRow(ResultSet rs, int rowNum) throws SQLException;

  // Static Factory Methods

  /**
   * Static factory method to create a new {@code BeanPropertyRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  static <T> BeanPropertyRowMapper<T> forMappedClass(Class<T> mappedClass) {
    return new BeanPropertyRowMapper<>(mappedClass);
  }

  /**
   * Static factory method to create a new {@code DataClassRowMapper}.
   *
   * @param mappedClass the class that each row should be mapped to
   */
  static <T> DataClassRowMapper<T> forDataClass(Class<T> mappedClass) {
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
  static <T> SingleColumnRowMapper<T> forSingleColumn(Class<T> requiredType) {
    return new SingleColumnRowMapper<>(requiredType);
  }

}
