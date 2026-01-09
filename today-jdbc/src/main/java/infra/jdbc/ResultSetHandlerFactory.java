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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import infra.jdbc.core.ResultSetExtractor;

/**
 * @param <T> element type
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
public interface ResultSetHandlerFactory<T> {

  /**
   * Get one row ResultSetExtractor
   */
  ResultSetExtractor<T> getResultSetHandler(ResultSetMetaData resultSetMetaData) throws SQLException;

}
