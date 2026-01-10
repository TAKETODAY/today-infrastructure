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

package infra.web.util;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * Thrown when a URL string cannot be parsed.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class InvalidUrlException extends IllegalArgumentException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Construct a {@code InvalidUrlException} with the specified detail message.
   *
   * @param msg the detail message
   */
  public InvalidUrlException(@Nullable String msg) {
    this(msg, null);
  }

  /**
   * Construct a {@code InvalidUrlException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public InvalidUrlException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }
}
