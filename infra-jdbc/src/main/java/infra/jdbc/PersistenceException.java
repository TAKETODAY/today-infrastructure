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

import infra.dao.DataAccessException;

/**
 * General-purpose exception for failures during JDBC or entity persistence operations.
 *
 * <p>Used when a failure cannot be represented by a more specific
 * {@link DataAccessException}. An underlying cause, when supplied, is retained
 * for diagnostic purposes.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class PersistenceException extends DataAccessException {

  /**
   * Create a new persistence exception with the given detail message.
   *
   * @param message the detail message, or {@code null} if none
   */
  public PersistenceException(@Nullable String message) {
    super(message);
  }

  /**
   * Create a new persistence exception with the given detail message and cause.
   *
   * @param message the detail message, or {@code null} if none
   * @param cause the underlying cause, or {@code null} if none
   */
  public PersistenceException(@Nullable String message, @Nullable Throwable cause) {
    super(message, cause);
  }

}
