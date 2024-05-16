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

import cn.taketoday.lang.Nullable;

/**
 * @author TODAY 2021/7/27 20:50
 * @since 4.0
 */
public class NoStackTraceRuntimeException extends NestedRuntimeException {

  /**
   * Construct a {@code NestedRuntimeException} with the specified detail message
   * and nested exception.
   *
   * @param msg the detail message
   * @param cause the nested exception
   */
  public NoStackTraceRuntimeException(@Nullable String msg, @Nullable Throwable cause) {
    super(msg, cause, false, false);
  }

  /**
   * no stack trace
   */
  @Override
  public final Throwable fillInStackTrace() {
    return this;
  }

}
