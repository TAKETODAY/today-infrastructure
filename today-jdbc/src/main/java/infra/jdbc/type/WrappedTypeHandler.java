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

package infra.jdbc.type;

import org.jspecify.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A specialized {@link TypeHandler} that works with wrapped objects.
 *
 * <p>This interface extends the basic {@code TypeHandler} functionality to support
 * scenarios where the target object is wrapped within another object. It provides
 * methods to directly apply results to the wrapped object rather than returning
 * the value directly.
 *
 * <p>This is particularly useful for complex object mappings where the database
 * result needs to be applied to a nested or wrapped property.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/6 16:43
 */
public interface WrappedTypeHandler<T> extends TypeHandler<T> {

  /**
   * Applies the result from the database to a wrapped object.
   *
   * <p>This method retrieves the value using {@link #getResult(ResultSet, int)}
   * and then applies it to the provided wrapped object.
   *
   * @param wrapped the wrapped object to which the result should be applied
   * @param rs the database result set containing the data
   * @param columnIndex the index of the column to retrieve (1-based)
   * @throws SQLException if a database access error occurs or the result cannot be applied
   */
  void applyResult(T wrapped, ResultSet rs, int columnIndex) throws SQLException;

  @Override
  default @Nullable T getResult(ResultSet rs, int columnIndex) throws SQLException {
    throw new UnsupportedOperationException();
  }

}
