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

package infra.jmx.export;

/**
 * Exception thrown when we are unable to register an MBean,
 * for example because of a naming conflict.
 *
 * @author Rob Harrop
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class UnableToRegisterMBeanException extends MBeanExportException {

  /**
   * Create a new {@code UnableToRegisterMBeanException} with the
   * specified error message.
   *
   * @param msg the detail message
   */
  public UnableToRegisterMBeanException(String msg) {
    super(msg);
  }

  /**
   * Create a new {@code UnableToRegisterMBeanException} with the
   * specified error message and root cause.
   *
   * @param msg the detail message
   * @param cause the root caus
   */
  public UnableToRegisterMBeanException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
