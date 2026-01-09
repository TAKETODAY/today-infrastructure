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

package infra.dao;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown on mismatch between Java type and database type:
 * for example on an attempt to set an object of the wrong type
 * in an RDBMS column.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class TypeMismatchDataAccessException extends InvalidDataAccessResourceUsageException {

  /**
   * Constructor for TypeMismatchDataAccessException.
   *
   * @param msg the detail message
   */
  public TypeMismatchDataAccessException(@Nullable String msg) {
    this(msg, null);
  }

  /**
   * Constructor for TypeMismatchDataAccessException.
   *
   * @param msg the detail message
   * @param cause the root cause from the data access API in use
   */
  public TypeMismatchDataAccessException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
