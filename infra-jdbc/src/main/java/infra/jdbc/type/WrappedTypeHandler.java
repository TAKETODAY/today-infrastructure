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
public interface WrappedTypeHandler<T extends @Nullable Object> extends TypeHandler<T> {

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
