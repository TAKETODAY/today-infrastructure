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

package cn.taketoday.jmx;

import cn.taketoday.jmx.support.JmxUtils;

/**
 * Exception thrown when we cannot locate an instance of an {@code MBeanServer},
 * or when more than one instance is found.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see JmxUtils#locateMBeanServer
 * @since 4.0
 */
public class MBeanServerNotFoundException extends JmxException {

  /**
   * Create a new {@code MBeanServerNotFoundException} with the
   * supplied error message.
   *
   * @param msg the error message
   */
  public MBeanServerNotFoundException(String msg) {
    super(msg);
  }

  /**
   * Create a new {@code MBeanServerNotFoundException} with the
   * specified error message and root cause.
   *
   * @param msg the error message
   * @param cause the root cause
   */
  public MBeanServerNotFoundException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
