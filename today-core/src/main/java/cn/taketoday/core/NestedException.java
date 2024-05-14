/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.core;

import java.io.Serializable;

import cn.taketoday.lang.Nullable;

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

  long serialVersionUID = 1L;

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
