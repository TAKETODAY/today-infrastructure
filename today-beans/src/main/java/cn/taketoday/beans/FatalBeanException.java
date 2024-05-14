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

package cn.taketoday.beans;

import cn.taketoday.lang.Nullable;

/**
 * Thrown on an unrecoverable problem encountered in the
 * beans packages or sub-packages, e.g. bad class or field.
 *
 * @author Rod Johnson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/17 22:20
 */
public class FatalBeanException extends BeansException {

  /**
   * Create a new FatalBeanException with the specified message.
   *
   * @param msg the detail message
   */
  public FatalBeanException(String msg) {
    super(msg);
  }

  /**
   * Create a new FatalBeanException with the specified message
   * and root cause.
   *
   * @param msg the detail message
   * @param cause the root cause
   */
  public FatalBeanException(String msg, @Nullable Throwable cause) {
    super(msg, cause);
  }

}

