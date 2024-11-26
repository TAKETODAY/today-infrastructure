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

package infra.scheduling;

import infra.core.NestedRuntimeException;

/**
 * General exception to be thrown on scheduling failures,
 * such as the scheduler already having shut down.
 * Unchecked since scheduling failures are usually fatal.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class SchedulingException extends NestedRuntimeException {

  /**
   * Constructor for SchedulingException.
   *
   * @param msg the detail message
   */
  public SchedulingException(String msg) {
    super(msg);
  }

  /**
   * Constructor for SchedulingException.
   *
   * @param msg the detail message
   * @param cause the root cause (usually from using a underlying
   * scheduling API such as Quartz)
   */
  public SchedulingException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
