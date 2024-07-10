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

package cn.taketoday.beans.factory.aot;

import cn.taketoday.lang.Nullable;

/**
 * Throw when an AOT processor failed.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
@SuppressWarnings("serial")
public class AotProcessingException extends AotException {

  /**
   * Create a new instance with the detail message and a root cause, if any.
   *
   * @param msg the detail message
   * @param cause the root cause, if any
   */
  public AotProcessingException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}
