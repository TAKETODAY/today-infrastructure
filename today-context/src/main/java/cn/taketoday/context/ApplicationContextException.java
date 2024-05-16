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

package cn.taketoday.context;

import cn.taketoday.beans.FatalBeanException;

/**
 * Exception thrown during application context initialization.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2018-10-05 21:33
 */
public class ApplicationContextException extends FatalBeanException {

  /**
   * Create a new {@code ApplicationContextException}
   * with the specified detail message and no root cause.
   *
   * @param msg the detail message
   */
  public ApplicationContextException(String msg) {
    super(msg);
  }

  /**
   * Create a new {@code ApplicationContextException}
   * with the specified detail message and the given root cause.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public ApplicationContextException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
