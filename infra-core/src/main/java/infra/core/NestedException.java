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

package infra.core;

import org.jspecify.annotations.Nullable;

import java.io.Serializable;

/**
 * Handy class for wrapping {@code Exceptions} with a root cause.
 * This class is {@code abstract} to force the programmer to extend the class.
 * <p>
 * This interface is for exception handling
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see NestedCheckedException
 * @see NestedRuntimeException
 * @since 4.0 2024/3/14 10:37
 */
public interface NestedException extends Serializable {

  /**
   * Return the detail message, including the message from the nested exception
   * if there is one.
   */
  @Nullable
  String getNestedMessage();

  /**
   * Retrieve the innermost cause of this exception, if any.
   *
   * @return the innermost exception, or {@code null} if none
   */
  @Nullable
  Throwable getRootCause();

  /**
   * Retrieve the most specific cause of this exception, that is,
   * either the innermost cause (root cause) or this exception itself.
   * <p>Differs from {@link #getRootCause()} in that it falls back
   * to the present exception if there is no root cause.
   *
   * @return the most specific cause (never {@code null})
   */
  Throwable getMostSpecificCause();

  /**
   * Check whether this exception contains an exception of the given type:
   * either it is of the given class itself or it contains a nested cause
   * of the given type.
   *
   * @param exType the exception type to look for
   * @return whether there is a nested exception of the specified type
   */
  boolean contains(@Nullable Class<?> exType);

}
